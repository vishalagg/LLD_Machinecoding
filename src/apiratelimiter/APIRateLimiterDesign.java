package apiratelimiter;

import java.util.Map;
import java.util.concurrent.*;

public class APIRateLimiterDesign {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        RateLimiter rateLimiter = new RateLimiter(new TokenBucketImpl(10, 5));

        for (int i = 0; i < 25; i++) {
            if (i == 15)
                Thread.sleep(1000);
            System.out.println("Request-" + i + ": " + rateLimiter.shouldAllow("client1"));
            System.out.println("Request-" + i + ": " + rateLimiter.shouldAllow("client2"));

        }
    }

}

class RateLimiter {

    private final IRateLimitStrategy rateLimitStrategy;

    RateLimiter(IRateLimitStrategy rateLimitStrategy) {
        this.rateLimitStrategy = rateLimitStrategy;
    }

    public boolean shouldAllow(String clientId) throws ExecutionException, InterruptedException {
        return rateLimitStrategy.shouldAllow(clientId);
    }
}

interface IRateLimitStrategy {

    public boolean shouldAllow(String clientId) throws ExecutionException, InterruptedException;
}

class TokenBucketImpl implements IRateLimitStrategy {

    private final int MAX_TOKENS;
    private final int REFILL_RATE;

    private static final int nThread = 10;

    private final Executor[] executors;
    private final Map<String, TokenBucket> buckets;

    TokenBucketImpl(int maxTokens, int refillRate) {
        this.buckets = new ConcurrentHashMap<>();
        this.MAX_TOKENS = maxTokens;
        this.REFILL_RATE = refillRate;
        this.executors = new Executor[nThread];

        for (int i=0; i<nThread; i++) {
            this.executors[i] = Executors.newSingleThreadExecutor();
        }
    }

    @Override
    public boolean shouldAllow(String clientId) throws ExecutionException, InterruptedException {

        return CompletableFuture.supplyAsync(
                () -> {
                    buckets.putIfAbsent(clientId, new TokenBucket(MAX_TOKENS, System.currentTimeMillis()));
                    TokenBucket bucket = buckets.get(clientId);
                    refillTokens(bucket);

                    if (bucket.tokens > 0) {
                        bucket.tokens--;
                        return true;
                    }
                    return false;
                },
                this.executors[Math.abs(clientId.hashCode())%nThread]
        ).get();
    }

    private void refillTokens(TokenBucket bucket) {
        long currentTime = System.currentTimeMillis();
        long timeWindow = currentTime - bucket.lastRefillTime;
        int tokensToAdd = (int) (timeWindow / 1000) * REFILL_RATE;

        if (tokensToAdd > 0) {
            bucket.tokens = Math.min(bucket.tokens + tokensToAdd, MAX_TOKENS);
            bucket.lastRefillTime = currentTime;
        }
    }
}

class TokenBucket {
    int tokens;
    long lastRefillTime;

    public TokenBucket(int tokens, long lastRefillTime) {
        this.tokens = tokens;
        this.lastRefillTime = lastRefillTime;
    }
}
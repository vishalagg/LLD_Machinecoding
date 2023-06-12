package practice.cache;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class CacheDesign {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Cache<Integer, String> cache = new Cache<>(EVICTION_POLICY.LRU, 100000, new Datastore<>(), 3);

        cache.put(1, "a").get();
        cache.put(2, "b").get();
        cache.put(3, "c").get();

        System.out.println("2: " + cache.get(2));

        Thread.sleep(1000);

        cache.put(1, "a2").get();
        cache.put(4, "d").get();

        System.out.println("1: " + cache.get(1));
        System.out.println("2: " + cache.get(2));
        System.out.println("3: " + cache.get(3));
        System.out.println("4: " + cache.get(4));
    }
}

class Cache<KEY, VALUE> {
    private static int TTL;
    private static final int nThread = 3;

    private final Datastore<KEY, VALUE> datastore;
    private final IEvictionStrategy<KEY, VALUE> evictionStrategy;

    private final CacheExecutor<KEY, VALUE> executor;

    Cache(EVICTION_POLICY evictionPolicy, int ttl, Datastore<KEY, VALUE> datastore, int pageSize) {
        TTL = ttl;
        this.datastore = datastore;
        this.evictionStrategy = EvictionStrategyFactory.getEvictionStrategy(evictionPolicy, ttl, pageSize);
        this.executor = new CacheExecutor<>(nThread);
    }

    public VALUE get(KEY key) throws ExecutionException, InterruptedException {
        return executor.supplyAsync(key, () -> evictionStrategy.get(key)).get();
    }

    public CompletableFuture<Void> put(KEY key, VALUE value) {
        return executor.runAsync(key, () -> evictionStrategy.put(key, value))
                .thenRun(() -> datastore.load(key, value));
    }
}

enum EVICTION_POLICY {
    LRU,
    LFU
}

class Datastore<KEY, VALUE> {

    public CompletableFuture<Void> load(KEY key, VALUE value) {
        return CompletableFuture.completedFuture(null);
    }
}

interface IEvictionStrategy<KEY, VALUE> {
    public VALUE get(KEY key);
    public void put(KEY key, VALUE value);
}

class LRUEvictionStrategyImpl<KEY, VALUE> implements IEvictionStrategy<KEY, VALUE> {

    Map<KEY, Record<KEY, VALUE>> cache;
    ConcurrentSkipListMap<Long, List<KEY>> priorityMap;
    private int TTL;
    private int LIMIT;
    LRUEvictionStrategyImpl(int ttl, int size) {
        this.TTL = ttl;
        this.LIMIT = size;

        this.cache = new ConcurrentHashMap<>();
        this.priorityMap = new ConcurrentSkipListMap<>();
    }

    @Override
    public VALUE get(KEY key) {
        if (cache.containsKey(key)) {
            Record<KEY, VALUE> record = removeFromCache(key);

            if (!isExpired(record)) {
                insertIntoCache(new Record<>(key, record.value, record.insertedAt, System.currentTimeMillis(), record.count+1));
                return record.value;
            }
        }
        return null;
    }

    @Override
    public void put(KEY key, VALUE value) {
        if (cache.containsKey(key)) {
            removeFromCache(key);
        }

        if (cache.size() >= LIMIT) {
            removeFromCache(priorityMap.firstEntry().getValue().get(0));
        }
        long currentTime = System.currentTimeMillis();
        insertIntoCache(new Record<>(key, value, currentTime, currentTime, 1));
    }

    private boolean isExpired(Record<KEY, VALUE> record) {
        return record.insertedAt + TTL <= System.currentTimeMillis();
    }

    private Record<KEY, VALUE> removeFromCache(KEY key) {
        Record<KEY, VALUE> record = cache.remove(key);
        priorityMap.get(record.accessedAt).remove(key);
        if (priorityMap.get(record.accessedAt).isEmpty())
            priorityMap.remove(record.accessedAt);
        return record;
    }

    private void insertIntoCache(Record<KEY, VALUE> record) {
        cache.put(record.key, record);
        priorityMap.putIfAbsent(record.accessedAt, new CopyOnWriteArrayList<>());
        priorityMap.get(record.accessedAt).add(record.key);
    }
}

class Record<KEY, VALUE> {
    KEY key;
    VALUE value;
    long insertedAt;
    long accessedAt;
    int count;

    public Record(KEY key, VALUE value, long insertedAt, long accessedAt, int count) {
        this.key = key;
        this.value = value;
        this.insertedAt = insertedAt;
        this.accessedAt = accessedAt;
        this.count = count;
    }
}

class EvictionStrategyFactory {
    public static IEvictionStrategy getEvictionStrategy(EVICTION_POLICY policy, int ttl, int limit) {

        switch (policy) {
            case LRU:
                return new LRUEvictionStrategyImpl(ttl, limit);
        }

        return null;
    }
}

class CacheExecutor<KEY, VALUE> {
    Executor[] executors;

    CacheExecutor(int nThread) {
        executors = new Executor[nThread];

        for (int i=0; i<executors.length; i++) {
            this.executors[i] = Executors.newSingleThreadExecutor();
        }
    }

    public CompletableFuture<Void> runAsync(final KEY key, final Runnable runnable) {
        return CompletableFuture.runAsync(runnable, executors[Math.abs(key.hashCode())%executors.length]);
    }

    public CompletableFuture<VALUE> supplyAsync(final KEY key, final Supplier supplier) {
        return CompletableFuture.supplyAsync(supplier, executors[Math.abs(key.hashCode())%executors.length]);
    }

}





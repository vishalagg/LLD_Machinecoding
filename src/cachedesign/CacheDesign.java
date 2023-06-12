package cachedesign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class CacheDesign {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Cache<Integer, String> cache = new Cache<>(EvictionPolicy.LRU, new DataStore<>(), 100000);

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

    private HashExecutor<KEY, VALUE> executor;
    private DataStore<KEY, VALUE> dataStore;
    private IEvictionStrategy<KEY, VALUE> evictionStrategy;
    private static final int SIZE = 3;
    private static final int nThreads = 5;

    private static int TTL;

    Cache(EvictionPolicy evictionPolicy, DataStore<KEY, VALUE> dataStore, int ttl) {
        TTL = ttl;

        executor = new HashExecutor<>(nThreads);
        evictionStrategy = EvictionStrategyFactory.getEvictionStrategy(evictionPolicy, TTL, SIZE);
        this.dataStore = dataStore;
    }

    public VALUE get(KEY key) throws ExecutionException, InterruptedException {
        return executor.submit(key, ()->evictionStrategy.get(key)).get();
    }

    public CompletableFuture<Void> put(KEY key, VALUE value) {

        return executor.runAsync(key, () -> {
            evictionStrategy.put(key, value);
        }).thenRun(() -> dataStore.load(key, value));
    }
}

class HashExecutor<KEY, VALUE> {
    private ExecutorService[] executorServices;

    HashExecutor(int nThreads) {
        executorServices = new ExecutorService[nThreads];
        for (int i=0; i<nThreads; i++) {
            executorServices[i] = Executors.newSingleThreadExecutor();
        }
    }

    public CompletableFuture<Void> runAsync(final KEY key, final Runnable task) {
        return CompletableFuture.runAsync(task, executorServices[Math.abs(key.hashCode()) % executorServices.length]);
    }

    public CompletableFuture<VALUE> submit(final KEY key, final Supplier supplier) {
        return CompletableFuture.supplyAsync(supplier, executorServices[Math.abs(key.hashCode()) % executorServices.length]);
    }
}

enum EvictionPolicy {
    LRU,
    LFU
}

class Record<KEY, VALUE> {
    KEY key;
    VALUE value;
    Long insertionTime;
    Long accessedTime;
    int accessedCount;

    public Record(KEY key, VALUE value, Long insertionTime, Long accessedTime, int accessedCount) {
        this.key = key;
        this.value = value;
        this.insertionTime = insertionTime;
        this.accessedTime = accessedTime;
        this.accessedCount = accessedCount;
    }
}

class EvictionStrategyFactory {

    public static IEvictionStrategy getEvictionStrategy(EvictionPolicy policy, int ttl, int size) {
        switch (policy) {
            case LRU:
                return new LRUEvictionStrategyImpl<>(ttl,size);
            case LFU:
                return new LFUEvictionStrategyImpl<>(ttl, size);
            default:
                return null;
        }
    }
}

interface IEvictionStrategy<KEY, VALUE> {

    public VALUE get(KEY key);
    public void put(KEY key, VALUE value);
}

class LRUEvictionStrategyImpl<KEY, VALUE> implements IEvictionStrategy<KEY, VALUE> {

    private ConcurrentSkipListMap<Long, List<KEY>> priorityMap;
    private Map<KEY, Record<KEY, VALUE>> cache;
    private static int TTL;
    private static int LIMIT;

    LRUEvictionStrategyImpl(final int ttl, final int limit) {
        priorityMap = new ConcurrentSkipListMap<>();
        cache = new HashMap<>();

        TTL = ttl;
        LIMIT = limit;
    }


    @Override
    public VALUE get(KEY key) {

        if (cache.containsKey(key)) {
            Record<KEY, VALUE> record = removeFromCache(key);

            if (!isExpired(record)) {
                Long currentTime = System.currentTimeMillis();
                record = new Record<>(key, record.value, record.insertionTime, currentTime, record.accessedCount+1);
                putIntoCache(key, record);
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
            Map.Entry<Long, List<KEY>> entry = priorityMap.firstEntry();
            removeFromCache(entry.getValue().get(0));
        }
        Long currentTime = System.currentTimeMillis();
        putIntoCache(key, new Record<>(key, value, currentTime, currentTime, 1));
    }

    private boolean isExpired(Record<KEY, VALUE> record) {
        Long currentTime = System.currentTimeMillis();
        return record.insertionTime + TTL <= currentTime;
    }

    private void putIntoCache(KEY key, Record<KEY, VALUE> record) {
        cache.put(key, record);
        priorityMap.putIfAbsent(record.accessedTime, new CopyOnWriteArrayList<>());
        priorityMap.get(record.accessedTime).add(key);
    }

    private Record<KEY, VALUE> removeFromCache(KEY key) {
        Record<KEY, VALUE> record = cache.remove(key);
        List<KEY> keys = priorityMap.get(record.accessedTime);
        keys.remove(key);
        if (keys.isEmpty())
            priorityMap.remove(record.accessedTime);
        return record;
    }
}

class LFUEvictionStrategyImpl<KEY, VALUE> implements IEvictionStrategy<KEY, VALUE> {

    private ConcurrentSkipListMap<Integer, List<KEY>> priorityMap;
    private Map<KEY, Record<KEY, VALUE>> cache;
    private static int TTL;
    private static int LIMIT;

    LFUEvictionStrategyImpl(final int ttl, final int limit) {
        priorityMap = new ConcurrentSkipListMap<>();
        cache = new HashMap<>();

        TTL = ttl;
        LIMIT = limit;
    }


    @Override
    public synchronized VALUE get(KEY key) {

        if (cache.containsKey(key)) {
            Record<KEY, VALUE> record = removeFromCache(key);

            if (!isExpired(record)) {
                Long currentTime = System.currentTimeMillis();
                record = new Record<>(key, record.value, record.insertionTime, currentTime, record.accessedCount+1);
                putIntoCache(key, record);
                return record.value;
            }
        }
        return null;
    }

    @Override
    public synchronized void put(KEY key, VALUE value) {

        if (cache.containsKey(key)) {
            removeFromCache(key);
        }
        if (cache.size() >= LIMIT) {
            Map.Entry<Integer, List<KEY>> entry = priorityMap.firstEntry();
            removeFromCache(entry.getValue().get(0));
        }
        Long currentTime = System.currentTimeMillis();
        putIntoCache(key, new Record<>(key, value, currentTime, currentTime, 1));
    }

    private boolean isExpired(Record<KEY, VALUE> record) {
        Long currentTime = System.currentTimeMillis();
        return record.insertionTime + TTL <= currentTime;
    }

    private void putIntoCache(KEY key, Record<KEY, VALUE> record) {
        cache.put(key, record);
        priorityMap.putIfAbsent(record.accessedCount, new ArrayList<>());
        priorityMap.get(record.accessedCount).add(key);
    }

    private Record<KEY, VALUE> removeFromCache(KEY key) {
        Record<KEY, VALUE> record = cache.remove(key);
        List<KEY> keys = priorityMap.get(record.accessedCount);
        keys.remove(0);
        if (keys.isEmpty())
            priorityMap.remove(record.accessedCount);
        return record;
    }
}

class DataStore<KEY, VALUE> {

    public CompletableFuture<Void> load(KEY key, VALUE value) {
        return CompletableFuture.completedFuture(null);
    }
}

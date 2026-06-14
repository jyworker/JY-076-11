package com.iyzipay.idempotency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultIdempotencyCache implements IdempotencyCache {

    private final ConcurrentHashMap<IdempotencyKey, IdempotencyRecord> store;
    private final IdempotencyConfig config;
    private final ScheduledExecutorService evictionScheduler;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public DefaultIdempotencyCache(IdempotencyConfig config) {
        this.config = config;
        this.store = new ConcurrentHashMap<>();
        this.evictionScheduler = Executors.newSingleThreadScheduledExecutor(new java.util.concurrent.ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "idempotency-eviction");
                t.setDaemon(true);
                return t;
            }
        });
        startEviction();
    }

    private void startEviction() {
        if (started.compareAndSet(false, true)) {
            evictionScheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    evictExpired();
                    evictOverflow();
                }
            }, config.getEvictionIntervalMillis(), config.getEvictionIntervalMillis(), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void put(IdempotencyKey key, IdempotencyRecord record) {
        store.put(key, record);
        evictOverflow();
    }

    @Override
    public IdempotencyRecord get(IdempotencyKey key) {
        IdempotencyRecord record = store.get(key);
        if (record != null) {
            record.recordAccess();
        }
        return record;
    }

    @Override
    public boolean containsKey(IdempotencyKey key) {
        return store.containsKey(key);
    }

    @Override
    public void remove(IdempotencyKey key) {
        store.remove(key);
    }

    @Override
    public void evictExpired() {
        long now = System.currentTimeMillis();
        for (Map.Entry<IdempotencyKey, IdempotencyRecord> entry : store.entrySet()) {
            IdempotencyRecord record = entry.getValue();
            long ttl = record.isPenetrationMarker() ? config.getPenetrationTtlMillis() : config.getTtlMillis();
            if ((now - record.getCreatedAt()) > ttl) {
                store.remove(entry.getKey());
            }
        }
    }

    private void evictOverflow() {
        while (store.size() > config.getMaxEntries()) {
            IdempotencyKey lruKey = findLruKey();
            if (lruKey != null) {
                store.remove(lruKey);
            } else {
                break;
            }
        }
    }

    private IdempotencyKey findLruKey() {
        long oldestAccess = Long.MAX_VALUE;
        IdempotencyKey lruKey = null;
        for (Map.Entry<IdempotencyKey, IdempotencyRecord> entry : store.entrySet()) {
            long lastAccess = entry.getValue().getLastAccessedAt();
            if (lastAccess < oldestAccess) {
                oldestAccess = lastAccess;
                lruKey = entry.getKey();
            }
        }
        return lruKey;
    }

    @Override
    public int size() {
        return store.size();
    }

    @Override
    public void clear() {
        store.clear();
    }

    public void shutdown() {
        if (started.compareAndSet(true, false)) {
            evictionScheduler.shutdown();
            try {
                if (!evictionScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    evictionScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                evictionScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}

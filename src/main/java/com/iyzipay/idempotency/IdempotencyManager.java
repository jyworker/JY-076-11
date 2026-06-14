package com.iyzipay.idempotency;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class IdempotencyManager {

    private static final IdempotencyManager INSTANCE = new IdempotencyManager(IdempotencyConfig.defaultConfig());

    private final IdempotencyCache cache;
    private final IdempotencyConfig config;
    private final Gson gson;
    private final ConcurrentHashMap<String, ReentrantLock> keyLocks;

    public IdempotencyManager(IdempotencyConfig config) {
        this(config, new DefaultIdempotencyCache(config));
    }

    public IdempotencyManager(IdempotencyConfig config, IdempotencyCache cache) {
        this.config = config;
        this.cache = cache;
        this.gson = new Gson();
        this.keyLocks = new ConcurrentHashMap<>();
    }

    public static IdempotencyManager getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public <T> T execute(IdempotencyKey key, Object request, IdempotencyAction<T> action) {
        ReentrantLock lock = keyLocks.computeIfAbsent(key.getValue(), k -> new ReentrantLock());
        lock.lock();
        try {
            return doExecute(key, request, action);
        } finally {
            lock.unlock();
            keyLocks.remove(key.getValue());
        }
    }

    private <T> T doExecute(IdempotencyKey key, Object request, IdempotencyAction<T> action) {
        IdempotencyRecord existing = cache.get(key);

        if (existing != null) {
            return handleExistingRecord(key, request, existing);
        }

        checkReplayWindow(request);

        T result;
        try {
            result = action.execute();
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey(key.getValue())
                    .requestFingerprint(computeFingerprint(request))
                    .result(result)
                    .status(IdempotencyRecord.Status.SUCCESS)
                    .requestTimestamp(System.currentTimeMillis())
                    .build();
            cache.put(key, record);
            return result;
        } catch (Exception e) {
            if (config.isPenetrationProtectionEnabled()) {
                IdempotencyRecord marker = IdempotencyRecord.builder()
                        .idempotencyKey(key.getValue())
                        .requestFingerprint(computeFingerprint(request))
                        .result(e)
                        .status(IdempotencyRecord.Status.PENETRATION_MARKER)
                        .requestTimestamp(System.currentTimeMillis())
                        .build();
                cache.put(key, marker);
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T handleExistingRecord(IdempotencyKey key, Object request, IdempotencyRecord existing) {
        long ttl = existing.isPenetrationMarker() ? config.getPenetrationTtlMillis() : config.getTtlMillis();

        if (existing.isExpired(ttl)) {
            cache.remove(key);
            return null;
        }

        if (existing.isPenetrationMarker()) {
            Object storedError = existing.getResult();
            if (storedError instanceof RuntimeException) {
                throw (RuntimeException) storedError;
            }
            throw new RuntimeException("Previous request failed: " + storedError);
        }

        String currentFingerprint = computeFingerprint(request);
        if (!existing.getRequestFingerprint().equals(currentFingerprint)) {
            throw new IdempotencyConflictException(
                    key.getValue(),
                    existing.getRequestFingerprint(),
                    currentFingerprint
            );
        }

        return (T) existing.getResult();
    }

    private void checkReplayWindow(Object request) {
        if (config.getReplayWindowMillis() <= 0) {
            return;
        }
        if (request instanceof ReplayTimestampProvider) {
            long requestTs = ((ReplayTimestampProvider) request).getRequestTimestamp();
            long now = System.currentTimeMillis();
            long diff = Math.abs(now - requestTs);
            if (diff > config.getReplayWindowMillis()) {
                throw new ReplayAttackDetectedException(now, requestTs, config.getReplayWindowMillis());
            }
        }
    }

    String computeFingerprint(Object request) {
        try {
            String json = gson.toJson(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public IdempotencyCache getCache() {
        return cache;
    }

    public void clearCache() {
        cache.clear();
    }

    public void shutdown() {
        if (cache instanceof DefaultIdempotencyCache) {
            ((DefaultIdempotencyCache) cache).shutdown();
        }
    }
}

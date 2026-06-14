package com.iyzipay.idempotency;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class IdempotencyCacheTest {

    private DefaultIdempotencyCache cache;
    private IdempotencyConfig config;

    @Before
    public void setUp() {
        config = IdempotencyConfig.builder()
                .ttlMillis(2000)
                .maxEntries(5)
                .evictionIntervalMillis(60000)
                .penetrationTtlMillis(1000)
                .build();
        cache = new DefaultIdempotencyCache(config);
    }

    @After
    public void tearDown() {
        cache.shutdown();
    }

    @Test
    public void should_put_and_get_record() {
        IdempotencyKey key = IdempotencyKey.of("test-key-1");
        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey("test-key-1")
                .requestFingerprint("abc123")
                .result("payment-result")
                .status(IdempotencyRecord.Status.SUCCESS)
                .build();

        cache.put(key, record);

        IdempotencyRecord retrieved = cache.get(key);
        assertNotNull(retrieved);
        assertEquals("test-key-1", retrieved.getIdempotencyKey());
        assertEquals("abc123", retrieved.getRequestFingerprint());
        assertEquals("payment-result", retrieved.getResult());
        assertEquals(IdempotencyRecord.Status.SUCCESS, retrieved.getStatus());
    }

    @Test
    public void should_return_null_for_missing_key() {
        IdempotencyKey key = IdempotencyKey.of("nonexistent");
        assertNull(cache.get(key));
    }

    @Test
    public void should_check_contains_key() {
        IdempotencyKey key = IdempotencyKey.of("test-key-2");
        assertFalse(cache.containsKey(key));

        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey("test-key-2")
                .requestFingerprint("fp")
                .status(IdempotencyRecord.Status.SUCCESS)
                .build();
        cache.put(key, record);

        assertTrue(cache.containsKey(key));
    }

    @Test
    public void should_remove_record() {
        IdempotencyKey key = IdempotencyKey.of("test-key-3");
        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey("test-key-3")
                .requestFingerprint("fp")
                .status(IdempotencyRecord.Status.SUCCESS)
                .build();
        cache.put(key, record);
        assertTrue(cache.containsKey(key));

        cache.remove(key);
        assertFalse(cache.containsKey(key));
    }

    @Test
    public void should_evict_expired_records() throws InterruptedException {
        IdempotencyKey key = IdempotencyKey.of("test-key-4");
        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey("test-key-4")
                .requestFingerprint("fp")
                .status(IdempotencyRecord.Status.SUCCESS)
                .createdAt(System.currentTimeMillis())
                .build();
        cache.put(key, record);
        assertTrue(cache.containsKey(key));

        Thread.sleep(2100);

        cache.evictExpired();
        assertFalse(cache.containsKey(key));
    }

    @Test
    public void should_not_evict_non_expired_records() throws InterruptedException {
        IdempotencyKey key = IdempotencyKey.of("test-key-5");
        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey("test-key-5")
                .requestFingerprint("fp")
                .status(IdempotencyRecord.Status.SUCCESS)
                .createdAt(System.currentTimeMillis())
                .build();
        cache.put(key, record);

        Thread.sleep(100);

        cache.evictExpired();
        assertTrue(cache.containsKey(key));
    }

    @Test
    public void should_evict_on_overflow() {
        for (int i = 0; i < 10; i++) {
            IdempotencyKey key = IdempotencyKey.of("overflow-key-" + i);
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey("overflow-key-" + i)
                    .requestFingerprint("fp" + i)
                    .status(IdempotencyRecord.Status.SUCCESS)
                    .build();
            cache.put(key, record);
        }

        assertTrue(cache.size() <= 5);
    }

    @Test
    public void should_track_audit_fields_on_access() {
        IdempotencyKey key = IdempotencyKey.of("audit-key");
        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey("audit-key")
                .requestFingerprint("fp")
                .status(IdempotencyRecord.Status.SUCCESS)
                .build();
        cache.put(key, record);

        assertEquals(0, record.getAccessCount());

        cache.get(key);
        assertEquals(1, record.getAccessCount());

        cache.get(key);
        assertEquals(2, record.getAccessCount());
        assertTrue(record.getLastAccessedAt() >= record.getCreatedAt());
    }

    @Test
    public void should_evict_penetration_markers_with_shorter_ttl() throws InterruptedException {
        IdempotencyKey key = IdempotencyKey.of("penetration-key");
        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey("penetration-key")
                .requestFingerprint("fp")
                .status(IdempotencyRecord.Status.PENETRATION_MARKER)
                .createdAt(System.currentTimeMillis())
                .build();
        cache.put(key, record);

        Thread.sleep(1100);

        cache.evictExpired();
        assertFalse(cache.containsKey(key));
    }

    @Test
    public void should_clear_all_records() {
        for (int i = 0; i < 3; i++) {
            IdempotencyKey key = IdempotencyKey.of("clear-key-" + i);
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey("clear-key-" + i)
                    .requestFingerprint("fp" + i)
                    .status(IdempotencyRecord.Status.SUCCESS)
                    .build();
            cache.put(key, record);
        }

        assertEquals(3, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
    }
}

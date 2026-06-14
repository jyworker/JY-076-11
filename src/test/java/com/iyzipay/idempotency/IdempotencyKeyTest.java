package com.iyzipay.idempotency;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class IdempotencyKeyTest {

    @Test
    public void should_create_valid_key() {
        IdempotencyKey key = IdempotencyKey.of("order-123");
        assertNotNull(key);
        assertEquals("order-123", key.getValue());
        assertEquals("order-123", key.toString());
    }

    @Test
    public void should_create_key_with_allowed_characters() {
        IdempotencyKey key = IdempotencyKey.of("order_123.abc-def");
        assertNotNull(key);
        assertEquals("order_123.abc-def", key.getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_reject_null_key() {
        IdempotencyKey.of(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_reject_empty_key() {
        IdempotencyKey.of("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_reject_key_exceeding_max_length() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            sb.append("a");
        }
        IdempotencyKey.of(sb.toString());
    }

    @Test
    public void should_accept_key_at_max_length() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 255; i++) {
            sb.append("a");
        }
        IdempotencyKey key = IdempotencyKey.of(sb.toString());
        assertNotNull(key);
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_reject_key_with_special_characters() {
        IdempotencyKey.of("order@123");
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_reject_key_with_spaces() {
        IdempotencyKey.of("order 123");
    }

    @Test
    public void should_implement_equals_and_hashcode() {
        IdempotencyKey key1 = IdempotencyKey.of("order-123");
        IdempotencyKey key2 = IdempotencyKey.of("order-123");
        IdempotencyKey key3 = IdempotencyKey.of("order-456");

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
        assertNotEquals(key1, key3);
    }
}

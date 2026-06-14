package com.iyzipay.idempotency;

public interface IdempotencyCache {

    void put(IdempotencyKey key, IdempotencyRecord record);

    IdempotencyRecord get(IdempotencyKey key);

    boolean containsKey(IdempotencyKey key);

    void remove(IdempotencyKey key);

    void evictExpired();

    int size();

    void clear();
}

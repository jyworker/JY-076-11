package com.iyzipay.idempotency;

public interface IdempotencyAction<T> {
    T execute();
}

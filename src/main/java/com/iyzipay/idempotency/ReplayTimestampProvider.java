package com.iyzipay.idempotency;

public interface ReplayTimestampProvider {
    long getRequestTimestamp();
}

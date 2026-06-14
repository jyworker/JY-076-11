package com.iyzipay.idempotency;

import java.util.concurrent.atomic.AtomicLong;

public class IdempotencyRecord {

    public enum Status {
        SUCCESS,
        FAILURE,
        PENETRATION_MARKER
    }

    private final String idempotencyKey;
    private final String requestFingerprint;
    private final Object result;
    private final Status status;
    private final long createdAt;
    private final long requestTimestamp;
    private volatile long lastAccessedAt;
    private final AtomicLong accessCount;

    private IdempotencyRecord(Builder builder) {
        this.idempotencyKey = builder.idempotencyKey;
        this.requestFingerprint = builder.requestFingerprint;
        this.result = builder.result;
        this.status = builder.status;
        this.createdAt = builder.createdAt;
        this.requestTimestamp = builder.requestTimestamp;
        this.lastAccessedAt = builder.createdAt;
        this.accessCount = new AtomicLong(0);
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public Object getResult() {
        return result;
    }

    public Status getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getRequestTimestamp() {
        return requestTimestamp;
    }

    public long getLastAccessedAt() {
        return lastAccessedAt;
    }

    public long getAccessCount() {
        return accessCount.get();
    }

    public void recordAccess() {
        this.lastAccessedAt = System.currentTimeMillis();
        this.accessCount.incrementAndGet();
    }

    public boolean isExpired(long ttlMillis) {
        return (System.currentTimeMillis() - createdAt) > ttlMillis;
    }

    public boolean isPenetrationMarker() {
        return status == Status.PENETRATION_MARKER;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String idempotencyKey;
        private String requestFingerprint;
        private Object result;
        private Status status = Status.SUCCESS;
        private long createdAt = System.currentTimeMillis();
        private long requestTimestamp = System.currentTimeMillis();

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder requestFingerprint(String requestFingerprint) {
            this.requestFingerprint = requestFingerprint;
            return this;
        }

        public Builder result(Object result) {
            this.result = result;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder requestTimestamp(long requestTimestamp) {
            this.requestTimestamp = requestTimestamp;
            return this;
        }

        public IdempotencyRecord build() {
            return new IdempotencyRecord(this);
        }
    }
}

package com.iyzipay.idempotency;

public class IdempotencyConflictException extends RuntimeException {

    private final String idempotencyKey;
    private final String expectedFingerprint;
    private final String actualFingerprint;

    public IdempotencyConflictException(String idempotencyKey, String expectedFingerprint, String actualFingerprint) {
        super("Idempotency conflict for key: " + idempotencyKey
                + ". Expected fingerprint: " + expectedFingerprint
                + ", actual: " + actualFingerprint);
        this.idempotencyKey = idempotencyKey;
        this.expectedFingerprint = expectedFingerprint;
        this.actualFingerprint = actualFingerprint;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getExpectedFingerprint() {
        return expectedFingerprint;
    }

    public String getActualFingerprint() {
        return actualFingerprint;
    }
}

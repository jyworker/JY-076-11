package com.iyzipay.idempotency;

public class ReplayAttackDetectedException extends RuntimeException {

    private final long currentTimestamp;
    private final long requestTimestamp;
    private final long replayWindowMillis;

    public ReplayAttackDetectedException(long currentTimestamp, long requestTimestamp, long replayWindowMillis) {
        super("Replay attack detected. Request timestamp " + requestTimestamp
                + " is outside the allowed replay window of " + replayWindowMillis
                + "ms from current time " + currentTimestamp);
        this.currentTimestamp = currentTimestamp;
        this.requestTimestamp = requestTimestamp;
        this.replayWindowMillis = replayWindowMillis;
    }

    public long getCurrentTimestamp() {
        return currentTimestamp;
    }

    public long getRequestTimestamp() {
        return requestTimestamp;
    }

    public long getReplayWindowMillis() {
        return replayWindowMillis;
    }
}

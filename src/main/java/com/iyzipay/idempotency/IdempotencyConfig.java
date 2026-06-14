package com.iyzipay.idempotency;

public class IdempotencyConfig {

    private static final long DEFAULT_TTL_MILLIS = 24 * 60 * 60 * 1000L;
    private static final int DEFAULT_MAX_ENTRIES = 10000;
    private static final long DEFAULT_REPLAY_WINDOW_MILLIS = 5 * 60 * 1000L;
    private static final boolean DEFAULT_PENETRATION_PROTECTION_ENABLED = true;
    private static final long DEFAULT_PENETRATION_TTL_MILLIS = 5 * 60 * 1000L;
    private static final long DEFAULT_EVICTION_INTERVAL_MILLIS = 60 * 1000L;

    private final long ttlMillis;
    private final int maxEntries;
    private final long replayWindowMillis;
    private final boolean penetrationProtectionEnabled;
    private final long penetrationTtlMillis;
    private final long evictionIntervalMillis;

    private IdempotencyConfig(Builder builder) {
        this.ttlMillis = builder.ttlMillis;
        this.maxEntries = builder.maxEntries;
        this.replayWindowMillis = builder.replayWindowMillis;
        this.penetrationProtectionEnabled = builder.penetrationProtectionEnabled;
        this.penetrationTtlMillis = builder.penetrationTtlMillis;
        this.evictionIntervalMillis = builder.evictionIntervalMillis;
    }

    public long getTtlMillis() {
        return ttlMillis;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public long getReplayWindowMillis() {
        return replayWindowMillis;
    }

    public boolean isPenetrationProtectionEnabled() {
        return penetrationProtectionEnabled;
    }

    public long getPenetrationTtlMillis() {
        return penetrationTtlMillis;
    }

    public long getEvictionIntervalMillis() {
        return evictionIntervalMillis;
    }

    public static IdempotencyConfig defaultConfig() {
        return new Builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long ttlMillis = DEFAULT_TTL_MILLIS;
        private int maxEntries = DEFAULT_MAX_ENTRIES;
        private long replayWindowMillis = DEFAULT_REPLAY_WINDOW_MILLIS;
        private boolean penetrationProtectionEnabled = DEFAULT_PENETRATION_PROTECTION_ENABLED;
        private long penetrationTtlMillis = DEFAULT_PENETRATION_TTL_MILLIS;
        private long evictionIntervalMillis = DEFAULT_EVICTION_INTERVAL_MILLIS;

        public Builder ttlMillis(long ttlMillis) {
            if (ttlMillis <= 0) {
                throw new IllegalArgumentException("TTL must be positive");
            }
            this.ttlMillis = ttlMillis;
            return this;
        }

        public Builder maxEntries(int maxEntries) {
            if (maxEntries <= 0) {
                throw new IllegalArgumentException("Max entries must be positive");
            }
            this.maxEntries = maxEntries;
            return this;
        }

        public Builder replayWindowMillis(long replayWindowMillis) {
            if (replayWindowMillis < 0) {
                throw new IllegalArgumentException("Replay window must not be negative");
            }
            this.replayWindowMillis = replayWindowMillis;
            return this;
        }

        public Builder penetrationProtectionEnabled(boolean enabled) {
            this.penetrationProtectionEnabled = enabled;
            return this;
        }

        public Builder penetrationTtlMillis(long penetrationTtlMillis) {
            if (penetrationTtlMillis <= 0) {
                throw new IllegalArgumentException("Penetration TTL must be positive");
            }
            this.penetrationTtlMillis = penetrationTtlMillis;
            return this;
        }

        public Builder evictionIntervalMillis(long evictionIntervalMillis) {
            if (evictionIntervalMillis <= 0) {
                throw new IllegalArgumentException("Eviction interval must be positive");
            }
            this.evictionIntervalMillis = evictionIntervalMillis;
            return this;
        }

        public IdempotencyConfig build() {
            return new IdempotencyConfig(this);
        }
    }
}

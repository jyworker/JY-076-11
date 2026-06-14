package com.iyzipay.idempotency;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IdempotencyManagerTest {

    private static final String RESULT = "payment-success";
    private IdempotencyManager manager;
    private IdempotencyConfig config;
    private int executionCount;

    @Before
    public void setUp() {
        config = IdempotencyConfig.builder()
                .ttlMillis(5000)
                .maxEntries(100)
                .replayWindowMillis(0)
                .penetrationProtectionEnabled(true)
                .penetrationTtlMillis(2000)
                .build();
        manager = new IdempotencyManager(config);
        executionCount = 0;
    }

    @Test
    public void should_execute_action_when_no_cached_result() {
        IdempotencyKey key = IdempotencyKey.of("test-1");
        Object request = new TestRequest("data");

        String result = manager.execute(key, request, new IdempotencyAction<String>() {
            @Override
            public String execute() {
                executionCount++;
                return RESULT;
            }
        });

        assertEquals(RESULT, result);
        assertEquals(1, executionCount);
    }

    @Test
    public void should_return_cached_result_on_duplicate_submission() {
        IdempotencyKey key = IdempotencyKey.of("test-2");
        Object request = new TestRequest("data");

        String result1 = manager.execute(key, request, new IdempotencyAction<String>() {
            @Override
            public String execute() {
                executionCount++;
                return RESULT;
            }
        });

        String result2 = manager.execute(key, request, new IdempotencyAction<String>() {
            @Override
            public String execute() {
                executionCount++;
                return "different-result";
            }
        });

        assertEquals(result1, result2);
        assertEquals(1, executionCount);
    }

    @Test(expected = IdempotencyConflictException.class)
    public void should_detect_conflict_when_same_key_different_request() {
        IdempotencyKey key = IdempotencyKey.of("test-3");

        Object request1 = new TestRequest("data1");
        manager.execute(key, request1, new IdempotencyAction<String>() {
            @Override
            public String execute() {
                return RESULT;
            }
        });

        Object request2 = new TestRequest("data2");
        manager.execute(key, request2, new IdempotencyAction<String>() {
            @Override
            public String execute() {
                return "different";
            }
        });
    }

    @Test
    public void should_cache_failure_on_exception_when_penetration_protection_enabled() {
        IdempotencyKey key = IdempotencyKey.of("test-4");
        Object request = new TestRequest("data");

        try {
            manager.execute(key, request, new IdempotencyAction<String>() {
                @Override
                public String execute() {
                    throw new RuntimeException("payment failed");
                }
            });
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            assertEquals("payment failed", e.getMessage());
        }

        try {
            manager.execute(key, request, new IdempotencyAction<String>() {
                @Override
                public String execute() {
                    return "should-not-reach";
                }
            });
            fail("Should have thrown cached exception");
        } catch (RuntimeException e) {
            assertEquals("payment failed", e.getMessage());
        }
    }

    @Test
    public void should_not_cache_failure_when_penetration_protection_disabled() {
        IdempotencyConfig noProtectionConfig = IdempotencyConfig.builder()
                .ttlMillis(5000)
                .maxEntries(100)
                .replayWindowMillis(0)
                .penetrationProtectionEnabled(false)
                .build();
        IdempotencyManager noProtectionManager = new IdempotencyManager(noProtectionConfig);

        IdempotencyKey key = IdempotencyKey.of("test-5");
        Object request = new TestRequest("data");

        try {
            noProtectionManager.execute(key, request, new IdempotencyAction<String>() {
                @Override
                public String execute() {
                    throw new RuntimeException("payment failed");
                }
            });
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            assertEquals("payment failed", e.getMessage());
        }

        String result = noProtectionManager.execute(key, request, new IdempotencyAction<String>() {
            @Override
            public String execute() {
                return "retry-success";
            }
        });

        assertEquals("retry-success", result);
    }

    @Test
    public void should_detect_replay_attack() {
        IdempotencyConfig replayConfig = IdempotencyConfig.builder()
                .ttlMillis(5000)
                .maxEntries(100)
                .replayWindowMillis(5000)
                .build();
        IdempotencyManager replayManager = new IdempotencyManager(replayConfig);

        IdempotencyKey key = IdempotencyKey.of("test-6");
        TimestampedRequest oldRequest = new TimestampedRequest("data", System.currentTimeMillis() - 10000);

        try {
            replayManager.execute(key, oldRequest, new IdempotencyAction<String>() {
                @Override
                public String execute() {
                    return RESULT;
                }
            });
            fail("Should have detected replay attack");
        } catch (ReplayAttackDetectedException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void should_allow_request_within_replay_window() {
        IdempotencyConfig replayConfig = IdempotencyConfig.builder()
                .ttlMillis(5000)
                .maxEntries(100)
                .replayWindowMillis(5000)
                .build();
        IdempotencyManager replayManager = new IdempotencyManager(replayConfig);

        IdempotencyKey key = IdempotencyKey.of("test-7");
        TimestampedRequest request = new TimestampedRequest("data", System.currentTimeMillis() - 1000);

        String result = replayManager.execute(key, request, new IdempotencyAction<String>() {
            @Override
            public String execute() {
                return RESULT;
            }
        });

        assertEquals(RESULT, result);
    }

    @Test
    public void should_compute_consistent_fingerprint() {
        Object request = new TestRequest("same-data");
        String fp1 = manager.computeFingerprint(request);
        String fp2 = manager.computeFingerprint(request);
        assertEquals(fp1, fp2);
    }

    @Test
    public void should_compute_different_fingerprint_for_different_requests() {
        Object request1 = new TestRequest("data1");
        Object request2 = new TestRequest("data2");
        String fp1 = manager.computeFingerprint(request1);
        String fp2 = manager.computeFingerprint(request2);
        assertTrue(!fp1.equals(fp2));
    }

    @Test
    public void should_clear_cache() {
        IdempotencyKey key = IdempotencyKey.of("test-8");
        Object request = new TestRequest("data");

        manager.execute(key, request, new IdempotencyAction<String>() {
            @Override
            public String execute() {
                return RESULT;
            }
        });

        assertTrue(manager.getCache().size() > 0);
        manager.clearCache();
        assertEquals(0, manager.getCache().size());
    }

    @Test
    public void should_allow_different_keys_independently() {
        IdempotencyKey key1 = IdempotencyKey.of("key-1");
        IdempotencyKey key2 = IdempotencyKey.of("key-2");
        Object request = new TestRequest("data");

        String result1 = manager.execute(key1, request, new IdempotencyAction<String>() {
            @Override
            public String execute() {
                return "result-1";
            }
        });

        String result2 = manager.execute(key2, request, new IdempotencyAction<String>() {
            @Override
            public String execute() {
                return "result-2";
            }
        });

        assertEquals("result-1", result1);
        assertEquals("result-2", result2);
    }

    private static class TestRequest {
        private final String data;

        TestRequest(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }
    }

    private static class TimestampedRequest implements ReplayTimestampProvider {
        private final String data;
        private final long requestTimestamp;

        TimestampedRequest(String data, long requestTimestamp) {
            this.data = data;
            this.requestTimestamp = requestTimestamp;
        }

        public String getData() {
            return data;
        }

        @Override
        public long getRequestTimestamp() {
            return requestTimestamp;
        }
    }
}

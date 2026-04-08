package com.quantum.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReplayProtectionServiceTest {

    private ReplayProtectionService replayService;
    private static final String SESSION_KEY = "abcdef1234567890abcdef1234567890";

    @BeforeEach
    void setUp() {
        replayService = new ReplayProtectionService(300000, 300000, 100000);
    }

    @Test
    void validateRequest_validRequest_passes() {
        String nonce = "a1b2c3d4e5f6a7b8";
        long timestamp = System.currentTimeMillis();
        String sessionId = "test-session";
        String hmac = replayService.calculateHMAC(sessionId, nonce, timestamp, SESSION_KEY);

        assertTrue(replayService.validateRequest(sessionId, nonce, timestamp, hmac, SESSION_KEY));
    }

    @Test
    void validateRequest_expiredTimestamp_fails() {
        String nonce = "a1b2c3d4e5f6a7b8";
        long timestamp = System.currentTimeMillis() - 600000;
        String sessionId = "test-session";
        String hmac = replayService.calculateHMAC(sessionId, nonce, timestamp, SESSION_KEY);

        assertFalse(replayService.validateRequest(sessionId, nonce, timestamp, hmac, SESSION_KEY));
    }

    @Test
    void validateRequest_replayNonce_fails() {
        String nonce = "a1b2c3d4e5f6a7b8";
        long timestamp = System.currentTimeMillis();
        String sessionId = "test-session";
        String hmac = replayService.calculateHMAC(sessionId, nonce, timestamp, SESSION_KEY);

        assertTrue(replayService.validateRequest(sessionId, nonce, timestamp, hmac, SESSION_KEY));
        assertFalse(replayService.validateRequest(sessionId, nonce, timestamp, hmac, SESSION_KEY));
    }

    @Test
    void validateRequest_invalidHMAC_fails() {
        String nonce = "a1b2c3d4e5f6a7b8";
        long timestamp = System.currentTimeMillis();
        String sessionId = "test-session";

        assertFalse(replayService.validateRequest(sessionId, nonce, timestamp, "invalid-hmac", SESSION_KEY));
    }
}

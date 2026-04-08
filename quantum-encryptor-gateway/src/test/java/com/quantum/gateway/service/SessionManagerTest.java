package com.quantum.gateway.service;

import com.quantum.gateway.dto.SessionContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    @Test
    void createSession_returnsValidSession() {
        SessionManager sessionManager = new SessionManager(86400000, 10000);
        SessionContext session = sessionManager.createSession(
                "client-nonce-123",
                "Kyber768",
                "Dilithium2",
                "kyber-pub-hex",
                "kyber-priv-hex"
        );

        assertNotNull(session.getSessionId());
        assertEquals("Kyber768", session.getKyberAlgorithm());
        assertFalse(session.isExpired());
        assertFalse(session.isResumed());
    }

    @Test
    void getSession_returnsExistingSession() {
        SessionManager sessionManager = new SessionManager(86400000, 10000);
        SessionContext created = sessionManager.createSession(
                "nonce", "Kyber768", "Dilithium2", "pub", "priv"
        );

        SessionContext retrieved = sessionManager.getSession(created.getSessionId());

        assertEquals(created.getSessionId(), retrieved.getSessionId());
    }

    @Test
    void getSession_throwsForExpiredSession() {
        SessionManager shortLived = new SessionManager(1, 10000);
        SessionContext session = shortLived.createSession(
                "nonce", "Kyber768", "Dilithium2", "pub", "priv"
        );

        try { Thread.sleep(10); } catch (InterruptedException e) {}

        assertThrows(IllegalStateException.class,
                () -> shortLived.getSession(session.getSessionId()));
    }

    @Test
    void resumeSession_createsNewSessionWithSameKeys() {
        SessionManager sessionManager = new SessionManager(86400000, 10000);
        SessionContext original = sessionManager.createSession(
                "nonce", "Kyber768", "Dilithium2", "pub", "priv"
        );

        SessionContext resumed = sessionManager.resumeSession(
                original.getSessionId(),
                "new-nonce"
        );

        assertTrue(resumed.isResumed());
        assertNotEquals(original.getSessionId(), resumed.getSessionId());
        assertEquals(original.getKyberPublicKey(), resumed.getKyberPublicKey());
    }

    @Test
    void resumeSession_throwsForExpiredSession() {
        SessionManager shortLived = new SessionManager(1, 10000);
        SessionContext session = shortLived.createSession(
                "nonce", "Kyber768", "Dilithium2", "pub", "priv"
        );

        try { Thread.sleep(10); } catch (InterruptedException e) {}

        assertThrows(IllegalStateException.class,
                () -> shortLived.resumeSession(session.getSessionId(), "new-nonce"));
    }

    @Test
    void updateSessionKeys_storesKeysInSession() {
        SessionManager sessionManager = new SessionManager(86400000, 10000);
        SessionContext session = sessionManager.createSession(
                "nonce", "Kyber768", "Dilithium2", "pub", "priv"
        );

        sessionManager.updateSessionKeys(
                session.getSessionId(),
                "sm2-pub", "sm2-priv",
                "dilithium-pub", "dilithium-priv"
        );

        SessionContext updated = sessionManager.getSession(session.getSessionId());
        assertEquals("sm2-pub", updated.getSm2PublicKey());
        assertEquals("dilithium-pub", updated.getDilithiumPublicKey());
    }

    @Test
    void isValidSession_returnsTrueForValidSession() {
        SessionManager sessionManager = new SessionManager(86400000, 10000);
        SessionContext session = sessionManager.createSession(
                "nonce", "Kyber768", "Dilithium2", "pub", "priv"
        );

        assertTrue(sessionManager.isValidSession(session.getSessionId()));
    }

    @Test
    void isValidSession_returnsFalseForNonExistentSession() {
        SessionManager sessionManager = new SessionManager(86400000, 10000);
        assertFalse(sessionManager.isValidSession("non-existent-id"));
    }
}

package com.quantum.gateway.service;

import com.quantum.gateway.dto.SessionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionManager {

    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();
    private final long sessionTtlMs;
    private final int maxSessions;
    private final SecureRandom secureRandom = new SecureRandom();

    public SessionManager(
            @Value("${session.ttl-ms:86400000}") long sessionTtlMs,
            @Value("${session.max-sessions:10000}") int maxSessions) {
        this.sessionTtlMs = sessionTtlMs;
        this.maxSessions = maxSessions;
    }

    public SessionContext createSession(String clientNonce, String kyberAlgorithm,
                                        String dilithiumAlgorithm, String kyberPublicKey,
                                        String kyberPrivateKey) {
        if (sessions.size() >= maxSessions) {
            evictExpiredSessions();
        }

        String sessionId = UUID.randomUUID().toString();
        String serverNonce = generateNonce();
        String psk = generateNonce();
        String pskHint = psk.substring(0, 16);

        SessionContext session = SessionContext.builder()
                .sessionId(sessionId)
                .clientNonce(clientNonce)
                .serverNonce(serverNonce)
                .kyberAlgorithm(kyberAlgorithm)
                .dilithiumAlgorithm(dilithiumAlgorithm)
                .kyberPublicKey(kyberPublicKey)
                .kyberPrivateKey(kyberPrivateKey)
                .psk(psk)
                .pskHint(pskHint)
                .createdAt(System.currentTimeMillis())
                .expiresAt(System.currentTimeMillis() + sessionTtlMs)
                .resumed(false)
                .build();

        sessions.put(sessionId, session);
        return session;
    }

    public SessionContext getSession(String sessionId) {
        SessionContext session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalStateException("Session not found: " + sessionId);
        }
        if (session.isExpired()) {
            sessions.remove(sessionId);
            throw new IllegalStateException("Session expired: " + sessionId);
        }
        return session;
    }

    public SessionContext resumeSession(String oldSessionId, String clientNonce) {
        SessionContext oldSession = getSession(oldSessionId);

        String newSessionId = UUID.randomUUID().toString();
        String newServerNonce = generateNonce();

        SessionContext newSession = SessionContext.builder()
                .sessionId(newSessionId)
                .clientNonce(clientNonce)
                .serverNonce(newServerNonce)
                .kyberAlgorithm(oldSession.getKyberAlgorithm())
                .dilithiumAlgorithm(oldSession.getDilithiumAlgorithm())
                .kyberPublicKey(oldSession.getKyberPublicKey())
                .kyberPrivateKey(oldSession.getKyberPrivateKey())
                .psk(oldSession.getPsk())
                .pskHint(oldSession.getPskHint())
                .sm2PublicKey(oldSession.getSm2PublicKey())
                .sm2PrivateKey(oldSession.getSm2PrivateKey())
                .dilithiumPublicKey(oldSession.getDilithiumPublicKey())
                .dilithiumPrivateKey(oldSession.getDilithiumPrivateKey())
                .createdAt(System.currentTimeMillis())
                .expiresAt(System.currentTimeMillis() + sessionTtlMs)
                .resumed(true)
                .build();

        sessions.put(newSessionId, newSession);
        sessions.remove(oldSessionId);
        return newSession;
    }

    public void updateSessionKeys(String sessionId, String sm2PublicKey,
                                  String sm2PrivateKey, String dilithiumPublicKey,
                                  String dilithiumPrivateKey) {
        SessionContext session = getSession(sessionId);
        session.setSm2PublicKey(sm2PublicKey);
        session.setSm2PrivateKey(sm2PrivateKey);
        session.setDilithiumPublicKey(dilithiumPublicKey);
        session.setDilithiumPrivateKey(dilithiumPrivateKey);
    }

    public boolean isValidSession(String sessionId) {
        try {
            getSession(sessionId);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private void evictExpiredSessions() {
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private String generateNonce() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

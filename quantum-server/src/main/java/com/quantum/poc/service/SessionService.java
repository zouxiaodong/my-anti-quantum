package com.quantum.poc.service;

import com.quantum.poc.model.CryptoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@Service
public class SessionService {
    
    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    private static final long SESSION_TIMEOUT_MINUTES = 30;
    
    private final Map<String, CryptoSession> sessions = new ConcurrentHashMap<>();
    
    public CryptoSession createSession(String kyberAlgorithm, String dilithiumAlgorithm) {
        String sessionId = UUID.randomUUID().toString();
        CryptoSession session = new CryptoSession();
        session.setSessionId(sessionId);
        session.setKyberAlgorithm(kyberAlgorithm);
        session.setDilithiumAlgorithm(dilithiumAlgorithm);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(SESSION_TIMEOUT_MINUTES));
        session.setInitialized(false);
        session.setKeyWrapped(false);
        
        sessions.put(sessionId, session);
        log.info("[Session] Created new session: {}", sessionId);
        return session;
    }
    
    public Optional<CryptoSession> getSession(String sessionId) {
        CryptoSession session = sessions.get(sessionId);
        if (session != null && session.getExpiresAt().isAfter(LocalDateTime.now())) {
            return Optional.of(session);
        }
        if (session != null) {
            sessions.remove(sessionId);
            log.info("[Session] Session expired and removed: {}", sessionId);
        }
        return Optional.empty();
    }
    
    public void updateSession(String sessionId, CryptoSession session) {
        if (sessions.containsKey(sessionId)) {
            sessions.put(sessionId, session);
            log.info("[Session] Updated session: {}", sessionId);
        }
    }
    
    public void deleteSession(String sessionId) {
        sessions.remove(sessionId);
        log.info("[Session] Deleted session: {}", sessionId);
    }
    
    public boolean isValidSession(String sessionId) {
        return getSession(sessionId).isPresent();
    }
}
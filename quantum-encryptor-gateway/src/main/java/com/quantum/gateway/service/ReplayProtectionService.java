package com.quantum.gateway.service;

import com.quantum.gateway.util.HmacUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

@Service
public class ReplayProtectionService {

    private final Cache<String, Long> nonceCache;
    private final long timestampWindowMs;

    public ReplayProtectionService(
            @Value("${replay.timestamp-window-ms:300000}") long timestampWindowMs,
            @Value("${replay.nonce-ttl-ms:300000}") long nonceTtlMs,
            @Value("${replay.nonce-cache-size:100000}") int cacheSize) {
        this.timestampWindowMs = timestampWindowMs;
        this.nonceCache = Caffeine.newBuilder()
                .expireAfterWrite(nonceTtlMs, TimeUnit.MILLISECONDS)
                .maximumSize(cacheSize)
                .build();
    }

    public boolean validateRequest(String sessionId, String nonce, long timestamp,
                                   String hmac, String sessionKey) {
        long currentTime = System.currentTimeMillis();
        if (Math.abs(currentTime - timestamp) > timestampWindowMs) {
            return false;
        }

        String nonceKey = sessionId + ":" + nonce;
        if (nonceCache.getIfPresent(nonceKey) != null) {
            return false;
        }

        String expectedHmac = calculateHMAC(sessionId, nonce, timestamp, sessionKey);
        if (!HmacUtil.constantTimeEquals(hmac, expectedHmac)) {
            return false;
        }

        nonceCache.put(nonceKey, currentTime);
        return true;
    }

    public String calculateHMAC(String sessionId, String nonce, long timestamp, String sessionKey) {
        return HmacUtil.hmacSHA256(sessionId + nonce + timestamp, sessionKey);
    }
}

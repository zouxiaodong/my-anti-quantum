package com.quantum.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionContext {
    private String sessionId;
    private String clientNonce;
    private String serverNonce;
    private String kyberAlgorithm;
    private String dilithiumAlgorithm;
    private String kyberPublicKey;
    private String kyberPrivateKey;
    private String dilithiumPublicKey;
    private String dilithiumPrivateKey;
    private String sm2PublicKey;
    private String sm2PrivateKey;
    private String psk;
    private String pskHint;
    private long createdAt;
    private long expiresAt;
    private boolean resumed;

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}

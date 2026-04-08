package com.quantum.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInitResponse {
    private String sessionId;
    private String kyberPublicKey;
    private String serverNonce;
    private long expiresAt;
}

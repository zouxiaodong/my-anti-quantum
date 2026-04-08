package com.quantum.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecryptRequest {
    private String sessionId;
    private String cipherText;
    private String dilithiumSignature;
    private String sm2Signature;
    private String iv;
}

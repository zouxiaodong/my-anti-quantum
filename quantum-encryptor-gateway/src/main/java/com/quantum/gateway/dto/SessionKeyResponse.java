package com.quantum.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionKeyResponse {
    private String sm2PublicKey;
    private String sm2PrivateKey;
    private String dilithiumPublicKey;
    private String dilithiumPrivateKey;
}

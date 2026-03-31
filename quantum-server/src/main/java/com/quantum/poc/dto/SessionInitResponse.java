package com.quantum.poc.dto;

import lombok.Data;

@Data
public class SessionInitResponse {
    private String sessionId;
    private String kyberAlgorithm;
    private String dilithiumAlgorithm;
    private String kyberPublicKey;
    private String kyberPrivateKey;
    private String message;
}
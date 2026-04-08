package com.quantum.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SessionInitRequest {
    @NotBlank
    private String clientNonce;
    @NotBlank
    private String kyberAlgorithm;
    @NotBlank
    private String dilithiumAlgorithm;
}

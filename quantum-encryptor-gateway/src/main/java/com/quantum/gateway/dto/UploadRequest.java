package com.quantum.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UploadRequest {
    @NotBlank
    private String cipherText;
    @NotBlank
    private String iv;
    @NotBlank
    private String dilithiumSignature;
    @NotBlank
    private String sm2Signature;
}

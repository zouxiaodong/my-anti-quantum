package com.quantum.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResumeRequest {
    @NotBlank
    private String sessionId;
    @NotBlank
    private String clientNonce;
    @NotBlank
    private String pskHint;
}

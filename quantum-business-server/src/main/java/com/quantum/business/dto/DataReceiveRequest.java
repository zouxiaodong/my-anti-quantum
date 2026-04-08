package com.quantum.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DataReceiveRequest {
    @NotBlank
    private String plainText;
    @NotBlank
    private String sessionId;
    private boolean dilithiumVerifyResult;
    private boolean sm2VerifyResult;
    private long timestamp;
}

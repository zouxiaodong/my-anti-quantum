package com.quantum.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessDataRequest {
    private String plainText;
    private String sessionId;
    private boolean dilithiumVerifyResult;
    private boolean sm2VerifyResult;
    private long timestamp;
}

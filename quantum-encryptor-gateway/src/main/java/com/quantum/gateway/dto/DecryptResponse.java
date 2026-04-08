package com.quantum.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecryptResponse {
    private String plainText;
    private boolean dilithiumVerifyResult;
    private boolean sm2VerifyResult;
    private String message;
}

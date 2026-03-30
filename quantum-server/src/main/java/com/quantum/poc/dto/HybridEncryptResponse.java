package com.quantum.poc.dto;

import lombok.Data;

@Data
public class HybridEncryptResponse {
    private String cipherText;
    private String signature;
}

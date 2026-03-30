package com.quantum.poc.dto;

import lombok.Data;

@Data
public class HybridDecryptResponse {
    private String plainText;
    private Boolean verifyResult;
}

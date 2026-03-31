package com.quantum.poc.dto;

import lombok.Data;

@Data
public class SessionDecryptResponse {
    private String plainText;
    private Boolean sm2VerifyResult;
}
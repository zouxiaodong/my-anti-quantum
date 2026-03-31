package com.quantum.poc.dto;

import lombok.Data;

@Data
public class SessionEncryptResponse {
    private String cipherText;
    private String signature;
    private String iv;
}
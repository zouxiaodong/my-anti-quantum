package com.quantum.poc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TestVerifyRequest {
    @NotBlank(message = "算法不能为空")
    private String algorithm;
    
    @NotBlank(message = "PQC私钥不能为空")
    private String pqcPrivateKey;
    
    @NotBlank(message = "加密的会话密钥不能为空")
    private String keyCipher;
    
    private String keyId;
    
    @NotBlank(message = "原始数据不能为空")
    private String data;
    
    @NotBlank(message = "密文不能为空")
    private String cipherText;
    
    @NotBlank(message = "签名不能为空")
    private String signature;
    
    private String signPublicKey;
    
    private String sm4Algorithm;
    
    private String iv;
}
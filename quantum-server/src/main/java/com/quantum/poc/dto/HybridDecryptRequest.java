package com.quantum.poc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HybridDecryptRequest {
    @NotBlank(message = "密文不能为空")
    private String cipherText;
    
    @NotBlank(message = "签名不能为空")
    private String signature;
    
    @NotBlank(message = "SM4密钥不能为空")
    private String sm4Key;
    
    private String sm4Algorithm = "SM4/CBC/NoPadding";
    
    private String signAlgorithm;
    
    @NotBlank(message = "签名公钥不能为空")
    private String signPublicKey;
}

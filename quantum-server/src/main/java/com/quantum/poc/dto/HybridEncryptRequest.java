package com.quantum.poc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HybridEncryptRequest {
    @NotBlank(message = "数据不能为空")
    private String data;
    
    @NotBlank(message = "SM4密钥不能为空")
    private String sm4Key;
    
    private String sm4Algorithm = "SM4/CBC/NoPadding";
    
    private String signAlgorithm;
    
    @NotBlank(message = "签名私钥不能为空")
    private String signPrivateKey;
}

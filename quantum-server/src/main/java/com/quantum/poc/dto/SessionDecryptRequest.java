package com.quantum.poc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SessionDecryptRequest {
    @NotBlank(message = "密文不能为空")
    private String cipherText;
    
    @NotBlank(message = "签名不能为空")
    private String signature;
    
    private String sm4Algorithm = "SM4/CBC/NoPadding";
    
    private String iv;
}
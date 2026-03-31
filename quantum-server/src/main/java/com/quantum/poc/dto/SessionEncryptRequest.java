package com.quantum.poc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SessionEncryptRequest {
    @NotBlank(message = "数据不能为空")
    private String data;
    
    private String sm4Algorithm = "SM4/CBC/NoPadding";
    
    private String iv;
}
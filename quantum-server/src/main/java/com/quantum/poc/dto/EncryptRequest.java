package com.quantum.poc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EncryptRequest {
    @NotBlank(message = "数据不能为空")
    private String data;
    
    @NotBlank(message = "密钥不能为空")
    private String keyData;
    
    @NotBlank(message = "算法不能为空")
    private String algorithm;
    
    private String iv;
}

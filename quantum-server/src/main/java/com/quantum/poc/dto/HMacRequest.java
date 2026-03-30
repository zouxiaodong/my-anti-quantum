package com.quantum.poc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HMacRequest {
    @NotBlank(message = "数据不能为空")
    private String data;
    
    @NotBlank(message = "密钥不能为空")
    private String key;
}

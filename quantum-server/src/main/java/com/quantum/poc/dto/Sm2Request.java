package com.quantum.poc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Sm2Request {
    @NotBlank(message = "数据不能为空")
    private String data;
    
    @NotBlank(message = "私钥不能为空")
    private String privateKey;
}

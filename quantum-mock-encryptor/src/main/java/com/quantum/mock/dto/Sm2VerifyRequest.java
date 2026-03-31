package com.quantum.mock.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Sm2VerifyRequest {
    @NotBlank(message = "数据不能为空")
    private String data;
    
    @NotBlank(message = "签名不能为空")
    private String signature;
    
    @NotBlank(message = "公钥不能为空")
    private String publicKey;
}
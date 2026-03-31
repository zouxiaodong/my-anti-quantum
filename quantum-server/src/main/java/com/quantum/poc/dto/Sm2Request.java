package com.quantum.poc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Sm2Request {
    @NotBlank(message = "数据不能为空")
    private String data;
    
    private String privateKey;
    private String publicKey;
}

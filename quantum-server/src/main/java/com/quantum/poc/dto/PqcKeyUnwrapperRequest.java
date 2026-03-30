package com.quantum.poc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PqcKeyUnwrapperRequest {
    @NotBlank(message = "算法不能为空")
    private String algorithm;
    
    @NotBlank(message = "密文不能为空")
    private String cipherText;
    
    @NotBlank(message = "PQC私钥不能为空")
    private String pqcPrikey;
}

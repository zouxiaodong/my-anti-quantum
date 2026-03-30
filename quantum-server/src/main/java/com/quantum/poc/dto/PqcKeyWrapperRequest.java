package com.quantum.poc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PqcKeyWrapperRequest {
    @NotBlank(message = "算法不能为空")
    private String algorithm;
    
    @NotBlank(message = "PQC公钥不能为空")
    private String pqcPubkey;
    
    @NotBlank(message = "对称密钥不能为空")
    private String symmetricKey;
}

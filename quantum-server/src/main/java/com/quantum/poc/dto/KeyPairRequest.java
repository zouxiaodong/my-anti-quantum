package com.quantum.poc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KeyPairRequest {
    @NotBlank(message = "算法不能为空")
    private String algorithm;
}

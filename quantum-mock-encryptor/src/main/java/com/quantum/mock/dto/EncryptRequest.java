package com.quantum.mock.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EncryptRequest {
    @NotBlank(message = "算法不能为空")
    private String algorithm;

    @NotBlank(message = "数据不能为空")
    private String data;

    private String iv;

    @NotBlank(message = "密钥不能为空")
    private String keyData;
}

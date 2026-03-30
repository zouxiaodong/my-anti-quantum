package com.quantum.mock.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Sm2Request {
    @NotBlank(message = "数据不能为空")
    private String data;

    @NotBlank(message = "密钥不能为空")
    private String privateKey;
}

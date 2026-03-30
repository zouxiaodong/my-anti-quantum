package com.quantum.mock.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HashRequest {
    @NotBlank(message = "算法不能为空")
    private String algorithm;

    @NotBlank(message = "数据不能为空")
    private String data;
}

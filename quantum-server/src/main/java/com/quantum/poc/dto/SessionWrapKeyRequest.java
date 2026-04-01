package com.quantum.poc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SessionWrapKeyRequest {
    @NotBlank(message = "会话密钥不能为空")
    private String sessionKey;
}
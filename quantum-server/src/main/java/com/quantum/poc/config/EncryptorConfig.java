package com.quantum.poc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "encryptor")
public class EncryptorConfig {
    private String host;
    private Integer port;
    private String baseUrl;
    private Long timeout;
}

package com.quantum.poc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    
    private final EncryptorConfig encryptorConfig;
    
    public WebClientConfig(EncryptorConfig encryptorConfig) {
        this.encryptorConfig = encryptorConfig;
    }
    
    @Bean
    public WebClient encryptorWebClient() {
        return WebClient.builder()
                .baseUrl(encryptorConfig.getBaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}

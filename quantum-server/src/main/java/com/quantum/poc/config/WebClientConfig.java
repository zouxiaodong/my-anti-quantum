package com.quantum.poc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig {
    
    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);
    
    private final EncryptorConfig encryptorConfig;
    
    public WebClientConfig(EncryptorConfig encryptorConfig) {
        this.encryptorConfig = encryptorConfig;
    }
    
    @Bean
    public WebClient encryptorWebClient() {
        return WebClient.builder()
                .baseUrl(encryptorConfig.getBaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .filter(loggingFilter())
                .build();
    }
    
    private ExchangeFilterFunction loggingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("==================== HTTP 请求 ====================");
            log.info("  URL: {} {}", clientRequest.method(), clientRequest.url());
            log.info("  Headers: {}", clientRequest.headers());
            if (log.isDebugEnabled()) {
                log.debug("  Request body: {}", clientRequest.body());
            }
            log.info("====================================================");
            return Mono.just(clientRequest);
        }).andThen(ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("==================== HTTP 响应 ====================");
            log.info("  Status: {}", clientResponse.statusCode());
            log.info("  Headers: {}", clientResponse.headers());
            log.info("====================================================");
            return Mono.just(clientResponse);
        }));
    }
}

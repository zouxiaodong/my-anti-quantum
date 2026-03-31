package com.quantum.poc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
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
        return (clientRequest, next) -> {
            log.info("[请求] {} {}", clientRequest.method(), clientRequest.url());
            return next.exchange(clientRequest)
                .doOnNext(clientResponse -> {
                    HttpStatus status = (HttpStatus) clientResponse.statusCode();
                    log.info("[响应] HTTP {} - {}", status.value(), status.getReasonPhrase());
                });
        };
    }
}

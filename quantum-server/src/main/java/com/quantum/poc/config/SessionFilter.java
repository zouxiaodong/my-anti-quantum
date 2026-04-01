package com.quantum.poc.config;

import com.quantum.poc.service.SessionService;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Order(1)
public class SessionFilter implements WebFilter {
    
    private static final String SESSION_HEADER = "X-Session-Id";
    private static final String SESSION_ATTR = "cryptoSession";
    
    private static final List<String> EXEMPT_PATHS = List.of(
        "/api/crypto/session/init",
        "/api/crypto/session/genRandom",
        "/api/crypto/genRandom"
    );

    private final SessionService sessionService;

    public SessionFilter(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        
        if (requiresSession(path)) {
            String sessionId = exchange.getRequest().getHeaders().getFirst(SESSION_HEADER);
            
            if (sessionId == null || sessionId.isBlank()) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                return exchange.getResponse().writeWith(Mono.just(
                    exchange.getResponse().bufferFactory().wrap(
                        "{\"code\":401,\"msg\":\"Missing X-Session-Id header\"}".getBytes()
                    )
                ));
            }
            
            var sessionOpt = sessionService.getSession(sessionId);
            if (sessionOpt.isEmpty()) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                return exchange.getResponse().writeWith(Mono.just(
                    exchange.getResponse().bufferFactory().wrap(
                        "{\"code\":401,\"msg\":\"Invalid or expired session\"}".getBytes()
                    )
                ));
            }
            
            exchange.getAttributes().put(SESSION_ATTR, sessionOpt.get());
        }
        
        return chain.filter(exchange);
    }
    
    private boolean requiresSession(String path) {
        return EXEMPT_PATHS.stream().noneMatch(path::startsWith);
    }
}
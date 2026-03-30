package com.quantum.poc.exception;

import com.quantum.poc.dto.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Result<Map<String, String>>> handleWebClientException(WebClientResponseException e) {
        log.error("==================== 加密机连接异常 ====================");
        log.error("  异常类型: WebClientResponseException");
        log.error("  HTTP状态码: {}", e.getStatusCode());
        log.error("  响应内容: {}", e.getResponseBodyAsString());
        log.error("  异常消息: {}", e.getMessage());
        log.error("  异常原因: {}", e.getCause() != null ? e.getCause().getMessage() : "无");
        log.error("=======================================================");
        
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Result.error(40002, "加密机连接失败: " + e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Map<String, String>>> handleException(Exception e) {
        log.error("==================== 服务器内部异常 ====================");
        log.error("  异常类型: {}", e.getClass().getName());
        log.error("  异常消息: {}", e.getMessage());
        log.error("  异常原因: {}", e.getCause() != null ? e.getCause().getMessage() : "无");
        
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace.length > 0) {
            log.error("  异常堆栈 (前5行):");
            for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                log.error("    at {}", stackTrace[i]);
            }
        }
        log.error("=======================================================");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "服务器内部错误: " + e.getMessage()));
    }
}

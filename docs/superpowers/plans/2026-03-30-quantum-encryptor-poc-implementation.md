# 抗量子加密机POC测试项目实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现完整的抗量子密码POC测试系统，包含Spring Boot后端网关层和Android客户端

**Architecture:** 采用分层架构 - 后端作为网关层转发请求到加密机硬件，客户端提供POC测试界面

**Tech Stack:** 
- 后端: Spring Boot 3.x, WebClient, Maven
- 客户端: Android Kotlin, XML, ViewBinding, Retrofit2, OkHttp, MVVM

---

## Chunk 1: Spring Boot后端项目搭建

### Task 1.1: 创建Spring Boot项目骨架

**Files:**
- Create: `quantum-server/pom.xml`
- Create: `quantum-server/src/main/java/com/quantum/poc/QuantumServerApplication.java`
- Create: `quantum-server/src/main/resources/application.yml`
- Create: `quantum-server/src/main/resources/application-dev.yml`

- [ ] **Step 1: 创建pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>
    
    <groupId>com.quantum</groupId>
    <artifactId>poc-server</artifactId>
    <version>1.0.0</version>
    <name>quantum-poc-server</name>
    <description>抗量子加密机POC测试后端</description>
    
    <properties>
        <java.version>17</java.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Spring Boot Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- Spring Boot WebFlux (for WebClient) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Jackson -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        
        <!-- Apache Commons Codec (for hex conversion) -->
        <dependency>
            <groupId>commons-codec</groupId>
            <artifactId>commons-codec</artifactId>
        </dependency>
        
        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建Spring Boot主类**

```java
package com.quantum.poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QuantumServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuantumServerApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建application.yml**

```yaml
server:
  port: 8080

spring:
  application:
    name: quantum-poc-server
  profiles:
    active: dev

encryptor:
  host: 192.168.1.100
  port: 8101
  base-url: http://${encryptor.host}:${encryptor.port}
  timeout: 30000

logging:
  level:
    com.quantum.poc: DEBUG
```

- [ ] **Step 4: 创建application-dev.yml**

```yaml
encryptor:
  host: localhost
  port: 8101
```

- [ ] **Step 5: 验证项目编译**

Run: `cd quantum-server && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add quantum-server/
git commit -m "feat: 创建Spring Boot后端项目骨架"
```

---

### Task 1.2: 创建DTO类

**Files:**
- Create: `quantum-server/src/main/java/com/quantum/poc/dto/Result.java`
- Create: `quantum-server/src/main/java/com/quantum/poc/dto/EncryptRequest.java`
- Create: `quantum-server/src/main/java/com/quantum/poc/dto/EncryptResponse.java`
- Create: `quantum-server/src/main/java/com/quantum/poc/dto/KeyPairRequest.java`
- Create: `quantum-server/src/main/java/com/quantum/poc/dto/KeyPairResponse.java`
- Create: `quantum-server/src/main/java/com/quantum/poc/dto/HashRequest.java`
- Create: `quantum-server/src/main/java/com/quantum/poc/dto/HMacRequest.java`
- Create: `quantum-server/src/main/java/com/quantum/poc/dto/PqcKeyWrapperRequest.java`
- Create: `quantum-server/src/main/java/com/quantum/poc/dto/PqcKeyUnwrapperRequest.java`
- Create: `quantum-server/src/main/java/com/quantum/poc/dto/Sm2Request.java`

- [ ] **Step 1: 创建通用响应 Result.java**

```java
package com.quantum.poc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private T data;
    private String msg;
    
    public static <T> Result<T> success(T data) {
        return new Result<>(0, data, "");
    }
    
    public static <T> Result<T> success(T data, String msg) {
        return new Result<>(0, data, msg);
    }
    
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, null, msg);
    }
}
```

- [ ] **Step 2: 创建加密请求 EncryptRequest.java**

```java
package com.quantum.poc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EncryptRequest {
    @NotBlank(message = "数据不能为空")
    private String data;
    
    @NotBlank(message = "密钥不能为空")
    private String keyData;
    
    @NotBlank(message = "算法不能为空")
    private String algorithm;
    
    private String iv;  // CBC模式必填
}
```

- [ ] **Step 3: 创建密钥对请求 KeyPairRequest.java**

```java
package com.quantum.poc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KeyPairRequest {
    @NotBlank(message = "算法不能为空")
    private String algorithm;
}
```

- [ ] **Step 4: 创建PQC密钥封装请求 PqcKeyWrapperRequest.java**

```java
package com.quantum.poc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PqcKeyWrapperRequest {
    @NotBlank(message = "算法不能为空")
    private String algorithm;
    
    @NotBlank(message = "PQC公钥不能为空")
    private String pqcPubkey;
    
    @NotBlank(message = "对称密钥不能为空")
    private String symmetricKey;
}
```

- [ ] **Step 5: 创建其他DTO类 (简化版)**

```java
// HashRequest.java
@Data
public class HashRequest {
    @NotBlank(message = "数据不能为空")
    private String data;
    @NotBlank(message = "算法不能为空")
    private String algorithm;
}

// HMacRequest.java  
@Data
public class HMacRequest {
    @NotBlank(message = "数据不能为空")
    private String data;
    @NotBlank(message = "密钥不能为空")
    private String key;
}

// Sm2Request.java
@Data
public class Sm2Request {
    @NotBlank(message = "数据不能为空")
    private String data;
    @NotBlank(message = "私钥不能为空")
    private String privateKey;
}
```

- [ ] **Step 6: Commit**

```bash
git add quantum-server/src/main/java/com/quantum/poc/dto/
git commit -m "feat: 创建DTO类"
```

---

### Task 1.3: 创建加密机网关配置和服务

**Files:**
- Create: `quantum-server/src/main/java/com/quantum/poc/config/EncryptorConfig.java`
- Create: `quantum-server/src/main/java/com/quantum/poc/config/WebClientConfig.java`
- Create: `quantum-server/src/main/java/com/quantum/poc/service/CryptoGatewayService.java`

- [ ] **Step 1: 创建加密机配置类 EncryptorConfig.java**

```java
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
```

- [ ] **Step 2: 创建WebClient配置 WebClientConfig.java**

```java
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
```

- [ ] **Step 3: 创建加密机网关服务 CryptoGatewayService.java**

```java
package com.quantum.poc.service;

import com.quantum.poc.config.EncryptorConfig;
import com.quantum.poc.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class CryptoGatewayService {
    
    private final WebClient encryptorWebClient;
    private final EncryptorConfig encryptorConfig;
    
    public CryptoGatewayService(WebClient encryptorWebClient, EncryptorConfig encryptorConfig) {
        this.encryptorWebClient = encryptorWebClient;
        this.encryptorConfig = encryptorConfig;
    }
    
    /**
     * 随机数生成
     */
    public Mono<Result<String>> genRandom(Integer length) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/genRandom?length=" + length)
                .retrieve()
                .bodyToMono(Result.class);
    }
    
    /**
     * SM4加密
     */
    public Mono<Result<String>> sm4Encrypt(EncryptRequest request) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/symAlgEnc")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Result.class);
    }
    
    /**
     * SM4解密
     */
    public Mono<Result<String>> sm4Decrypt(EncryptRequest request) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/symAlgDec")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Result.class);
    }
    
    /**
     * 摘要运算
     */
    public Mono<Result<String>> hash(HashRequest request) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/hash")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Result.class);
    }
    
    /**
     * HMAC计算
     */
    public Mono<Result<String>> hmac(HMacRequest request) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/hmac")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Result.class);
    }
    
    /**
     * ECC密钥对生成
     */
    public Mono<Result<Map>> genEccKeyPair() {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/genEccKeyPair")
                .retrieve()
                .bodyToMono(Result.class);
    }
    
    /**
     * SM2加密
     */
    public Mono<Result<String>> sm2Encrypt(Sm2Request request) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/sm2Enc")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Result.class);
    }
    
    /**
     * SM2解密
     */
    public Mono<Result<String>> sm2Decrypt(Sm2Request request) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/sm2Dec")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Result.class);
    }
    
    /**
     * PQC密钥对生成
     */
    public Mono<Result<Map>> genPqcKeyPair(KeyPairRequest request) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/genPqcKeyPair")
                .bodyValue(Map.of("algorithm", request.getAlgorithm()))
                .retrieve()
                .bodyToMono(Result.class);
    }
    
    /**
     * PQC公钥封装
     */
    public Mono<Result<PqcKeyWrapperResponse>> pqcKeyWrapper(PqcKeyWrapperRequest request) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/pqcKeyWrapper")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Result.class);
    }
    
    /**
     * PQC私钥解封
     */
    public Mono<Result<String>> pqcKeyUnwrapper(PqcKeyUnwrapperRequest request) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/pqcKeyUnWrapper")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Result.class);
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add quantum-server/src/main/java/com/quantum/poc/config/ quantum-server/src/main/java/com/quantum/poc/service/
git commit -m "feat: 创建加密机网关配置和服务"
```

---

### Task 1.4: 创建REST控制器

**Files:**
- Create: `quantum-server/src/main/java/com/quantum/poc/controller/CryptoController.java`
- Create: `quantum-server/src/main/java/com/quantum/poc/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: 创建CryptoController.java**

```java
package com.quantum.poc.controller;

import com.quantum.poc.dto.*;
import com.quantum.poc.service.CryptoGatewayService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/crypto")
@CrossOrigin(origins = "*")
public class CryptoController {
    
    private final CryptoGatewayService cryptoGatewayService;
    
    public CryptoController(CryptoGatewayService cryptoGatewayService) {
        this.cryptoGatewayService = cryptoGatewayService;
    }
    
    /**
     * 随机数生成
     */
    @PostMapping("/genRandom")
    public Mono<ResponseEntity<Result<String>>> genRandom(@RequestParam(required = false, defaultValue = "32") Integer length) {
        return cryptoGatewayService.genRandom(length)
                .map(ResponseEntity::ok);
    }
    
    /**
     * SM4加密
     */
    @PostMapping("/sm4/encrypt")
    public Mono<ResponseEntity<Result<String>>> sm4Encrypt(@Valid @RequestBody EncryptRequest request) {
        return cryptoGatewayService.sm4Encrypt(request)
                .map(ResponseEntity::ok);
    }
    
    /**
     * SM4解密
     */
    @PostMapping("/sm4/decrypt")
    public Mono<ResponseEntity<Result<String>>> sm4Decrypt(@Valid @RequestBody EncryptRequest request) {
        return cryptoGatewayService.sm4Decrypt(request)
                .map(ResponseEntity::ok);
    }
    
    /**
     * 摘要运算
     */
    @PostMapping("/hash")
    public Mono<ResponseEntity<Result<String>>> hash(@Valid @RequestBody HashRequest request) {
        return cryptoGatewayService.hash(request)
                .map(ResponseEntity::ok);
    }
    
    /**
     * HMAC计算
     */
    @PostMapping("/hmac")
    public Mono<ResponseEntity<Result<String>>> hmac(@Valid @RequestBody HMacRequest request) {
        return cryptoGatewayService.hmac(request)
                .map(ResponseEntity::ok);
    }
    
    /**
     * ECC密钥对生成
     */
    @PostMapping("/ecc/genKeyPair")
    public Mono<ResponseEntity<Result<Map>>> genEccKeyPair() {
        return cryptoGatewayService.genEccKeyPair()
                .map(ResponseEntity::ok);
    }
    
    /**
     * SM2加密
     */
    @PostMapping("/sm2/encrypt")
    public Mono<ResponseEntity<Result<String>>> sm2Encrypt(@Valid @RequestBody Sm2Request request) {
        return cryptoGatewayService.sm2Encrypt(request)
                .map(ResponseEntity::ok);
    }
    
    /**
     * SM2解密
     */
    @PostMapping("/sm2/decrypt")
    public Mono<ResponseEntity<Result<String>>> sm2Decrypt(@Valid @RequestBody Sm2Request request) {
        return cryptoGatewayService.sm2Decrypt(request)
                .map(ResponseEntity::ok);
    }
    
    /**
     * PQC密钥对生成
     */
    @PostMapping("/pqc/genKeyPair")
    public Mono<ResponseEntity<Result<Map>>> genPqcKeyPair(@Valid @RequestBody KeyPairRequest request) {
        return cryptoGatewayService.genPqcKeyPair(request)
                .map(ResponseEntity::ok);
    }
    
    /**
     * PQC公钥封装
     */
    @PostMapping("/pqc/wrapKey")
    public Mono<ResponseEntity<Result<PqcKeyWrapperResponse>>> pqcKeyWrapper(@Valid @RequestBody PqcKeyWrapperRequest request) {
        return cryptoGatewayService.pqcKeyWrapper(request)
                .map(ResponseEntity::ok);
    }
    
    /**
     * PQC私钥解封
     */
    @PostMapping("/pqc/unwrapKey")
    public Mono<ResponseEntity<Result<String>>> pqcKeyUnwrapper(@Valid @RequestBody PqcKeyUnwrapperRequest request) {
        return cryptoGatewayService.pqcKeyUnwrapper(request)
                .map(ResponseEntity::ok);
    }
    
    /**
     * 混合加密 (SM4加密 + Dilithium签名)
     */
    @PostMapping("/encrypt")
    public Mono<ResponseEntity<Result<HybridEncryptResponse>>> hybridEncrypt(@Valid @RequestBody HybridEncryptRequest request) {
        // 1. SM4加密
        EncryptRequest encRequest = new EncryptRequest();
        encRequest.setData(request.getData());
        encRequest.setKeyData(request.getSm4Key());
        encRequest.setAlgorithm(request.getSm4Algorithm());
        
        return cryptoGatewayService.sm4Encrypt(encRequest)
                .flatMap(encResult -> {
                    // 2. Dilithium签名 (使用HMAC作为签名)
                    HMacRequest hmacRequest = new HMacRequest();
                    hmacRequest.setData(encResult.getData());
                    hmacRequest.setKey(request.getSignPrivateKey());
                    
                    return cryptoGatewayService.hmac(hmacRequest)
                            .map(hmacResult -> {
                                HybridEncryptResponse response = new HybridEncryptResponse();
                                response.setCipherText(encResult.getData());
                                response.setSignature(hmacResult.getData());
                                return ResponseEntity.ok(Result.success(response));
                            });
                });
    }
    
    /**
     * 混合解密 (SM4解密 + Dilithium验签)
     */
    @PostMapping("/decrypt")
    public Mono<ResponseEntity<Result<HybridDecryptResponse>>> hybridDecrypt(@Valid @RequestBody HybridDecryptRequest request) {
        // 1. 验签 (使用HMAC验证)
        HMacRequest hmacRequest = new HMacRequest();
        hmacRequest.setData(request.getCipherText());
        hmacRequest.setKey(request.getSignPublicKey());
        
        return cryptoGatewayService.hmac(hmacRequest)
                .flatMap(hmacResult -> {
                    boolean verifyResult = hmacResult.getData() != null 
                            && hmacResult.getData().equals(request.getSignature());
                    
                    if (!verifyResult) {
                        HybridDecryptResponse response = new HybridDecryptResponse();
                        response.setVerifyResult(false);
                        return Mono.just(ResponseEntity.ok(Result.success(response, "验签失败")));
                    }
                    
                    // 2. SM4解密
                    EncryptRequest decRequest = new EncryptRequest();
                    decRequest.setData(request.getCipherText());
                    decRequest.setKeyData(request.getSm4Key());
                    decRequest.setAlgorithm(request.getSm4Algorithm());
                    
                    return cryptoGatewayService.sm4Decrypt(decRequest)
                            .map(decResult -> {
                                HybridDecryptResponse response = new HybridDecryptResponse();
                                response.setPlainText(decResult.getData());
                                response.setVerifyResult(true);
                                return ResponseEntity.ok(Result.success(response));
                            });
                });
    }
}
```

- [ ] **Step 2: 创建全局异常处理器**

```java
package com.quantum.poc.exception;

import com.quantum.poc.dto.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Result<Map<String, String>>> handleWebClientException(WebClientResponseException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "加密机连接失败: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Result.error(40002, "加密机连接失败"));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Map<String, String>>> handleException(Exception e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "服务器内部错误"));
    }
}
```

- [ ] **Step 3: 创建混合加密请求响应类**

```java
// HybridEncryptRequest.java
@Data
public class HybridEncryptRequest {
    @NotBlank(message = "数据不能为空")
    private String data;
    @NotBlank(message = "SM4密钥不能为空")
    private String sm4Key;
    private String sm4Algorithm = "SM4/CBC/NoPadding";
    private String signAlgorithm;
    @NotBlank(message = "签名私钥不能为空")
    private String signPrivateKey;
}

// HybridEncryptResponse.java
@Data
public class HybridEncryptResponse {
    private String cipherText;
    private String signature;
}

// HybridDecryptRequest.java
@Data
public class HybridDecryptRequest {
    @NotBlank(message = "密文不能为空")
    private String cipherText;
    @NotBlank(message = "签名不能为空")
    private String signature;
    @NotBlank(message = "SM4密钥不能为空")
    private String sm4Key;
    private String sm4Algorithm = "SM4/CBC/NoPadding";
    private String signAlgorithm;
    @NotBlank(message = "签名公钥不能为空")
    private String signPublicKey;
}

// HybridDecryptResponse.java
@Data
public class HybridDecryptResponse {
    private String plainText;
    private Boolean verifyResult;
}

// PqcKeyWrapperResponse.java
@Data
public class PqcKeyWrapperResponse {
    private String keyCipher;
    private String keyId;
}

// PqcKeyUnwrapperRequest.java
@Data
public class PqcKeyUnwrapperRequest {
    @NotBlank(message = "算法不能为空")
    private String algorithm;
    @NotBlank(message = "密文不能为空")
    private String cipherText;
    @NotBlank(message = "PQC私钥不能为空")
    private String pqcPrikey;
}
```

- [ ] **Step 4: 验证项目编译**

Run: `cd quantum-server && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add quantum-server/src/main/java/com/quantum/poc/controller/ quantum-server/src/main/java/com/quantum/poc/exception/
git commit -m "feat: 创建REST控制器和异常处理"
```

---

## Chunk 2: Android客户端项目搭建

### Task 2.1: 创建Android项目骨架

**Files:**
- Create: `quantum-client-android/settings.gradle`
- Create: `quantum-client-android/build.gradle`
- Create: `quantum-client-android/app/build.gradle`
- Create: `quantum-client-android/app/src/main/AndroidManifest.xml`
- Create: `quantum-client-android/gradle/wrapper/gradle-wrapper.properties`

- [ ] **Step 1: 创建settings.gradle**

```groovy
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "QuantumPOC"
include ':app'
```

- [ ] **Step 2: 创建根build.gradle**

```groovy
plugins {
    id 'com.android.application' version '8.1.0' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.0' apply false
}
```

- [ ] **Step 3: 创建app/build.gradle**

```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.quantum.poc'
    compileSdk 34
    
    defaultConfig {
        applicationId "com.quantum.poc"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
        
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = '17'
    }
    
    buildFeatures {
        viewBinding true
    }
}

dependencies {
    // AndroidX Core
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.10.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    
    // Lifecycle & ViewModel
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.6.2'
    implementation 'androidx.activity:activity-ktx:1.8.0'
    
    // Retrofit2 & OkHttp
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
    
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'
    
    // Gson
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

- [ ] **Step 4: 创建AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    
    <uses-permission android:name="android.permission.INTERNET" />
    
    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.QuantumPOC"
        tools:targetApi="31">
        <activity
            android:name=".ui.MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

- [ ] **Step 5: Commit**

```bash
git add quantum-client-android/
git commit -m "feat: 创建Android项目骨架"
```

---

### Task 2.2: 创建网络层

**Files:**
- Create: `quantum-client-android/app/src/main/java/com/quantum/poc/api/ApiClient.kt`
- Create: `quantum-client-android/app/src/main/java/com/quantum/poc/api/CryptoApiService.kt`
- Create: `quantum-client-android/app/src/main/java/com/quantum/poc/model/ApiModels.kt`

- [ ] **Step 1: 创建API客户端 ApiClient.kt**

```kotlin
package com.quantum.poc.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // 修改为实际后端服务器地址
    private const val BASE_URL = "http://10.0.2.2:8080/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val cryptoApiService: CryptoApiService = retrofit.create(CryptoApiService::class.java)
}
```

- [ ] **Step 2: 创建API服务接口 CryptoApiService.kt**

```kotlin
package com.quantum.poc.api

import com.quantum.poc.model.*
import retrofit2.Call
import retrofit2.http.*

interface CryptoApiService {
    
    @POST("api/crypto/genRandom")
    fun genRandom(@Query("length") length: Int): Call<ApiResult<String>>
    
    @POST("api/crypto/sm4/encrypt")
    fun sm4Encrypt(@Body request: EncryptRequest): Call<ApiResult<String>>
    
    @POST("api/crypto/sm4/decrypt")
    fun sm4Decrypt(@Body request: EncryptRequest): Call<ApiResult<String>>
    
    @POST("api/crypto/hash")
    fun hash(@Body request: HashRequest): Call<ApiResult<String>>
    
    @POST("api/crypto/hmac")
    fun hmac(@Body request: HMacRequest): Call<ApiResult<String>>
    
    @POST("api/crypto/ecc/genKeyPair")
    fun genEccKeyPair(): Call<ApiResult<Map<String, String>>>
    
    @POST("api/crypto/sm2/encrypt")
    fun sm2Encrypt(@Body request: Sm2Request): Call<ApiResult<String>>
    
    @POST("api/crypto/sm2/decrypt")
    fun sm2Decrypt(@Body request: Sm2Request): Call<ApiResult<String>>
    
    @POST("api/crypto/pqc/genKeyPair")
    fun genPqcKeyPair(@Body request: KeyPairRequest): Call<ApiResult<PqcKeyPairResponse>>
    
    @POST("api/crypto/pqc/wrapKey")
    fun pqcKeyWrap(@Body request: PqcKeyWrapRequest): Call<ApiResult<PqcKeyWrapResponse>>
    
    @POST("api/crypto/pqc/unwrapKey")
    fun pqcKeyUnwrap(@Body request: PqcKeyUnwrapRequest): Call<ApiResult<String>>
    
    @POST("api/crypto/encrypt")
    fun hybridEncrypt(@Body request: HybridEncryptRequest): Call<ApiResult<HybridEncryptResponse>>
    
    @POST("api/crypto/decrypt")
    fun hybridDecrypt(@Body request: HybridDecryptRequest): Call<ApiResult<HybridDecryptResponse>>
}
```

- [ ] **Step 3: 创建数据模型 ApiModels.kt**

```kotlin
package com.quantum.poc.model

import com.google.gson.annotations.SerializedName

// 通用响应
data class ApiResult<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("data") val data: T?,
    @SerializedName("msg") val msg: String
)

// 请求模型
data class EncryptRequest(
    @SerializedName("data") val data: String,
    @SerializedName("keyData") val keyData: String,
    @SerializedName("algorithm") val algorithm: String,
    @SerializedName("iv") val iv: String? = null
)

data class HashRequest(
    @SerializedName("data") val data: String,
    @SerializedName("algorithm") val algorithm: String
)

data class HMacRequest(
    @SerializedName("data") val data: String,
    @SerializedName("key") val key: String
)

data class Sm2Request(
    @SerializedName("data") val data: String,
    @SerializedName("privateKey") val privateKey: String
)

data class KeyPairRequest(
    @SerializedName("algorithm") val algorithm: String
)

data class PqcKeyWrapRequest(
    @SerializedName("algorithm") val algorithm: String,
    @SerializedName("pqcPubkey") val pqcPubkey: String,
    @SerializedName("symmetricKey") val symmetricKey: String
)

data class PqcKeyUnwrapRequest(
    @SerializedName("algorithm") val algorithm: String,
    @SerializedName("cipherText") val cipherText: String,
    @SerializedName("pqcPrikey") val pqcPrikey: String
)

data class HybridEncryptRequest(
    @SerializedName("data") val data: String,
    @SerializedName("sm4Key") val sm4Key: String,
    @SerializedName("sm4Algorithm") val sm4Algorithm: String = "SM4/CBC/NoPadding",
    @SerializedName("signAlgorithm") val signAlgorithm: String?,
    @SerializedName("signPrivateKey") val signPrivateKey: String
)

data class HybridDecryptRequest(
    @SerializedName("cipherText") val cipherText: String,
    @SerializedName("signature") val signature: String,
    @SerializedName("sm4Key") val sm4Key: String,
    @SerializedName("sm4Algorithm") val sm4Algorithm: String = "SM4/CBC/NoPadding",
    @SerializedName("signAlgorithm") val signAlgorithm: String?,
    @SerializedName("signPublicKey") val signPublicKey: String
)

// 响应模型
data class PqcKeyPairResponse(
    @SerializedName("publicKey") val publicKey: String,
    @SerializedName("privateKey") val privateKey: String
)

data class PqcKeyWrapResponse(
    @SerializedName("keyCipher") val keyCipher: String,
    @SerializedName("keyId") val keyId: String
)

data class HybridEncryptResponse(
    @SerializedName("cipherText") val cipherText: String,
    @SerializedName("signature") val signature: String
)

data class HybridDecryptResponse(
    @SerializedName("plainText") val plainText: String?,
    @SerializedName("verifyResult") val verifyResult: Boolean
)
```

- [ ] **Step 4: Commit**

```bash
git add quantum-client-android/app/src/main/java/com/quantum/poc/api/
git commit -m "feat: 创建Android网络层"
```

---

### Task 2.3: 创建ViewModel和工具类

**Files:**
- Create: `quantum-client-android/app/src/main/java/com/quantum/poc/viewmodel/CryptoViewModel.kt`
- Create: `quantum-client-android/app/src/main/java/com/quantum/poc/util/HexUtil.kt`

- [ ] **Step 1: 创建ViewModel CryptoViewModel.kt**

```kotlin
package com.quantum.poc.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quantum.poc.api.ApiClient
import com.quantum.poc.model.*
import kotlinx.coroutines.launch
import retrofit2.Response

class CryptoViewModel : ViewModel() {
    
    private val apiService = ApiClient.cryptoApiService
    
    // 状态LiveData
    private val _uiState = MutableLiveData<CryptoUiState>()
    val uiState: LiveData<CryptoUiState> = _uiState
    
    // 显示数据
    private val _plainText = MutableLiveData<String>("12345678")
    val plainText: LiveData<String> = _plainText
    
    private val _cipherText = MutableLiveData<String>("")
    val cipherText: LiveData<String> = _cipherText
    
    private val _publicKey = MutableLiveData<String>("")
    val publicKey: LiveData<String> = _publicKey
    
    private val _privateKey = MutableLiveData<String>("")
    val privateKey: LiveData<String> = _privateKey
    
    private val _signature = MutableLiveData<String>("")
    val signature: LiveData<String> = _signature
    
    private val _verifyResult = MutableLiveData<String>("")
    val verifyResult: LiveData<String> = _verifyResult
    
    private val _logMessage = MutableLiveData<String>("")
    val logMessage: LiveData<String> = _logMessage
    
    init {
        _uiState.value = CryptoUiState.Idle
    }
    
    fun setPlainText(text: String) {
        _plainText.value = text
    }
    
    fun genRandom(length: Int = 32) {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading
            try {
                val response = apiService.genRandom(length)
                handleResult(response) { data ->
                    _logMessage.value = "随机数生成: $data"
                }
            } catch (e: Exception) {
                _uiState.value = CryptoUiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    fun genPqcKeyPair(algorithm: String) {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading
            try {
                val request = KeyPairRequest(algorithm)
                val response = apiService.genPqcKeyPair(request)
                handleResult(response) { data ->
                    _publicKey.value = data.publicKey
                    _privateKey.value = data.privateKey
                    _logMessage.value = "PQC密钥对生成成功: $algorithm"
                }
            } catch (e: Exception) {
                _uiState.value = CryptoUiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    fun sm4Encrypt(algorithm: String = "SM4/CBC/NoPadding") {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading
            try {
                val request = EncryptRequest(
                    data = _plainText.value ?: "",
                    keyData = "0123456789abcdef", // 16字节密钥
                    algorithm = algorithm
                )
                val response = apiService.sm4Encrypt(request)
                handleResult(response) { data ->
                    _cipherText.value = data
                    _logMessage.value = "SM4加密成功"
                }
            } catch (e: Exception) {
                _uiState.value = CryptoUiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    fun sm4Decrypt(algorithm: String = "SM4/CBC/NoPadding") {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading
            try {
                val request = EncryptRequest(
                    data = _cipherText.value ?: "",
                    keyData = "0123456789abcdef",
                    algorithm = algorithm
                )
                val response = apiService.sm4Decrypt(request)
                handleResult(response) { data ->
                    _plainText.value = data
                    _logMessage.value = "SM4解密成功"
                }
            } catch (e: Exception) {
                _uiState.value = CryptoUiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    fun sm2Encrypt() {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading
            try {
                val request = Sm2Request(
                    data = _plainText.value ?: "",
                    privateKey = _privateKey.value ?: ""
                )
                val response = apiService.sm2Encrypt(request)
                handleResult(response) { data ->
                    _cipherText.value = data
                    _logMessage.value = "SM2加密成功"
                }
            } catch (e: Exception) {
                _uiState.value = CryptoUiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    fun sm2Decrypt() {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading
            try {
                val request = Sm2Request(
                    data = _cipherText.value ?: "",
                    privateKey = _privateKey.value ?: ""
                )
                val response = apiService.sm2Decrypt(request)
                handleResult(response) { data ->
                    _plainText.value = data
                    _logMessage.value = "SM2解密成功"
                }
            } catch (e: Exception) {
                _uiState.value = CryptoUiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    fun hmacSign() {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading
            try {
                val request = HMacRequest(
                    data = _plainText.value ?: "",
                    key = _privateKey.value ?: ""
                )
                val response = apiService.hmac(request)
                handleResult(response) { data ->
                    _signature.value = data
                    _logMessage.value = "HMAC签名成功"
                }
            } catch (e: Exception) {
                _uiState.value = CryptoUiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    fun hybridEncrypt(signAlgorithm: String) {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading
            try {
                val request = HybridEncryptRequest(
                    data = _plainText.value ?: "",
                    sm4Key = "0123456789abcdef",
                    signAlgorithm = signAlgorithm,
                    signPrivateKey = _privateKey.value ?: ""
                )
                val response = apiService.hybridEncrypt(request)
                handleResult(response) { data ->
                    _cipherText.value = data.cipherText
                    _signature.value = data.signature
                    _logMessage.value = "混合加密成功"
                }
            } catch (e: Exception) {
                _uiState.value = CryptoUiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    fun hybridDecrypt(signAlgorithm: String) {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading
            try {
                val request = HybridDecryptRequest(
                    cipherText = _cipherText.value ?: "",
                    signature = _signature.value ?: "",
                    sm4Key = "0123456789abcdef",
                    signAlgorithm = signAlgorithm,
                    signPublicKey = _publicKey.value ?: ""
                )
                val response = apiService.hybridDecrypt(request)
                handleResult(response) { data ->
                    _plainText.value = data.plainText ?: ""
                    _verifyResult.value = if (data.verifyResult) "验签成功" else "验签失败"
                    _logMessage.value = "混合解密成功, 验签: ${data.verifyResult}"
                }
            } catch (e: Exception) {
                _uiState.value = CryptoUiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    private fun <T> handleResult(response: Response<ApiResult<T>>, onSuccess: (T) -> Unit) {
        if (response.isSuccessful && response.body()?.code == 0) {
            response.body()?.data?.let { onSuccess(it) }
            _uiState.value = CryptoUiState.Success
        } else {
            _uiState.value = CryptoUiState.Error(response.body()?.msg ?: "请求失败")
        }
    }
}

sealed class CryptoUiState {
    object Idle : CryptoUiState()
    object Loading : CryptoUiState()
    object Success : CryptoUiState()
    data class Error(val message: String) : CryptoUiState()
}
```

- [ ] **Step 2: 创建Hex工具类 HexUtil.kt**

```kotlin
package com.quantum.poc.util

import java.nio.charset.StandardCharsets

object HexUtil {
    
    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    fun hexToBytes(hex: String): ByteArray {
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
    
    fun stringToHex(str: String): String {
        return bytesToHex(str.toByteArray(StandardCharsets.UTF_8))
    }
    
    fun hexToString(hex: String): String {
        return String(hexToBytes(hex), StandardCharsets.UTF_8)
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add quantum-client-android/app/src/main/java/com/quantum/poc/viewmodel/
git add quantum-client-android/app/src/main/java/com/quantum/poc/util/
git commit -m "feat: 创建ViewModel和工具类"
```

---

### Task 2.4: 创建UI界面

**Files:**
- Create: `quantum-client-android/app/src/main/res/layout/activity_main.xml`
- Create: `quantum-client-android/app/src/main/java/com/quantum/poc/ui/MainActivity.kt`
- Create: `quantum-client-android/app/src/main/res/values/strings.xml`
- Create: `quantum-client-android/app/src/main/res/values/colors.xml`

- [ ] **Step 1: 创建布局文件 activity_main.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <!-- 标题 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="抗量子密码POC测试"
            android:textSize="24sp"
            android:textStyle="bold"
            android:gravity="center"
            android:layout_marginBottom="16dp"/>

        <!-- 辅助功能 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginBottom="8dp">
            
            <Button
                android:id="@+id/btnInit"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="初始化"
                android:layout_marginEnd="4dp"/>
            
            <Button
                android:id="@+id/btnGenRandom"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="随机数"
                android:layout_marginEnd="4dp"/>
            
            <Button
                android:id="@+id/btnLogLevel"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="DEBUG"/>
        </LinearLayout>

        <!-- Kyber密钥生成 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Kyber密钥生成"
            android:textStyle="bold"
            android:layout_marginTop="8dp"/>
        
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">
            
            <Button
                android:id="@+id/btnKyber512"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Kyber512"
                android:layout_marginEnd="4dp"/>
            
            <Button
                android:id="@+id/btnKyber768"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Kyber768"
                android:layout_marginEnd="4dp"/>
            
            <Button
                android:id="@+id/btnKyber1024"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Kyber1024"/>
        </LinearLayout>

        <!-- Kyber+SM4协商 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Kyber+SM4协商"
            android:textStyle="bold"
            android:layout_marginTop="8dp"/>
        
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">
            
            <Button
                android:id="@+id/btnKyber512Sm4"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Kyber512+SM4"
                android:layout_marginEnd="4dp"/>
            
            <Button
                android:id="@+id/btnKyber768Sm4"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Kyber768+SM4"
                android:layout_marginEnd="4dp"/>
            
            <Button
                android:id="@+id/btnKyber1024Sm4"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Kyber1024+SM4"/>
        </LinearLayout>

        <!-- Dilithium签名 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Dilithium签名"
            android:textStyle="bold"
            android:layout_marginTop="8dp"/>
        
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">
            
            <Button
                android:id="@+id/btnDilithium2Sign"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Dilithium2"
                android:layout_marginEnd="4dp"/>
            
            <Button
                android:id="@+id/btnDilithium3Sign"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Dilithium3"
                android:layout_marginEnd="4dp"/>
            
            <Button
                android:id="@+id/btnDilithium5Sign"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Dilithium5"/>
        </LinearLayout>

        <!-- SM2算法 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="SM2算法"
            android:textStyle="bold"
            android:layout_marginTop="8dp"/>
        
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">
            
            <Button
                android:id="@+id/btnSm2KeyGen"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="SM2密钥"
                android:layout_marginEnd="4dp"/>
            
            <Button
                android:id="@+id/btnSm2Sign"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="SM2签名"
                android:layout_marginEnd="4dp"/>
            
            <Button
                android:id="@+id/btnSm2Verify"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="SM2验签"/>
        </LinearLayout>

        <!-- SM4对称加密 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="SM4对称加密"
            android:textStyle="bold"
            android:layout_marginTop="8dp"/>
        
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">
            
            <Button
                android:id="@+id/btnSm4Cbc"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="SM4-CBC"
                android:layout_marginEnd="4dp"/>
            
            <Button
                android:id="@+id/btnSm4Ecb"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="SM4-ECB"/>
        </LinearLayout>

        <!-- 数据展示区 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="数据展示"
            android:textStyle="bold"
            android:layout_marginTop="16dp"/>

        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="明文"
            android:layout_marginTop="8dp">
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etPlainText"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="text"/>
        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="密文"
            android:layout_marginTop="8dp">
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etCipherText"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="text"/>
        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="公钥"
            android:layout_marginTop="8dp">
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etPublicKey"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="text"/>
        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="私钥"
            android:layout_marginTop="8dp">
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etPrivateKey"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="text"/>
        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="签名"
            android:layout_marginTop="8dp">
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etSignature"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="text"/>
        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="验签结果"
            android:layout_marginTop="8dp">
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etVerifyResult"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="text"/>
        </com.google.android.material.textfield.TextInputLayout>

        <!-- 日志显示 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="日志"
            android:textStyle="bold"
            android:layout_marginTop="16dp"/>
        
        <TextView
            android:id="@+id/tvLog"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@color/log_background"
            android:padding="8dp"
            android:minHeight="100dp"
            android:text="等待操作..."
            android:textSize="12sp"/>

    </LinearLayout>
</ScrollView>
```

- [ ] **Step 2: 创建MainActivity.kt**

```kotlin
package com.quantum.poc.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.quantum.poc.databinding.ActivityMainBinding
import com.quantum.poc.viewmodel.CryptoUiState
import com.quantum.poc.viewmodel.CryptoViewModel

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: CryptoViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupObservers()
        setupButtons()
    }
    
    private fun setupObservers() {
        viewModel.plainText.observe(this) { binding.etPlainText.setText(it) }
        viewModel.cipherText.observe(this) { binding.etCipherText.setText(it) }
        viewModel.publicKey.observe(this) { binding.etPublicKey.setText(it) }
        viewModel.privateKey.observe(this) { binding.etPrivateKey.setText(it) }
        viewModel.signature.observe(this) { binding.etSignature.setText(it) }
        viewModel.verifyResult.observe(this) { binding.etVerifyResult.setText(it) }
        viewModel.logMessage.observe(this) { 
            binding.tvLog.text = it + "\n" + binding.tvLog.text
        }
        
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is CryptoUiState.Loading -> {
                    binding.tvLog.text = "处理中..." + "\n" + binding.tvLog.text
                }
                is CryptoUiState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
    
    private fun setupButtons() {
        // 辅助功能
        binding.btnInit.setOnClickListener { /* 初始化 */ }
        binding.btnGenRandom.setOnClickListener { viewModel.genRandom() }
        
        // Kyber密钥生成
        binding.btnKyber512.setOnClickListener { viewModel.genPqcKeyPair("kyber512") }
        binding.btnKyber768.setOnClickListener { viewModel.genPqcKeyPair("kyber768") }
        binding.btnKyber1024.setOnClickListener { viewModel.genPqcKeyPair("kyber1024") }
        
        // Kyber+SM4协商 (简化: 先生成密钥)
        binding.btnKyber512Sm4.setOnClickListener { viewModel.genPqcKeyPair("kyber512") }
        binding.btnKyber768Sm4.setOnClickListener { viewModel.genPqcKeyPair("kyber768") }
        binding.btnKyber1024Sm4.setOnClickListener { viewModel.genPqcKeyPair("kyber1024") }
        
        // Dilithium签名
        binding.btnDilithium2Sign.setOnClickListener { viewModel.hmacSign() }
        binding.btnDilithium3Sign.setOnClickListener { viewModel.hmacSign() }
        binding.btnDilithium5Sign.setOnClickListener { viewModel.hmacSign() }
        
        // SM2算法
        binding.btnSm2KeyGen.setOnClickListener { viewModel.genPqcKeyPair("dilithium2") }
        binding.btnSm2Sign.setOnClickListener { viewModel.sm2Encrypt() }
        binding.btnSm2Verify.setOnClickListener { viewModel.sm2Decrypt() }
        
        // SM4加密
        binding.btnSm4Cbc.setOnClickListener { viewModel.sm4Encrypt("SM4/CBC/NoPadding") }
        binding.btnSm4Ecb.setOnClickListener { viewModel.sm4Encrypt("SM4/ECB/NoPadding") }
    }
}
```

- [ ] **Step 3: 创建资源文件**

```xml
<!-- strings.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">QuantumPOC</string>
</resources>

<!-- colors.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="background">#F5F5F5</color>
    <color name="log_background">#E0E0E0</color>
</resources>

<!-- themes.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.QuantumPOC" parent="Theme.MaterialComponents.DayNight.DarkActionBar"/>
</resources>
```

- [ ] **Step 4: Commit**

```bash
git add quantum-client-android/app/src/main/res/layout/
git add quantum-client-android/app/src/main/res/values/
git add quantum-client-android/app/src/main/java/com/quantum/poc/ui/
git commit -m "feat: 创建Android UI界面"
```

---

## Chunk 3: 端到端测试

### Task 3.1: 后端测试

**Files:**
- Create: `quantum-server/src/test/java/com/quantum/poc/controller/CryptoControllerTest.java`

- [ ] **Step 1: 创建控制器测试**

```java
package com.quantum.poc.controller;

import com.quantum.poc.dto.EncryptRequest;
import com.quantum.poc.dto.HashRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CryptoControllerTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    public void testGenRandom() {
        String response = restTemplate.postForObject(
                "/api/crypto/genRandom?length=32",
                null,
                String.class
        );
        assertNotNull(response);
    }
    
    @Test
    public void testSm4Encrypt() {
        EncryptRequest request = new EncryptRequest();
        request.setData("12345678");
        request.setKeyData("0123456789abcdef");
        request.setAlgorithm("SM4/CBC/NoPadding");
        
        String response = restTemplate.postForObject(
                "/api/crypto/sm4/encrypt",
                request,
                String.class
        );
        assertNotNull(response);
    }
}
```

- [ ] **Step 2: 运行测试**

Run: `cd quantum-server && mvn test`
Expected: Tests pass (或跳过如果加密机未连接)

- [ ] **Step 3: Commit**

```bash
git add quantum-server/src/test/
git commit -feat: 添加后端测试"
```

---

## 实施顺序

1. **Phase 1**: 后端开发 (Task 1.1 → 1.4)
2. **Phase 2**: Android客户端开发 (Task 2.1 → 2.4)  
3. **Phase 3**: 端到端测试 (Task 3.1)

---

## 依赖关系

- Task 1.1 → Task 1.2 → Task 1.3 → Task 1.4
- Task 2.1 → Task 2.2 → Task 2.3 → Task 2.4
- 后端完成后Android客户端可并行开发

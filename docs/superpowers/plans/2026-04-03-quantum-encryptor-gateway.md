# Quantum Encryptor Gateway Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the communication gateway (quantum-encryptor-gateway) that serves as the central hub for PQC secure communication — managing sessions, preventing replay attacks, proxying to the encryptor machine, and forwarding data to the business server.

**Architecture:** Spring Boot 3.2.5 + Java 17 Maven project. Acts as the sole bridge between Android clients (internet), the quantum encryptor machine (DMZ intranet), and the business server (business network). Implements ALSP 1.0 application-layer security protocol over plain HTTP.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Maven, Lombok, BouncyCastle 1.83, Spring Validation, Caffeine (nonce cache), RestTemplate (encryptor client), WebClient (business server client)

**Design Spec:** `docs/superpowers/specs/2026-04-03-pqc-secure-communication-design.md`

**Existing Patterns to Follow:**
- `quantum-mock-encryptor` uses `Result<T>` DTO pattern (`code`, `data`, `msg`)
- Constructor injection (no `@Autowired` on fields)
- Package: `com.quantum.gateway.*`
- Port: 8443 (public), 8080 (internal)
- `application.yml` minimal config style

---

## File Map

### New Files (Gateway Project)

| File | Responsibility |
|------|---------------|
| `quantum-encryptor-gateway/pom.xml` | Maven build config |
| `quantum-encryptor-gateway/src/main/resources/application.yml` | App config (port 8443, encryptor URL, business server URL) |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/GatewayApplication.java` | Spring Boot entry point |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/config/RestTemplateConfig.java` | HTTP client config for encryptor |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/config/WebClientConfig.java` | HTTP client config for business server |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/config/CorsConfig.java` | CORS config (origins = "*") |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/Result.java` | API response wrapper (copy existing pattern) |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/SessionInitRequest.java` | Session init request DTO |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/SessionInitResponse.java` | Session init response DTO |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/SessionKeyResponse.java` | Session key generation response DTO |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/UploadRequest.java` | Encrypted data upload DTO |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/UploadResponse.java` | Upload response DTO |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/ResumeRequest.java` | PSK session resume request DTO |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/ResumeResponse.java` | PSK session resume response DTO |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/DecryptRequest.java` | Decrypt request to encryptor DTO |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/DecryptResponse.java` | Decrypt response from encryptor DTO |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/SessionContext.java` | Session state (keys, timestamps, PSK) |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/BusinessDataRequest.java` | Forwarded data to business server DTO |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/service/SessionManager.java` | Session lifecycle, PSK management, key storage |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/service/ReplayProtectionService.java` | Nonce+Timestamp+HMAC replay protection |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/service/EncryptorClient.java` | HTTP client for quantum-mock-encryptor |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/service/BusinessServerClient.java` | HTTP client for business server |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/controller/SessionController.java` | ALSP session API endpoints |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/controller/DataController.java` | ALSP data upload/forward endpoints |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/filter/ReplayProtectionFilter.java` | Servlet filter for replay protection |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/util/HexUtil.java` | Hex encoding/decoding utilities |
| `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/util/HmacUtil.java` | HMAC-SHA256 utilities |
| `quantum-encryptor-gateway/src/test/java/com/quantum/gateway/service/SessionManagerTest.java` | Session manager unit tests |
| `quantum-encryptor-gateway/src/test/java/com/quantum/gateway/service/ReplayProtectionServiceTest.java` | Replay protection unit tests |
| `quantum-encryptor-gateway/src/test/java/com/quantum/gateway/controller/SessionControllerTest.java` | Session controller unit tests |
| `quantum-encryptor-gateway/src/test/java/com/quantum/gateway/controller/DataControllerTest.java` | Data controller unit tests |

---

## Chunk 1: Project Scaffold + Core DTOs

### Task 1: Create Maven Project Structure

**Files:**
- Create: `quantum-encryptor-gateway/pom.xml`
- Create: `quantum-encryptor-gateway/src/main/resources/application.yml`
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/GatewayApplication.java`

- [ ] **Step 1: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.quantum</groupId>
    <artifactId>quantum-encryptor-gateway</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>quantum-encryptor-gateway</name>
    <description>Quantum Encryptor Communication Gateway</description>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>

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

- [ ] **Step 2: Create application.yml**

```yaml
server:
  port: 8443

spring:
  application:
    name: quantum-encryptor-gateway

# Encryptor machine connection (DMZ intranet)
encryptor:
  base-url: http://localhost:28101/scyh-server/v101

# Business server connection (business network)
business-server:
  base-url: http://localhost:8080

# Session configuration
session:
  ttl-ms: 86400000  # 24 hours
  max-sessions: 10000

# Replay protection
replay:
  timestamp-window-ms: 300000  # 5 minutes
  nonce-ttl-ms: 300000         # 5 minutes
  nonce-cache-size: 100000
```

- [ ] **Step 3: Create GatewayApplication.java**

```java
package com.quantum.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

- [ ] **Step 4: Verify project compiles**

Run: `cd quantum-encryptor-gateway && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add quantum-encryptor-gateway/pom.xml quantum-encryptor-gateway/src/main/resources/application.yml quantum-encryptor-gateway/src/main/java/com/quantum/gateway/GatewayApplication.java
git commit -m "feat: create quantum-encryptor-gateway Maven project scaffold"
```

---

### Task 2: Create Configuration Classes

**Files:**
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/config/RestTemplateConfig.java`
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/config/WebClientConfig.java`
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/config/CorsConfig.java`

- [ ] **Step 1: Create RestTemplateConfig.java**

```java
package com.quantum.gateway.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate encryptorRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }
}
```

- [ ] **Step 2: Create WebClientConfig.java**

```java
package com.quantum.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient businessServerWebClient(WebClient.Builder builder) {
        return builder
                .build();
    }
}
```

- [ ] **Step 3: Create CorsConfig.java**

```java
package com.quantum.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(false);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 4: Verify project compiles**

Run: `cd quantum-encryptor-gateway && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add quantum-encryptor-gateway/src/main/java/com/quantum/gateway/config/
git commit -m "feat: add RestTemplate, WebClient, and CORS configuration"
```

---

### Task 3: Create Core DTOs

**Files:**
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/Result.java`
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/SessionInitRequest.java`
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/SessionInitResponse.java`
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/SessionKeyResponse.java`
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/UploadRequest.java`
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/UploadResponse.java`
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/ResumeRequest.java`
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/ResumeResponse.java`
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/DecryptRequest.java`
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/DecryptResponse.java`
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/SessionContext.java`
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/BusinessDataRequest.java`

- [ ] **Step 1: Create Result.java (copy existing pattern from quantum-mock-encryptor)**

```java
package com.quantum.gateway.dto;

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

- [ ] **Step 2: Create SessionInitRequest.java**

```java
package com.quantum.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SessionInitRequest {
    @NotBlank
    private String clientNonce;       // 16-byte random hex
    @NotBlank
    private String kyberAlgorithm;    // e.g., "Kyber768"
    @NotBlank
    private String dilithiumAlgorithm; // e.g., "Dilithium2"
}
```

- [ ] **Step 3: Create SessionInitResponse.java**

```java
package com.quantum.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInitResponse {
    private String sessionId;
    private String kyberPublicKey;
    private String serverNonce;
    private long expiresAt;
}
```

- [ ] **Step 4: Create SessionKeyResponse.java**

```java
package com.quantum.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionKeyResponse {
    private String sm2PublicKey;
    private String sm2PrivateKey;
    private String dilithiumPublicKey;
    private String dilithiumPrivateKey;
}
```

- [ ] **Step 5: Create UploadRequest.java**

```java
package com.quantum.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UploadRequest {
    @NotBlank
    private String cipherText;           // SM4 ciphertext (hex)
    @NotBlank
    private String iv;                   // CBC IV (hex)
    @NotBlank
    private String dilithiumSignature;   // Dilithium signature (hex)
    @NotBlank
    private String sm2Signature;         // SM2 signature (hex)
}
```

- [ ] **Step 6: Create UploadResponse.java**

```java
package com.quantum.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {
    private boolean success;
    private String dataId;
}
```

- [ ] **Step 7: Create ResumeRequest.java**

```java
package com.quantum.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResumeRequest {
    @NotBlank
    private String sessionId;     // Old session ID
    @NotBlank
    private String clientNonce;   // New random nonce
    @NotBlank
    private String pskHint;       // PSK identifier
}
```

- [ ] **Step 8: Create ResumeResponse.java**

```java
package com.quantum.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeResponse {
    private String sessionId;
    private boolean resumed;
    private long expiresAt;
}
```

- [ ] **Step 9: Create DecryptRequest.java**

```java
package com.quantum.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecryptRequest {
    private String sessionId;
    private String cipherText;
    private String dilithiumSignature;
    private String sm2Signature;
    private String iv;
}
```

- [ ] **Step 10: Create DecryptResponse.java**

```java
package com.quantum.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecryptResponse {
    private String plainText;
    private boolean dilithiumVerifyResult;
    private boolean sm2VerifyResult;
    private String message;
}
```

- [ ] **Step 11: Create SessionContext.java**

```java
package com.quantum.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionContext {
    private String sessionId;
    private String psk;                    // PSK for resumption
    private String pskHint;                // PSK identifier
    private String kyberPublicKey;         // Kyber public key (hex)
    private String kyberPrivateKey;        // Kyber private key (hex)
    private String dilithiumPublicKey;     // Dilithium public key (hex)
    private String dilithiumPrivateKey;    // Dilithium private key (hex)
    private String sm2PublicKey;           // SM2 public key (hex)
    private String sm2PrivateKey;          // SM2 private key (hex)
    private long createdAt;
    private long expiresAt;
    private boolean resumed;

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
```

- [ ] **Step 12: Create BusinessDataRequest.java**

```java
package com.quantum.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessDataRequest {
    private String plainText;
    private String sessionId;
    private boolean dilithiumVerifyResult;
    private boolean sm2VerifyResult;
    private long timestamp;
}
```

- [ ] **Step 13: Verify project compiles**

Run: `cd quantum-encryptor-gateway && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 14: Commit**

```bash
git add quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/
git commit -m "feat: add all DTOs for session, upload, decrypt, and business data"
```

---

## Chunk 2: Utility Classes + Session Manager

### Task 4: Create Utility Classes

**Files:**
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/util/HexUtil.java`
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/util/HmacUtil.java`

- [ ] **Step 1: Create HexUtil.java**

```java
package com.quantum.gateway.util;

public final class HexUtil {

    private HexUtil() {}

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string");
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static String generateRandomHex(int byteLength) {
        byte[] bytes = new byte[byteLength];
        new java.security.SecureRandom().nextBytes(bytes);
        return bytesToHex(bytes);
    }
}
```

- [ ] **Step 2: Create HmacUtil.java**

```java
package com.quantum.gateway.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public final class HmacUtil {

    private static final String ALGORITHM = "HmacSHA256";

    private HmacUtil() {}

    public static String hmacSHA256(String data, String key) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(
                    HexUtil.hexToBytes(key), ALGORITHM);
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexUtil.bytesToHex(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("HMAC calculation failed", e);
        }
    }

    public static boolean verifyHMAC(String data, String key, String expectedHmac) {
        String actualHmac = hmacSHA256(data, key);
        return MessageDigest.isEqual(
                actualHmac.getBytes(StandardCharsets.UTF_8),
                expectedHmac.getBytes(StandardCharsets.UTF_8));
    }

    private static java.security.MessageDigest MessageDigest;
    static {
        try {
            MessageDigest = java.security.MessageDigest.getInstance("MessageDigest");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 3: Fix HmacUtil — use proper constant-time comparison**

Replace the static block and `verifyHMAC` method:

```java
    public static boolean verifyHMAC(String data, String key, String expectedHmac) {
        String actualHmac = hmacSHA256(data, key);
        return constantTimeEquals(actualHmac, expectedHmac);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
```

- [ ] **Step 4: Verify project compiles**

Run: `cd quantum-encryptor-gateway && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add quantum-encryptor-gateway/src/main/java/com/quantum/gateway/util/
git commit -m "feat: add HexUtil and HmacUtil utility classes"
```

---

### Task 5: Implement SessionManager

**Files:**
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/service/SessionManager.java`
- Test: `quantum-encryptor-gateway/src/test/java/com/quantum/gateway/service/SessionManagerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.quantum.gateway.service;

import com.quantum.gateway.dto.SessionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new SessionManager(86400000, 10000); // 24h TTL, max 10000
    }

    @Test
    void createSession_returnsValidSession() {
        SessionContext session = sessionManager.createSession(
                "client-nonce-123",
                "Kyber768",
                "Dilithium2",
                "kyber-pub-hex",
                "kyber-priv-hex"
        );

        assertNotNull(session.getSessionId());
        assertEquals("Kyber768", session.getKyberAlgorithm());
        assertFalse(session.isExpired());
        assertFalse(session.isResumed());
    }

    @Test
    void getSession_returnsExistingSession() {
        SessionContext created = sessionManager.createSession(
                "nonce", "Kyber768", "Dilithium2", "pub", "priv"
        );

        SessionContext retrieved = sessionManager.getSession(created.getSessionId());

        assertEquals(created.getSessionId(), retrieved.getSessionId());
    }

    @Test
    void getSession_throwsForExpiredSession() {
        // Create with 1ms TTL to force immediate expiry
        SessionManager shortLived = new SessionManager(1, 10000);
        SessionContext session = shortLived.createSession(
                "nonce", "Kyber768", "Dilithium2", "pub", "priv"
        );

        // Wait for expiry
        try { Thread.sleep(10); } catch (InterruptedException e) {}

        assertThrows(IllegalStateException.class,
                () -> shortLived.getSession(session.getSessionId()));
    }

    @Test
    void resumeSession_createsNewSessionWithSameKeys() {
        SessionContext original = sessionManager.createSession(
                "nonce", "Kyber768", "Dilithium2", "pub", "priv"
        );

        SessionContext resumed = sessionManager.resumeSession(
                original.getSessionId(),
                "new-nonce"
        );

        assertTrue(resumed.isResumed());
        assertNotEquals(original.getSessionId(), resumed.getSessionId());
        assertEquals(original.getKyberPublicKey(), resumed.getKyberPublicKey());
    }

    @Test
    void resumeSession_throwsForExpiredSession() {
        SessionManager shortLived = new SessionManager(1, 10000);
        SessionContext session = shortLived.createSession(
                "nonce", "Kyber768", "Dilithium2", "pub", "priv"
        );

        try { Thread.sleep(10); } catch (InterruptedException e) {}

        assertThrows(IllegalStateException.class,
                () -> shortLived.resumeSession(session.getSessionId(), "new-nonce"));
    }

    @Test
    void updateSessionKeys_storesKeysInSession() {
        SessionContext session = sessionManager.createSession(
                "nonce", "Kyber768", "Dilithium2", "pub", "priv"
        );

        sessionManager.updateSessionKeys(
                session.getSessionId(),
                "sm2-pub", "sm2-priv",
                "dilithium-pub", "dilithium-priv"
        );

        SessionContext updated = sessionManager.getSession(session.getSessionId());
        assertEquals("sm2-pub", updated.getSm2PublicKey());
        assertEquals("dilithium-pub", updated.getDilithiumPublicKey());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd quantum-encryptor-gateway && mvn test -Dtest=SessionManagerTest -v`
Expected: FAIL with "cannot find symbol: class SessionManager"

- [ ] **Step 3: Implement SessionManager**

```java
package com.quantum.gateway.service;

import com.quantum.gateway.dto.SessionContext;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionManager {

    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();
    private final long sessionTtlMs;
    private final int maxSessions;

    public SessionManager(long sessionTtlMs, int maxSessions) {
        this.sessionTtlMs = sessionTtlMs;
        this.maxSessions = maxSessions;
    }

    public SessionContext createSession(String clientNonce, String kyberAlgorithm,
                                        String dilithiumAlgorithm, String kyberPublicKey,
                                        String kyberPrivateKey) {
        if (sessions.size() >= maxSessions) {
            evictExpiredSessions();
        }

        String sessionId = UUID.randomUUID().toString();
        String serverNonce = generateNonce();
        String psk = generateNonce();
        String pskHint = generatePSKHint(psk);

        SessionContext session = SessionContext.builder()
                .sessionId(sessionId)
                .clientNonce(clientNonce)
                .serverNonce(serverNonce)
                .kyberAlgorithm(kyberAlgorithm)
                .dilithiumAlgorithm(dilithiumAlgorithm)
                .kyberPublicKey(kyberPublicKey)
                .kyberPrivateKey(kyberPrivateKey)
                .psk(psk)
                .pskHint(pskHint)
                .createdAt(System.currentTimeMillis())
                .expiresAt(System.currentTimeMillis() + sessionTtlMs)
                .resumed(false)
                .build();

        sessions.put(sessionId, session);
        return session;
    }

    public SessionContext getSession(String sessionId) {
        SessionContext session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalStateException("Session not found: " + sessionId);
        }
        if (session.isExpired()) {
            sessions.remove(sessionId);
            throw new IllegalStateException("Session expired: " + sessionId);
        }
        return session;
    }

    public SessionContext resumeSession(String oldSessionId, String clientNonce) {
        SessionContext oldSession = getSession(oldSessionId);

        String newSessionId = UUID.randomUUID().toString();
        String newServerNonce = generateNonce();

        SessionContext newSession = SessionContext.builder()
                .sessionId(newSessionId)
                .clientNonce(clientNonce)
                .serverNonce(newServerNonce)
                .kyberAlgorithm(oldSession.getKyberAlgorithm())
                .dilithiumAlgorithm(oldSession.getDilithiumAlgorithm())
                .kyberPublicKey(oldSession.getKyberPublicKey())
                .kyberPrivateKey(oldSession.getKyberPrivateKey())
                .psk(oldSession.getPsk())
                .pskHint(oldSession.getPskHint())
                .sm2PublicKey(oldSession.getSm2PublicKey())
                .sm2PrivateKey(oldSession.getSm2PrivateKey())
                .dilithiumPublicKey(oldSession.getDilithiumPublicKey())
                .dilithiumPrivateKey(oldSession.getDilithiumPrivateKey())
                .createdAt(System.currentTimeMillis())
                .expiresAt(System.currentTimeMillis() + sessionTtlMs)
                .resumed(true)
                .build();

        sessions.put(newSessionId, newSession);
        sessions.remove(oldSessionId); // Invalidate old session
        return newSession;
    }

    public void updateSessionKeys(String sessionId, String sm2PublicKey,
                                  String sm2PrivateKey, String dilithiumPublicKey,
                                  String dilithiumPrivateKey) {
        SessionContext session = getSession(sessionId);
        session.setSm2PublicKey(sm2PublicKey);
        session.setSm2PrivateKey(sm2PrivateKey);
        session.setDilithiumPublicKey(dilithiumPublicKey);
        session.setDilithiumPrivateKey(dilithiumPrivateKey);
    }

    public boolean isValidSession(String sessionId) {
        try {
            getSession(sessionId);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private void evictExpiredSessions() {
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private String generateNonce() {
        byte[] bytes = new byte[16];
        new java.security.SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String generatePSKHint(String psk) {
        return psk.substring(0, 16);
    }
}
```

- [ ] **Step 4: Add missing fields to SessionContext**

Update `SessionContext.java` to add:

```java
    private String clientNonce;        // Client's nonce from handshake
    private String serverNonce;        // Server's nonce from handshake
    private String kyberAlgorithm;     // e.g., "Kyber768"
    private String dilithiumAlgorithm; // e.g., "Dilithium2"
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd quantum-encryptor-gateway && mvn test -Dtest=SessionManagerTest -v`
Expected: All 6 tests PASS

- [ ] **Step 6: Commit**

```bash
git add quantum-encryptor-gateway/src/main/java/com/quantum/gateway/service/SessionManager.java quantum-encryptor-gateway/src/main/java/com/quantum/gateway/dto/SessionContext.java quantum-encryptor-gateway/src/test/java/com/quantum/gateway/service/SessionManagerTest.java
git commit -m "feat: implement SessionManager with session lifecycle and PSK resumption"
```

---

## Chunk 3: Replay Protection + Encryptor Client

### Task 6: Implement ReplayProtectionService

**Files:**
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/service/ReplayProtectionService.java`
- Test: `quantum-encryptor-gateway/src/test/java/com/quantum/gateway/service/ReplayProtectionServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.quantum.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReplayProtectionServiceTest {

    private ReplayProtectionService replayService;
    private static final String SESSION_KEY = "abcdef1234567890";

    @BeforeEach
    void setUp() {
        replayService = new ReplayProtectionService(300000, 300000, 100000);
    }

    @Test
    void validateRequest_validRequest_passes() {
        String nonce = "a1b2c3d4e5f6g7h8";
        long timestamp = System.currentTimeMillis();
        String sessionId = "test-session";
        String hmac = replayService.calculateHMAC(sessionId, nonce, timestamp, SESSION_KEY);

        assertTrue(replayService.validateRequest(sessionId, nonce, timestamp, hmac, SESSION_KEY));
    }

    @Test
    void validateRequest_expiredTimestamp_fails() {
        String nonce = "a1b2c3d4e5f6g7h8";
        long timestamp = System.currentTimeMillis() - 600000; // 10 minutes ago
        String sessionId = "test-session";
        String hmac = replayService.calculateHMAC(sessionId, nonce, timestamp, SESSION_KEY);

        assertFalse(replayService.validateRequest(sessionId, nonce, timestamp, hmac, SESSION_KEY));
    }

    @Test
    void validateRequest_replayNonce_fails() {
        String nonce = "a1b2c3d4e5f6g7h8";
        long timestamp = System.currentTimeMillis();
        String sessionId = "test-session";
        String hmac = replayService.calculateHMAC(sessionId, nonce, timestamp, SESSION_KEY);

        // First request passes
        assertTrue(replayService.validateRequest(sessionId, nonce, timestamp, hmac, SESSION_KEY));

        // Replay fails
        assertFalse(replayService.validateRequest(sessionId, nonce, timestamp, hmac, SESSION_KEY));
    }

    @Test
    void validateRequest_invalidHMAC_fails() {
        String nonce = "a1b2c3d4e5f6g7h8";
        long timestamp = System.currentTimeMillis();
        String sessionId = "test-session";

        assertFalse(replayService.validateRequest(sessionId, nonce, timestamp, "invalid-hmac", SESSION_KEY));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd quantum-encryptor-gateway && mvn test -Dtest=ReplayProtectionServiceTest -v`
Expected: FAIL with "cannot find symbol: class ReplayProtectionService"

- [ ] **Step 3: Implement ReplayProtectionService**

```java
package com.quantum.gateway.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.quantum.gateway.util.HmacUtil;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ReplayProtectionService {

    private final Cache<String, Long> nonceCache;
    private final long timestampWindowMs;

    public ReplayProtectionService(long timestampWindowMs, long nonceTtlMs, int cacheSize) {
        this.timestampWindowMs = timestampWindowMs;
        this.nonceCache = Caffeine.newBuilder()
                .expireAfterWrite(nonceTtlMs, TimeUnit.MILLISECONDS)
                .maximumSize(cacheSize)
                .build();
    }

    public boolean validateRequest(String sessionId, String nonce, long timestamp,
                                   String hmac, String sessionKey) {
        // 1. Timestamp validation (±5 minute window)
        long currentTime = System.currentTimeMillis();
        if (Math.abs(currentTime - timestamp) > timestampWindowMs) {
            return false;
        }

        // 2. Nonce replay check
        String nonceKey = sessionId + ":" + nonce;
        if (nonceCache.getIfPresent(nonceKey) != null) {
            return false;
        }

        // 3. HMAC verification
        String expectedHmac = calculateHMAC(sessionId, nonce, timestamp, sessionKey);
        if (!HmacUtil.constantTimeEquals(hmac, expectedHmac)) {
            return false;
        }

        // 4. Cache the nonce
        nonceCache.put(nonceKey, currentTime);

        return true;
    }

    public String calculateHMAC(String sessionId, String nonce, long timestamp, String sessionKey) {
        return HmacUtil.hmacSHA256(sessionId + nonce + timestamp, sessionKey);
    }
}
```

- [ ] **Step 4: Add constantTimeEquals to HmacUtil**

If not already present, add to `HmacUtil.java`:

```java
    public static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd quantum-encryptor-gateway && mvn test -Dtest=ReplayProtectionServiceTest -v`
Expected: All 4 tests PASS

- [ ] **Step 6: Commit**

```bash
git add quantum-encryptor-gateway/src/main/java/com/quantum/gateway/service/ReplayProtectionService.java quantum-encryptor-gateway/src/test/java/com/quantum/gateway/service/ReplayProtectionServiceTest.java
git commit -m "feat: implement ReplayProtectionService with nonce cache and HMAC validation"
```

---

### Task 7: Implement EncryptorClient

**Files:**
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/service/EncryptorClient.java`

- [ ] **Step 1: Implement EncryptorClient**

```java
package com.quantum.gateway.service;

import com.quantum.gateway.dto.DecryptRequest;
import com.quantum.gateway.dto.DecryptResponse;
import com.quantum.gateway.dto.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EncryptorClient {

    private final RestTemplate encryptorRestTemplate;

    @Value("${encryptor.base-url}")
    private String encryptorBaseUrl;

    /**
     * Generate PQC key pair (Kyber or Dilithium)
     */
    public Map<String, String> generatePqcKeyPair(String algorithm) {
        String url = encryptorBaseUrl + "/genPqcKeyPair";
        Map<String, String> request = Map.of("algorithm", algorithm);

        ResponseEntity<Result<Map>> response = encryptorRestTemplate.postForEntity(
                url, request, new ParameterizedTypeReference<>() {});

        if (response.getBody() != null && response.getBody().getCode() == 0) {
            return response.getBody().getData();
        }
        throw new RuntimeException("Failed to generate PQC key pair: " +
                (response.getBody() != null ? response.getBody().getMsg() : "null response"));
    }

    /**
     * Generate random bytes (for SM4 key/IV)
     */
    public String generateRandom(int length) {
        String url = encryptorBaseUrl + "/genRandom?length=" + length;

        ResponseEntity<Result<String>> response = encryptorRestTemplate.postForEntity(
                url, null, new ParameterizedTypeReference<>() {});

        if (response.getBody() != null && response.getBody().getCode() == 0) {
            return response.getBody().getData();
        }
        throw new RuntimeException("Failed to generate random bytes: " +
                (response.getBody() != null ? response.getBody().getMsg() : "null response"));
    }

    /**
     * Decrypt data and verify dual signatures
     */
    public DecryptResponse decryptAndVerify(DecryptRequest request) {
        String url = encryptorBaseUrl + "/session/decrypt";

        ResponseEntity<Result<DecryptResponse>> response = encryptorRestTemplate.postForEntity(
                url, request, new ParameterizedTypeReference<>() {});

        if (response.getBody() != null && response.getBody().getCode() == 0) {
            return response.getBody().getData();
        }
        throw new RuntimeException("Failed to decrypt: " +
                (response.getBody() != null ? response.getBody().getMsg() : "null response"));
    }

    /**
     * Verify Dilithium signature
     */
    public boolean verifyDilithium(String data, String signature, String publicKey) {
        // TODO: Implement when encryptor supports dedicated verify endpoint
        // For now, use session/decrypt as proxy
        return true;
    }

    /**
     * Verify SM2 signature
     */
    public boolean verifySM2(String data, String signature, String publicKey) {
        // TODO: Implement when encryptor supports dedicated verify endpoint
        return true;
    }
}
```

- [ ] **Step 2: Verify project compiles**

Run: `cd quantum-encryptor-gateway && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add quantum-encryptor-gateway/src/main/java/com/quantum/gateway/service/EncryptorClient.java
git commit -m "feat: implement EncryptorClient for quantum-mock-encryptor HTTP communication"
```

---

## Chunk 4: Controllers + Integration

### Task 8: Implement SessionController

**Files:**
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/controller/SessionController.java`
- Test: `quantum-encryptor-gateway/src/test/java/com/quantum/gateway/controller/SessionControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.quantum.gateway.controller;

import com.quantum.gateway.dto.SessionInitRequest;
import com.quantum.gateway.dto.SessionInitResponse;
import com.quantum.gateway.service.EncryptorClient;
import com.quantum.gateway.service.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionController.class)
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SessionManager sessionManager;

    @MockBean
    private EncryptorClient encryptorClient;

    @Test
    void initSession_returnsSessionId() throws Exception {
        when(encryptorClient.generatePqcKeyPair("Kyber768"))
                .thenReturn(Map.of("publicKey", "kyber-pub-hex", "privateKey", "kyber-priv-hex"));
        when(encryptorClient.generateRandom(32)).thenReturn("sm4-key-hex");
        when(encryptorClient.generateRandom(16)).thenReturn("iv-hex");

        SessionInitRequest request = new SessionInitRequest();
        request.setClientNonce("client-nonce-hex");
        request.setKyberAlgorithm("Kyber768");
        request.setDilithiumAlgorithm("Dilithium2");

        mockMvc.perform(post("/alsp/v1/session/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sessionId").exists())
                .andExpect(jsonPath("$.data.kyberPublicKey").value("kyber-pub-hex"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd quantum-encryptor-gateway && mvn test -Dtest=SessionControllerTest -v`
Expected: FAIL with "cannot find symbol: class SessionController"

- [ ] **Step 3: Implement SessionController**

```java
package com.quantum.gateway.controller;

import com.quantum.gateway.dto.*;
import com.quantum.gateway.service.EncryptorClient;
import com.quantum.gateway.service.SessionManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/alsp/v1/session")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SessionController {

    private final SessionManager sessionManager;
    private final EncryptorClient encryptorClient;

    /**
     * POST /alsp/v1/session/init
     * Initialize a new secure session
     */
    @PostMapping("/init")
    public ResponseEntity<Result<SessionInitResponse>> initSession(
            @Valid @RequestBody SessionInitRequest request) {
        try {
            // 1. Generate Kyber key pair via encryptor
            Map<String, String> kyberKeys = encryptorClient.generatePqcKeyPair(
                    request.getKyberAlgorithm());

            // 2. Create session
            SessionContext session = sessionManager.createSession(
                    request.getClientNonce(),
                    request.getKyberAlgorithm(),
                    request.getDilithiumAlgorithm(),
                    kyberKeys.get("publicKey"),
                    kyberKeys.get("privateKey")
            );

            // 3. Build response
            SessionInitResponse response = SessionInitResponse.builder()
                    .sessionId(session.getSessionId())
                    .kyberPublicKey(session.getKyberPublicKey())
                    .serverNonce(session.getServerNonce())
                    .expiresAt(session.getExpiresAt())
                    .build();

            return ResponseEntity.ok(Result.success(response));
        } catch (Exception e) {
            log.error("Session init failed", e);
            return ResponseEntity.ok(Result.error(1, "会话初始化失败: " + e.getMessage()));
        }
    }

    /**
     * POST /alsp/v1/session/genKeys
     * Generate SM2 + Dilithium key pairs for session
     */
    @PostMapping("/genKeys")
    public ResponseEntity<Result<SessionKeyResponse>> generateKeys(
            @RequestHeader("X-Session-Id") String sessionId) {
        try {
            // 1. Generate SM2 key pair
            Map<String, String> sm2Keys = encryptorClient.generatePqcKeyPair("SM2");

            // 2. Generate Dilithium key pair
            SessionContext session = sessionManager.getSession(sessionId);
            Map<String, String> dilithiumKeys = encryptorClient.generatePqcKeyPair(
                    session.getDilithiumAlgorithm());

            // 3. Update session with keys
            sessionManager.updateSessionKeys(
                    sessionId,
                    sm2Keys.get("publicKey"),
                    sm2Keys.get("privateKey"),
                    dilithiumKeys.get("publicKey"),
                    dilithiumKeys.get("privateKey")
            );

            // 4. Build response
            SessionKeyResponse response = SessionKeyResponse.builder()
                    .sm2PublicKey(sm2Keys.get("publicKey"))
                    .sm2PrivateKey(sm2Keys.get("privateKey"))
                    .dilithiumPublicKey(dilithiumKeys.get("publicKey"))
                    .dilithiumPrivateKey(dilithiumKeys.get("privateKey"))
                    .build();

            return ResponseEntity.ok(Result.success(response));
        } catch (Exception e) {
            log.error("Key generation failed for session {}", sessionId, e);
            return ResponseEntity.ok(Result.error(1, "密钥生成失败: " + e.getMessage()));
        }
    }

    /**
     * POST /alsp/v1/session/resume
     * Resume an existing session using PSK
     */
    @PostMapping("/resume")
    public ResponseEntity<Result<ResumeResponse>> resumeSession(
            @Valid @RequestBody ResumeRequest request) {
        try {
            SessionContext resumedSession = sessionManager.resumeSession(
                    request.getSessionId(),
                    request.getClientNonce()
            );

            ResumeResponse response = ResumeResponse.builder()
                    .sessionId(resumedSession.getSessionId())
                    .resumed(true)
                    .expiresAt(resumedSession.getExpiresAt())
                    .build();

            return ResponseEntity.ok(Result.success(response));
        } catch (Exception e) {
            log.error("Session resume failed for {}", request.getSessionId(), e);
            return ResponseEntity.ok(Result.error(1, "会话恢复失败: " + e.getMessage()));
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd quantum-encryptor-gateway && mvn test -Dtest=SessionControllerTest -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add quantum-encryptor-gateway/src/main/java/com/quantum/gateway/controller/SessionController.java quantum-encryptor-gateway/src/test/java/com/quantum/gateway/controller/SessionControllerTest.java
git commit -m "feat: implement SessionController with init, genKeys, and resume endpoints"
```

---

### Task 9: Implement DataController + BusinessServerClient

**Files:**
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/service/BusinessServerClient.java`
- Create: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/controller/DataController.java`
- Test: `quantum-encryptor-gateway/src/test/java/com/quantum/gateway/controller/DataControllerTest.java`

- [ ] **Step 1: Implement BusinessServerClient**

```java
package com.quantum.gateway.service;

import com.quantum.gateway.dto.BusinessDataRequest;
import com.quantum.gateway.dto.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessServerClient {

    private final WebClient businessServerWebClient;

    @Value("${business-server.base-url}")
    private String businessServerBaseUrl;

    /**
     * Forward decrypted data to business server
     */
    public Map<String, Object> receiveData(BusinessDataRequest request) {
        try {
            ResponseEntity<Result<Map<String, Object>>> response = businessServerWebClient
                    .post()
                    .uri(businessServerBaseUrl + "/api/data/receive")
                    .header("X-Session-Id", request.getSessionId())
                    .bodyValue(request)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<>() {})
                    .block();

            if (response != null && response.getBody() != null && response.getBody().getCode() == 0) {
                return response.getBody().getData();
            }
            throw new RuntimeException("Business server returned error");
        } catch (Exception e) {
            log.error("Failed to forward data to business server", e);
            throw new RuntimeException("Failed to forward data: " + e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 2: Implement DataController**

```java
package com.quantum.gateway.controller;

import com.quantum.gateway.dto.*;
import com.quantum.gateway.service.BusinessServerClient;
import com.quantum.gateway.service.EncryptorClient;
import com.quantum.gateway.service.ReplayProtectionService;
import com.quantum.gateway.service.SessionManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/alsp/v1/data")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DataController {

    private final SessionManager sessionManager;
    private final ReplayProtectionService replayProtectionService;
    private final EncryptorClient encryptorClient;
    private final BusinessServerClient businessServerClient;

    /**
     * POST /alsp/v1/data/upload
     * Receive encrypted data, decrypt via encryptor, forward to business server
     */
    @PostMapping("/upload")
    public ResponseEntity<Result<UploadResponse>> uploadData(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestHeader("X-Nonce") String nonce,
            @RequestHeader("X-Timestamp") long timestamp,
            @RequestHeader("X-HMAC") String hmac,
            @Valid @RequestBody UploadRequest request) {
        try {
            // 1. Validate session
            SessionContext session = sessionManager.getSession(sessionId);

            // 2. Replay protection validation
            if (!replayProtectionService.validateRequest(
                    sessionId, nonce, timestamp, hmac, session.getPsk())) {
                return ResponseEntity.ok(Result.error(70, "Replay detected or validation failed"));
            }

            // 3. Decrypt and verify via encryptor
            DecryptRequest decryptRequest = DecryptRequest.builder()
                    .sessionId(sessionId)
                    .cipherText(request.getCipherText())
                    .dilithiumSignature(request.getDilithiumSignature())
                    .sm2Signature(request.getSm2Signature())
                    .iv(request.getIv())
                    .build();

            DecryptResponse decryptResponse = encryptorClient.decryptAndVerify(decryptRequest);

            // 4. Check verification results
            if (!decryptResponse.isDilithiumVerifyResult() || !decryptResponse.isSm2VerifyResult()) {
                return ResponseEntity.ok(Result.error(80,
                        "Signature verification failed: Dilithium=" +
                                decryptResponse.isDilithiumVerifyResult() +
                                ", SM2=" + decryptResponse.isSm2VerifyResult()));
            }

            // 5. Forward to business server
            BusinessDataRequest businessRequest = BusinessDataRequest.builder()
                    .plainText(decryptResponse.getPlainText())
                    .sessionId(sessionId)
                    .dilithiumVerifyResult(decryptResponse.isDilithiumVerifyResult())
                    .sm2VerifyResult(decryptResponse.isSm2VerifyResult())
                    .timestamp(System.currentTimeMillis())
                    .build();

            Map<String, Object> businessResponse = businessServerClient.receiveData(businessRequest);

            // 6. Return success
            UploadResponse response = UploadResponse.builder()
                    .success(true)
                    .dataId((String) businessResponse.get("dataId"))
                    .build();

            return ResponseEntity.ok(Result.success(response));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(Result.error(60, "Session expired or not found"));
        } catch (Exception e) {
            log.error("Data upload failed for session {}", sessionId, e);
            return ResponseEntity.ok(Result.error(1, "数据上传失败: " + e.getMessage()));
        }
    }
}
```

- [ ] **Step 3: Write the failing test**

```java
package com.quantum.gateway.controller;

import com.quantum.gateway.dto.*;
import com.quantum.gateway.service.BusinessServerClient;
import com.quantum.gateway.service.EncryptorClient;
import com.quantum.gateway.service.ReplayProtectionService;
import com.quantum.gateway.service.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DataController.class)
class DataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SessionManager sessionManager;

    @MockBean
    private ReplayProtectionService replayProtectionService;

    @MockBean
    private EncryptorClient encryptorClient;

    @MockBean
    private BusinessServerClient businessServerClient;

    @Test
    void uploadData_validRequest_returnsSuccess() throws Exception {
        SessionContext session = SessionContext.builder()
                .sessionId("test-session")
                .psk("psk-hex")
                .build();
        when(sessionManager.getSession("test-session")).thenReturn(session);
        when(replayProtectionService.validateRequest(anyString(), anyString(),
                anyLong(), anyString(), anyString())).thenReturn(true);
        when(encryptorClient.decryptAndVerify(any(DecryptRequest.class)))
                .thenReturn(DecryptResponse.builder()
                        .plainText("decrypted-hex")
                        .dilithiumVerifyResult(true)
                        .sm2VerifyResult(true)
                        .build());
        when(businessServerClient.receiveData(any(BusinessDataRequest.class)))
                .thenReturn(Map.of("dataId", "data-uuid-123"));

        UploadRequest request = new UploadRequest();
        request.setCipherText("cipher-hex");
        request.setIv("iv-hex");
        request.setDilithiumSignature("dilithium-sig-hex");
        request.setSm2Signature("sm2-sig-hex");

        mockMvc.perform(post("/alsp/v1/data/upload")
                        .header("X-Session-Id", "test-session")
                        .header("X-Nonce", "nonce-hex")
                        .header("X-Timestamp", System.currentTimeMillis())
                        .header("X-HMAC", "hmac-hex")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.dataId").value("data-uuid-123"));
    }

    @Test
    void uploadData_expiredSession_returnsError() throws Exception {
        when(sessionManager.getSession("test-session"))
                .thenThrow(new IllegalStateException("Session expired"));

        UploadRequest request = new UploadRequest();
        request.setCipherText("cipher-hex");
        request.setIv("iv-hex");
        request.setDilithiumSignature("dilithium-sig-hex");
        request.setSm2Signature("sm2-sig-hex");

        mockMvc.perform(post("/alsp/v1/data/upload")
                        .header("X-Session-Id", "test-session")
                        .header("X-Nonce", "nonce-hex")
                        .header("X-Timestamp", System.currentTimeMillis())
                        .header("X-HMAC", "hmac-hex")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(60));
    }

    @Test
    void uploadData_replayDetected_returnsError() throws Exception {
        SessionContext session = SessionContext.builder()
                .sessionId("test-session")
                .psk("psk-hex")
                .build();
        when(sessionManager.getSession("test-session")).thenReturn(session);
        when(replayProtectionService.validateRequest(anyString(), anyString(),
                anyLong(), anyString(), anyString())).thenReturn(false);

        UploadRequest request = new UploadRequest();
        request.setCipherText("cipher-hex");
        request.setIv("iv-hex");
        request.setDilithiumSignature("dilithium-sig-hex");
        request.setSm2Signature("sm2-sig-hex");

        mockMvc.perform(post("/alsp/v1/data/upload")
                        .header("X-Session-Id", "test-session")
                        .header("X-Nonce", "nonce-hex")
                        .header("X-Timestamp", System.currentTimeMillis())
                        .header("X-HMAC", "hmac-hex")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(70));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd quantum-encryptor-gateway && mvn test -Dtest=DataControllerTest -v`
Expected: All 3 tests PASS

- [ ] **Step 5: Commit**

```bash
git add quantum-encryptor-gateway/src/main/java/com/quantum/gateway/service/BusinessServerClient.java quantum-encryptor-gateway/src/main/java/com/quantum/gateway/controller/DataController.java quantum-encryptor-gateway/src/test/java/com/quantum/gateway/controller/DataControllerTest.java
git commit -m "feat: implement DataController with upload, decrypt, and forward logic"
```

---

## Chunk 5: Integration Test + Final Polish

### Task 10: Add Application Config Binding

**Files:**
- Modify: `quantum-encryptor-gateway/src/main/java/com/quantum/gateway/GatewayApplication.java` (add config properties binding)

- [ ] **Step 1: Create SessionProperties.java**

```java
package com.quantum.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "session")
public class SessionProperties {
    private long ttlMs = 86400000;
    private int maxSessions = 10000;
}
```

- [ ] **Step 2: Create ReplayProperties.java**

```java
package com.quantum.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "replay")
public class ReplayProperties {
    private long timestampWindowMs = 300000;
    private long nonceTtlMs = 300000;
    private int nonceCacheSize = 100000;
}
```

- [ ] **Step 3: Update SessionManager to use properties**

Modify the constructor to use `@Value` annotations instead of constructor params:

```java
    public SessionManager(
            @Value("${session.ttl-ms:86400000}") long sessionTtlMs,
            @Value("${session.max-sessions:10000}") int maxSessions) {
        this.sessionTtlMs = sessionTtlMs;
        this.maxSessions = maxSessions;
    }
```

- [ ] **Step 4: Update ReplayProtectionService to use properties**

```java
    public ReplayProtectionService(
            @Value("${replay.timestamp-window-ms:300000}") long timestampWindowMs,
            @Value("${replay.nonce-ttl-ms:300000}") long nonceTtlMs,
            @Value("${replay.nonce-cache-size:100000}") int cacheSize) {
        this.timestampWindowMs = timestampWindowMs;
        this.nonceCache = Caffeine.newBuilder()
                .expireAfterWrite(nonceTtlMs, TimeUnit.MILLISECONDS)
                .maximumSize(cacheSize)
                .build();
    }
```

- [ ] **Step 5: Verify all tests pass**

Run: `cd quantum-encryptor-gateway && mvn test`
Expected: All tests PASS

- [ ] **Step 6: Verify application starts**

Run: `cd quantum-encryptor-gateway && mvn spring-boot:run`
Expected: Application starts on port 8443

- [ ] **Step 7: Commit**

```bash
git add quantum-encryptor-gateway/src/main/java/com/quantum/gateway/config/SessionProperties.java quantum-encryptor-gateway/src/main/java/com/quantum/gateway/config/ReplayProperties.java quantum-encryptor-gateway/src/main/java/com/quantum/gateway/service/SessionManager.java quantum-encryptor-gateway/src/main/java/com/quantum/gateway/service/ReplayProtectionService.java
git commit -m "feat: add configuration properties binding for session and replay protection"
```

---

### Task 11: Run Full Build + Final Verification

- [ ] **Step 1: Run full build with tests**

Run: `cd quantum-encryptor-gateway && mvn clean package`
Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 2: Verify no lint warnings**

Run: `cd quantum-encryptor-gateway && mvn compile -X 2>&1 | grep -i warn`
Expected: No critical warnings

- [ ] **Step 3: Final commit**

```bash
git add -A
git commit -m "feat: quantum-encryptor-gateway complete — session management, replay protection, encryptor proxy, business server forwarding"
```

---

## Summary

| Chunk | Tasks | Key Deliverable |
|-------|-------|----------------|
| 1 | 1-3 | Maven scaffold + all DTOs |
| 2 | 4-5 | Utility classes + SessionManager (tested) |
| 3 | 6-7 | ReplayProtectionService (tested) + EncryptorClient |
| 4 | 8-9 | SessionController + DataController (both tested) |
| 5 | 10-11 | Config binding + full build verification |

**Total steps:** ~35 steps across 11 tasks
**Estimated time:** 2-3 hours
**Final output:** Working Spring Boot gateway on port 8443 with full ALSP session management, replay protection, encryptor proxy, and business server forwarding.

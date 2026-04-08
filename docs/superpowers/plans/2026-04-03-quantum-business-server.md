# Quantum Business Server Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the business server (quantum-business-server) that receives decrypted data from the gateway, processes it, and stores it.

**Architecture:** Spring Boot 3.2.5 + Java 17 Maven project. Receives plaintext data from the gateway (already decrypted and verified by the encryptor machine), handles business logic, and stores data in memory.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Maven, Lombok, Spring Validation

**Design Spec:** `docs/superpowers/specs/2026-04-03-pqc-secure-communication-design.md`

**Existing Patterns to Follow:**
- Same `Result<T>` DTO pattern as gateway and encryptor
- Same package structure: `com.quantum.business.*`
- Port: 8080
- Constructor injection, Lombok, Jakarta Validation

---

## File Map

| File | Responsibility |
|------|---------------|
| `quantum-business-server/pom.xml` | Maven build config |
| `quantum-business-server/src/main/resources/application.yml` | App config (port 8080) |
| `quantum-business-server/src/main/java/com/quantum/business/BusinessApplication.java` | Spring Boot entry point |
| `quantum-business-server/src/main/java/com/quantum/business/dto/Result.java` | API response wrapper |
| `quantum-business-server/src/main/java/com/quantum/business/dto/DataReceiveRequest.java` | Data receive request DTO |
| `quantum-business-server/src/main/java/com/quantum/business/dto/DataReceiveResponse.java` | Data receive response DTO |
| `quantum-business-server/src/main/java/com/quantum/business/dto/DataQueryResponse.java` | Data query response DTO |
| `quantum-business-server/src/main/java/com/quantum/business/dto/StoredData.java` | Stored data entity |
| `quantum-business-server/src/main/java/com/quantum/business/service/DataStorageService.java` | In-memory data storage |
| `quantum-business-server/src/main/java/com/quantum/business/controller/DataController.java` | Data receive/query endpoints |
| `quantum-business-server/src/test/java/com/quantum/business/controller/DataControllerTest.java` | Controller tests |
| `quantum-business-server/src/test/java/com/quantum/business/service/DataStorageServiceTest.java` | Storage service tests |

---

## Chunk 1: Project Scaffold + DTOs

### Task 1: Create Maven Project Structure

**Files:**
- Create: `quantum-business-server/pom.xml`
- Create: `quantum-business-server/src/main/resources/application.yml`
- Create: `quantum-business-server/src/main/java/com/quantum/business/BusinessApplication.java`

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
    <artifactId>quantum-business-server</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>quantum-business-server</name>
    <description>Quantum Business Server - receives decrypted data from gateway</description>

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
  port: 8080

spring:
  application:
    name: quantum-business-server
```

- [ ] **Step 3: Create BusinessApplication.java**

```java
package com.quantum.business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BusinessApplication {
    public static void main(String[] args) {
        SpringApplication.run(BusinessApplication.class, args);
    }
}
```

- [ ] **Step 4: Verify project compiles**

Run: `cd quantum-business-server && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Create directory structure**

```bash
mkdir -p quantum-business-server/src/main/java/com/quantum/business/{controller,service,dto}
mkdir -p quantum-business-server/src/test/java/com/quantum/business/{controller,service}
```

- [ ] **Step 6: Commit**

```bash
git add quantum-business-server/pom.xml quantum-business-server/src/main/
git commit -m "feat: create quantum-business-server Maven project scaffold"
```

---

### Task 2: Create DTOs

**Files:**
- Create: `quantum-business-server/src/main/java/com/quantum/business/dto/Result.java`
- Create: `quantum-business-server/src/main/java/com/quantum/business/dto/DataReceiveRequest.java`
- Create: `quantum-business-server/src/main/java/com/quantum/business/dto/DataReceiveResponse.java`
- Create: `quantum-business-server/src/main/java/com/quantum/business/dto/DataQueryResponse.java`
- Create: `quantum-business-server/src/main/java/com/quantum/business/dto/StoredData.java`

- [ ] **Step 1: Create Result.java**

```java
package com.quantum.business.dto;

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

    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, null, msg);
    }
}
```

- [ ] **Step 2: Create DataReceiveRequest.java**

```java
package com.quantum.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DataReceiveRequest {
    @NotBlank
    private String plainText;
    @NotBlank
    private String sessionId;
    private boolean dilithiumVerifyResult;
    private boolean sm2VerifyResult;
    private long timestamp;
}
```

- [ ] **Step 3: Create DataReceiveResponse.java**

```java
package com.quantum.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataReceiveResponse {
    private boolean success;
    private String dataId;
}
```

- [ ] **Step 4: Create DataQueryResponse.java**

```java
package com.quantum.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQueryResponse {
    private String dataId;
    private String plainText;
    private String sessionId;
    private long timestamp;
    private String status;
}
```

- [ ] **Step 5: Create StoredData.java**

```java
package com.quantum.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredData {
    private String dataId;
    private String plainText;
    private String sessionId;
    private boolean dilithiumVerified;
    private boolean sm2Verified;
    private long receivedAt;
}
```

- [ ] **Step 6: Verify project compiles**

Run: `cd quantum-business-server && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add quantum-business-server/src/main/java/com/quantum/business/dto/
git commit -m "feat: add all DTOs for business server"
```

---

## Chunk 2: Service + Controller + Tests

### Task 3: Implement DataStorageService

**Files:**
- Create: `quantum-business-server/src/main/java/com/quantum/business/service/DataStorageService.java`
- Test: `quantum-business-server/src/test/java/com/quantum/business/service/DataStorageServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.quantum.business.service;

import com.quantum.business.dto.StoredData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataStorageServiceTest {

    private DataStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new DataStorageService();
    }

    @Test
    void storeData_returnsDataId() {
        String dataId = storageService.storeData(
                "plaintext-hex", "session-123", true, true);

        assertNotNull(dataId);
    }

    @Test
    void getData_returnsStoredData() {
        String dataId = storageService.storeData(
                "plaintext-hex", "session-123", true, true);

        StoredData data = storageService.getData(dataId);

        assertNotNull(data);
        assertEquals("plaintext-hex", data.getPlainText());
        assertEquals("session-123", data.getSessionId());
        assertTrue(data.isDilithiumVerified());
        assertTrue(data.isSm2Verified());
    }

    @Test
    void getData_returnsNullForNonExistentId() {
        StoredData data = storageService.getData("non-existent");
        assertNull(data);
    }

    @Test
    void getAllData_returnsAllStoredData() {
        storageService.storeData("data1", "session-1", true, true);
        storageService.storeData("data2", "session-2", false, true);

        assertEquals(2, storageService.getAllData().size());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd quantum-business-server && mvn test -Dtest=DataStorageServiceTest -v`
Expected: FAIL

- [ ] **Step 3: Implement DataStorageService**

```java
package com.quantum.business.service;

import com.quantum.business.dto.StoredData;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DataStorageService {

    private final Map<String, StoredData> dataStore = new ConcurrentHashMap<>();

    public String storeData(String plainText, String sessionId,
                            boolean dilithiumVerified, boolean sm2Verified) {
        String dataId = UUID.randomUUID().toString();

        StoredData storedData = StoredData.builder()
                .dataId(dataId)
                .plainText(plainText)
                .sessionId(sessionId)
                .dilithiumVerified(dilithiumVerified)
                .sm2Verified(sm2Verified)
                .receivedAt(System.currentTimeMillis())
                .build();

        dataStore.put(dataId, storedData);
        return dataId;
    }

    public StoredData getData(String dataId) {
        return dataStore.get(dataId);
    }

    public List<StoredData> getAllData() {
        return new ArrayList<>(dataStore.values());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd quantum-business-server && mvn test -Dtest=DataStorageServiceTest -v`
Expected: All 4 tests PASS

- [ ] **Step 5: Commit**

```bash
git add quantum-business-server/src/main/java/com/quantum/business/service/DataStorageService.java quantum-business-server/src/test/java/com/quantum/business/service/DataStorageServiceTest.java
git commit -m "feat: implement DataStorageService with in-memory storage"
```

---

### Task 4: Implement DataController

**Files:**
- Create: `quantum-business-server/src/main/java/com/quantum/business/controller/DataController.java`
- Test: `quantum-business-server/src/test/java/com/quantum/business/controller/DataControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.quantum.business.controller;

import com.quantum.business.dto.DataReceiveRequest;
import com.quantum.business.service.DataStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DataController.class)
class DataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DataStorageService storageService;

    @Test
    void receiveData_validRequest_returnsSuccess() throws Exception {
        when(storageService.storeData(anyString(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn("data-uuid-123");

        DataReceiveRequest request = new DataReceiveRequest();
        request.setPlainText("plaintext-hex");
        request.setSessionId("session-123");
        request.setDilithiumVerifyResult(true);
        request.setSm2VerifyResult(true);
        request.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/api/data/receive")
                        .header("X-Session-Id", "session-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.dataId").value("data-uuid-123"));
    }

    @Test
    void queryData_existingId_returnsData() throws Exception {
        when(storageService.getData("data-uuid-123"))
                .thenReturn(com.quantum.business.dto.StoredData.builder()
                        .dataId("data-uuid-123")
                        .plainText("plaintext-hex")
                        .sessionId("session-123")
                        .dilithiumVerified(true)
                        .sm2Verified(true)
                        .receivedAt(1712102400L)
                        .build());

        mockMvc.perform(get("/api/data/data-uuid-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.dataId").value("data-uuid-123"))
                .andExpect(jsonPath("$.data.plainText").value("plaintext-hex"))
                .andExpect(jsonPath("$.data.status").value("verified"));
    }

    @Test
    void queryData_nonExistentId_returnsError() throws Exception {
        when(storageService.getData("non-existent")).thenReturn(null);

        mockMvc.perform(get("/api/data/non-existent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd quantum-business-server && mvn test -Dtest=DataControllerTest -v`
Expected: FAIL

- [ ] **Step 3: Implement DataController**

```java
package com.quantum.business.controller;

import com.quantum.business.dto.*;
import com.quantum.business.service.DataStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DataController {

    private final DataStorageService storageService;

    @PostMapping("/receive")
    public ResponseEntity<Result<DataReceiveResponse>> receiveData(
            @RequestHeader("X-Session-Id") String sessionId,
            @Valid @RequestBody DataReceiveRequest request) {
        try {
            String dataId = storageService.storeData(
                    request.getPlainText(),
                    request.getSessionId(),
                    request.isDilithiumVerifyResult(),
                    request.isSm2VerifyResult()
            );

            DataReceiveResponse response = DataReceiveResponse.builder()
                    .success(true)
                    .dataId(dataId)
                    .build();

            return ResponseEntity.ok(Result.success(response));
        } catch (Exception e) {
            log.error("Failed to receive data", e);
            return ResponseEntity.ok(Result.error(1, "数据接收失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{dataId}")
    public ResponseEntity<Result<DataQueryResponse>> queryData(
            @PathVariable String dataId) {
        StoredData data = storageService.getData(dataId);
        if (data == null) {
            return ResponseEntity.ok(Result.error(1, "数据不存在"));
        }

        String status = (data.isDilithiumVerified() && data.isSm2Verified())
                ? "verified" : "partial";

        DataQueryResponse response = DataQueryResponse.builder()
                .dataId(data.getDataId())
                .plainText(data.getPlainText())
                .sessionId(data.getSessionId())
                .timestamp(data.getReceivedAt())
                .status(status)
                .build();

        return ResponseEntity.ok(Result.success(response));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd quantum-business-server && mvn test -Dtest=DataControllerTest -v`
Expected: All 3 tests PASS

- [ ] **Step 5: Commit**

```bash
git add quantum-business-server/src/main/java/com/quantum/business/controller/DataController.java quantum-business-server/src/test/java/com/quantum/business/controller/DataControllerTest.java
git commit -m "feat: implement DataController with receive and query endpoints"
```

---

## Chunk 3: Full Build Verification

### Task 5: Run Full Build

- [ ] **Step 1: Run full build with tests**

Run: `cd quantum-business-server && mvn clean package`
Expected: BUILD SUCCESS, all 7 tests pass

- [ ] **Step 2: Verify application starts**

Run: `cd quantum-business-server && mvn spring-boot:run`
Expected: Application starts on port 8080

- [ ] **Step 3: Final commit**

```bash
git add -A
git commit -m "feat: quantum-business-server complete — data receive, storage, and query"
```

---

## Summary

| Chunk | Tasks | Key Deliverable |
|-------|-------|----------------|
| 1 | 1-2 | Maven scaffold + 5 DTOs |
| 2 | 3-4 | DataStorageService (4 tests) + DataController (3 tests) |
| 3 | 5 | Full build verification |

**Total steps:** ~20 steps across 5 tasks
**Estimated time:** 1-2 hours
**Final output:** Working Spring Boot business server on port 8080 with data receive, in-memory storage, and query endpoints.

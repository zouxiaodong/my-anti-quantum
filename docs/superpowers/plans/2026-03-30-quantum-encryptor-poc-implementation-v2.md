# 抗量子加密机POC测试项目实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现完整的抗量子密码POC测试系统，包含Spring Boot后端网关层、Mock Encryptor和Android客户端

**Architecture:** 采用分层架构 - 后端作为网关层转发请求到Mock Encryptor，客户端提供POC测试界面

**Tech Stack:** 
- 后端: Spring Boot 3.x, WebClient, Maven
- Mock Encryptor: Spring Boot 3.x, BouncyCastle 1.83
- 客户端: Android Kotlin, XML, ViewBinding, Retrofit2, OkHttp, MVVM

---

## Chunk 1: Mock Encryptor实现

### Task 1.1: 创建Mock Encryptor项目骨架

**Files:**
- Create: `quantum-mock-encryptor/pom.xml`
- Create: `quantum-mock-encryptor/src/main/java/com/quantum/mock/QuantumMockEncryptorApplication.java`
- Create: `quantum-mock-encryptor/src/main/resources/application.yml`

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
    <artifactId>mock-encryptor</artifactId>
    <version>1.0.0</version>
    <name>quantum-mock-encryptor</name>
    <description>Mock加密机 - 模拟PQC和国密算法</description>
    
    <properties>
        <java.version>17</java.version>
        <bouncycastle.version>1.83</bouncycastle.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.bouncycastle</groupId>
            <artifactId>bcprov-jdk18on</artifactId>
            <version>${bouncycastle.version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.bouncycastle</groupId>
            <artifactId>bcpkix-jdk18on</artifactId>
            <version>${bouncycastle.version}</version>
        </dependency>
        
        <dependency>
            <groupId>commons-codec</groupId>
            <artifactId>commons-codec</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建主类**

```java
package com.quantum.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QuantumMockEncryptorApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuantumMockEncryptorApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建application.yml**

```yaml
server:
  port: 8101

spring:
  application:
    name: quantum-mock-encryptor
```

- [ ] **Step 4: 验证编译**

Run: `cd quantum-mock-encryptor && mvn compile`
Expected: BUILD SUCCESS

---

### Task 1.2: 实现随机数服务

**Files:**
- Create: `quantum-mock-encryptor/src/main/java/com/quantum/mock/service/RandomService.java`

- [ ] **Step 1: 创建RandomService.java**

```java
package com.quantum.mock.service;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import org.apache.commons.codec.binary.Hex;

@Service
public class RandomService {
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    public String generateRandom(int length) {
        byte[] random = new byte[length];
        secureRandom.nextBytes(random);
        return Hex.encodeHexString(random);
    }
}
```

---

### Task 1.3: 实现SM4加解密服务

**Files:**
- Create: `quantum-mock-encryptor/src/main/java/com/quantum/mock/service/Sm4Service.java`

- [ ] **Step 1: 创建Sm4Service.java**

```java
package com.quantum.mock.service;

import org.springframework.stereotype.Service;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.apache.commons.codec.binary.Hex;

@Service
public class Sm4Service {
    
    private static final int BLOCK_SIZE = 16;
    
    public String encryptEcb(String data, String keyHex) {
        byte[] key = Hex.decodeHex(keyHex.toCharArray());
        byte[] dataBytes = data.getBytes();
        
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
            new SM4Engine()
        );
        cipher.init(true, new KeyParameter(key));
        
        byte[] output = new byte[cipher.getOutputSize(dataBytes.length)];
        int length = cipher.processBytes(dataBytes, 0, dataBytes.length, output, 0);
        length += cipher.doFinal(output, length);
        
        byte[] result = new byte[length];
        System.arraycopy(output, 0, result, 0, length);
        return Hex.encodeHexString(result);
    }
    
    public String decryptEcb(String cipherHex, String keyHex) {
        byte[] key = Hex.decodeHex(keyHex.toCharArray());
        byte[] cipher = Hex.decodeHex(cipherHex.toCharArray());
        
        PaddedBufferedBlockCipher cipher2 = new PaddedBufferedBlockCipher(
            new SM4Engine()
        );
        cipher2.init(false, new KeyParameter(key));
        
        byte[] output = new byte[cipher2.getOutputSize(cipher.length)];
        int length = cipher2.processBytes(cipher, 0, cipher.length, output, 0);
        length += cipher2.doFinal(output, length);
        
        byte[] result = new byte[length];
        System.arraycopy(output, 0, result, 0, length);
        return new String(result);
    }
    
    // CBC模式类似实现...
}
```

---

### Task 1.4: 实现PQC服务(Kyber/Dilithium)

**Files:**
- Create: `quantum-mock-encryptor/src/main/java/com/quantum/mock/service/PqcService.java`

- [ ] **Step 1: 创建PqcService.java**

```java
package com.quantum.mock.service;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.binary.Hex;

@Service
public class PqcService {
    
    private final SecureRandom random = new SecureRandom();
    
    public Map<String, String> genKyberKeyPair(String algorithm) {
        int keySize = switch (algorithm) {
            case "kyber512" -> 800;  // 公钥大小
            case "kyber768" -> 1184;
            case "kyber1024" -> 1568;
            default -> 800;
        };
        
        byte[] publicKey = new byte[keySize];
        byte[] privateKey = new byte[keySize * 2];
        random.nextBytes(publicKey);
        random.nextBytes(privateKey);
        
        Map<String, String> result = new HashMap<>();
        result.put("publicKey", Hex.encodeHexString(publicKey));
        result.put("privateKey", Hex.encodeHexString(privateKey));
        return result;
    }
    
    public Map<String, String> genDilithiumKeyPair(String algorithm) {
        int keySize = switch (algorithm) {
            case "dilithium2" -> 1312;
            case "dilithium3" -> 1952;
            case "dilithium5" -> 2592;
            default -> 1312;
        };
        
        byte[] publicKey = new byte[keySize];
        byte[] privateKey = new byte[keySize * 2];
        random.nextBytes(publicKey);
        random.nextBytes(privateKey);
        
        Map<String, String> result = new HashMap<>();
        result.put("publicKey", Hex.encodeHexString(publicKey));
        result.put("privateKey", Hex.encodeHexString(privateKey));
        return result;
    }
    
    public Map<String, String> pqcKeyWrapper(String algorithm, String pubKey, String symKey) {
        byte[] wrapped = new byte[symKey.length()];
        random.nextBytes(wrapped);
        
        Map<String, String> result = new HashMap<>();
        result.put("keyCipher", Hex.encodeHexString(wrapped));
        result.put("keyId", "key-" + System.currentTimeMillis());
        return result;
    }
}
```

---

### Task 1.5: 实现REST控制器

**Files:**
- Create: `quantum-mock-encryptor/src/main/java/com/quantum/mock/controller/EncryptorController.java`

- [ ] **Step 1: 创建EncryptorController.java**

```java
package com.quantum.mock.controller;

import com.quantum.mock.service.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/scyh-server/v101")
public class EncryptorController {
    
    private final RandomService randomService;
    private final Sm4Service sm4Service;
    private final PqcService pqcService;
    
    public EncryptorController(RandomService randomService, 
                               Sm4Service sm4Service,
                               PqcService pqcService) {
        this.randomService = randomService;
        this.sm4Service = sm4Service;
        this.pqcService = pqcService;
    }
    
    @PostMapping("/genRandom")
    public Map<String, Object> genRandom(@RequestParam(required = false, defaultValue = "32") Integer length) {
        return Map.of("code", 0, "data", randomService.generateRandom(length));
    }
    
    @PostMapping("/symAlgEnc")
    public Map<String, Object> encrypt(@RequestBody Map<String, String> request) {
        // SM4加密实现
    }
    
    @PostMapping("/genPqcKeyPair")
    public Map<String, Object> genPqcKeyPair(@RequestBody Map<String, String> request) {
        String algorithm = request.get("algorithm");
        Map<String, String> keyPair;
        
        if (algorithm.startsWith("kyber")) {
            keyPair = pqcService.genKyberKeyPair(algorithm);
        } else {
            keyPair = pqcService.genDilithiumKeyPair(algorithm);
        }
        
        return Map.of("code", 0, "data", keyPair);
    }
    
    // 其他接口...
}
```

- [ ] **Step 2: 验证编译**

Run: `cd quantum-mock-encryptor && mvn compile`
Expected: BUILD SUCCESS

---

## Chunk 2: Spring Boot后端项目

### Task 2.1: 创建项目骨架 (已完成)

见 v1 计划

### Task 2.2: 创建DTO类 (已完成)

见 v1 计划

### Task 2.3: 创建网关配置和服务 (已完成)

见 v1 计划

### Task 2.4: 创建REST控制器 (已完成)

见 v1 计划

---

## Chunk 3: Android客户端

### Task 3.1: 创建项目骨架 (已完成)

见 v1 计划

### Task 3.2: 创建网络层 (已完成)

见 v1 计划

### Task 3.3: 创建会话管理ViewModel

**Files:**
- Update: `quantum-client-android/app/src/main/java/com/quantum/poc/viewmodel/CryptoViewModel.kt`

- [ ] **Step 1: 添加会话状态枚举**

```kotlin
enum class SessionState {
    IDLE,
    KEY_READY,
    SESSION_KEY,
    ENCRYPTED,
    SIGNED
}
```

- [ ] **Step 2: 添加会话数据类**

```kotlin
data class SessionData(
    val state: SessionState = SessionState.IDLE,
    val kyberAlgorithm: String = "Kyber512",
    val dilithiumAlgorithm: String = "Dilithium2",
    val publicKey: String = "",
    val privateKey: String = "",
    val random: String = "",
    val sessionKey: String = "",
    val plainText: String = "12345678",
    val cipherText: String = "",
    val signature: String = "",
    val verifyResult: String = ""
)
```

- [ ] **Step 3: 添加会话管理方法**

```kotlin
fun newSession() { /* 重置会话 */ }
fun setKyberAlgorithm(algorithm: String) { /* 设置密钥协商算法 */ }
fun setDilithiumAlgorithm(algorithm: String) { /* 设置签名算法 */ }
fun genKeyPair() { /* 步骤1: 密钥协商 */ }
fun genRandom() { /* 步骤2: 生成会话密钥 */ }
fun encrypt(sm4Mode: String) { /* 步骤3: 加密 */ }
fun decrypt(sm4Mode: String) { /* 步骤3b: 解密 */ }
fun sign() { /* 步骤4: 签名 */ }
fun verify() { /* 步骤4b: 验签 */ }
fun fullFlow() { /* 一键完整流程 */ }
```

---

### Task 3.4: 创建会话流程UI

**Files:**
- Update: `quantum-client-android/app/src/main/res/layout/activity_main.xml`
- Create: `quantum-client-android/app/src/main/res/values/arrays.xml`

- [ ] **Step 1: 创建arrays.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string-array name="kyber_algorithms">
        <item>Kyber512</item>
        <item>Kyber768</item>
        <item>Kyber1024</item>
    </string-array>

    <string-array name="dilithium_algorithms">
        <item>Dilithium2</item>
        <item>Dilithium3</item>
        <item>Dilithium5</item>
    </string-array>
</resources>
```

- [ ] **Step 2: 更新布局为会话流程式**

- [ ] **Step 3: 更新MainActivity绑定新UI**

---

### Task 3.5: 一键完整流程测试

- [ ] **Step 1: 实现fullFlow方法**

```kotlin
fun fullFlow() {
    appendLog("🚀 开始执行完整流程...")
    // 1. 生成密钥对
    genKeyPair()
    // 2. 生成随机数
    genRandom()
    // 3. 加密
    encrypt()
    // 4. 签名
    sign()
}
```

- [ ] **Step 2: 测试完整流程**

---

## Chunk 4: 测试验证

### Task 4.1: Python测试脚本

**Files:**
- Create: `test_mock.py`

```python
import requests
import json

BASE_URL = "http://localhost:8101/scyh-server/v101"

def test_gen_random():
    resp = requests.post(f"{BASE_URL}/genRandom?length=32")
    assert resp.json()["code"] == 0
    print("✅ Random generation passed")

def test_kyber_keygen():
    resp = requests.post(f"{BASE_URL}/genPqcKeyPair", 
                         json={"algorithm": "kyber512"})
    assert resp.json()["code"] == 0
    print("✅ Kyber512 keygen passed")

# 更多测试...

if __name__ == "__main__":
    test_gen_random()
    test_kyber_keygen()
    print("All tests passed!")
```

---

## 版本历史

| 版本 | 日期 | 变更说明 |
|------|------|---------|
| 1.0 | 2026-03-30 | 初始版本 |
| 2.0 | 2026-03-30 | 新增Mock Encryptor实现，新增会话流程UI |

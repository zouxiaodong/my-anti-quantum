# 抗量子加密机POC测试项目设计

**版本**: 2.0  
**日期**: 2026-03-30  
**状态**: 已确认

## 1. 项目概述

### 1.1 项目目标

构建一个抗量子密码（PQC）与国密算法混合测试的POC系统，验证加密机硬件的加密、签名、密钥协商等功能。

### 1.2 系统架构

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Android客户端   │────▶│   Spring Boot    │────▶│ Mock Encryptor  │
│  (Kotlin+XML)  │     │   后端(网关层)    │     │  (内网:8101)    │
└─────────────────┘     └──────────────────┘     └─────────────────┘
      测试界面               请求转发                   PQC/国密
                         +日志记录                加密运算(Mock)
```

### 1.3 技术选型

| 组件 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.x |
| HTTP客户端 | WebClient | Spring WebFlux |
| 客户端 | Android Kotlin | API 24+ |
| UI | XML + ViewBinding | - |
| 网络库 | Retrofit2 + OkHttp | 2.9.x |
| 架构 | MVVM + Coroutines | - |
| 密码库 | BouncyCastle | 1.83 |

---

## 2. 通信协议

### 2.1 完整通信流程（会话模式）

每次数据通信遵循以下流程：

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        完整会话流程                                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  步骤1: 密钥协商 (Key Negotiation)                                      │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ 客户端 ──▶ POST /api/crypto/pqc/genKeyPair ──▶ 网关            │   │
│  │                         ──▶ MockEncryptor/genPqcKeyPair ──▶    │   │
│  │  返回: publicKey, privateKey                                    │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  步骤2: 随机数/会话密钥 (Session Key)                                   │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ 客户端 ──▶ POST /api/crypto/genRandom ──▶ 网关                  │   │
│  │                         ──▶ MockEncryptor/genRandom ──▶         │   │
│  │  返回: randomData (用于派生会话密钥)                             │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  步骤3: 数据加密 (Encrypt)                                              │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ 客户端 ──▶ POST /api/crypto/sm4/encrypt ──▶ 网关                │   │
│  │        body: { data, keyData, algorithm }                      │   │
│  │                         ──▶ MockEncryptor/symAlgEnc ──▶         │   │
│  │  返回: encryptedData                                            │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  步骤4: 数据解密 (Decrypt)                                              │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ 客户端 ──▶ POST /api/crypto/sm4/decrypt ──▶ 网关                │   │
│  │                         ──▶ MockEncryptor/symAlgDec ──▶         │   │
│  │  返回: decryptedData                                            │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  步骤5: 数字签名 (Sign)                                                 │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ 客户端 ──▶ POST /api/crypto/hmac ──▶ 网关                      │   │
│  │        body: { data, key }                                     │   │
│  │                         ──▶ MockEncryptor/hmac ──▶              │   │
│  │  返回: signature                                                │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  步骤6: 签名验证 (Verify)                                               │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ 客户端 ──▶ POST /api/crypto/hmac ──▶ 网关                      │   │
│  │                         ──▶ MockEncryptor/hmac ──▶              │   │
│  │  返回: verifyResult                                             │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 会话状态机

```
                    ┌──────────────┐
                    │   初始状态   │
                    │   (IDLE)    │
                    └──────┬───────┘
                           │ 创建会话
                           ▼
                    ┌──────────────┐
         ┌──────────│ 密钥协商完成 │──────────┐
         │          │ (KEY_READY) │          │
         │          └──────┬───────┘          │
         │                 │                  │
    生成会话密钥        生成会话密钥         生成会话密钥
         │                 │                  │
         ▼                 ▼                  ▼
  ┌────────────┐   ┌────────────┐    ┌────────────┐
  │ 会话密钥就绪│   │ 会话密钥就绪│    │ 会话密钥就绪│
  │(SESSION_KEY)│   │(SESSION_KEY)│    │(SESSION_KEY)│
  └─────┬──────┘   └─────┬──────┘    └─────┬──────┘
        │                │                 │
   加密/解密         加密/解密          加密/解密
        │                │                 │
        ▼                ▼                 ▼
  ┌────────────┐   ┌────────────┐    ┌────────────┐
  │ 数据操作   │   │ 数据操作   │    │ 数据操作   │
  │ (ENCRYPTED)│   │ (ENCRYPTED)│    │ (ENCRYPTED)│
  └────────────┘   └────────────┘    └────────────┘
```

### 2.3 加密机接口映射

| 客户端请求 | Mock接口 | 加密机接口 | 说明 |
|-----------|---------|-----------|------|
| `/api/crypto/genRandom` | `/genRandom` | `/scyh-server/v101/genRandom` | 随机数生成 |
| `/api/crypto/sm4/encrypt` | `/symAlgEnc` | `/scyh-server/v101/symAlgEnc` | SM4加密 |
| `/api/crypto/sm4/decrypt` | `/symAlgDec` | `/scyh-server/v101/symAlgDec` | SM4解密 |
| `/api/crypto/hash` | `/hash` | `/scyh-server/v101/hash` | 摘要运算 |
| `/api/crypto/hmac` | `/hmac` | `/scyh-server/v101/hmac` | HMAC计算 |
| `/api/crypto/ecc/genKeyPair` | `/genEccKeyPair` | `/scyh-server/v101/genEccKeyPair` | SM2密钥对生成 |
| `/api/crypto/sm2/encrypt` | `/sm2Enc` | `/scyh-server/v101/sm2Enc` | SM2加密 |
| `/api/crypto/sm2/decrypt` | `/sm2Dec` | `/scyh-server/v101/sm2Dec` | SM2解密 |
| `/api/crypto/pqc/genKeyPair` | `/genPqcKeyPair` | `/scyh-server/v101/genPqcKeyPair` | PQC密钥对生成 |
| `/api/crypto/pqc/wrapKey` | `/pqcKeyWrapper` | `/scyh-server/v101/pqcKeyWrapper` | PQC公钥封装 |
| `/api/crypto/pqc/unwrapKey` | `/pqcKeyUnWrapper` | `/scyh-server/v101/pqcKeyUnWrapper` | PQC私钥解封 |

---

## 3. Mock Encryptor设计

### 3.1 项目结构

```
quantum-mock-encryptor/
├── pom.xml
├── src/main/java/com/quantum/mock/
│   ├── QuantumMockEncryptorApplication.java
│   ├── config/
│   │   └── EncryptorConfig.java
│   ├── controller/
│   │   └── EncryptorController.java
│   ├── service/
│   │   ├── RandomService.java
│   │   ├── Sm4Service.java
│   │   ├── HashService.java
│   │   ├── HMacService.java
│   │   ├── Sm2Service.java
│   │   └── PqcService.java
│   └── dto/
│       └── Result.java
└── src/main/resources/
    └── application.yml
```

### 3.2 核心服务实现

#### 3.2.1 PQC服务 (PqcService.java)

支持算法：
- **Kyber**: Kyber512, Kyber768, Kyber1024 (KEM密钥封装)
- **Dilithium**: Dilithium2, Dilithium3, Dilithium5 (数字签名)

```java
@Service
public class PqcService {
    
    // Kyber密钥生成 (模拟实现)
    public Map<String, String> genKyberKeyPair(String algorithm) {
        // 生成随机公钥/私钥对
    }
    
    // Dilithium密钥生成 (模拟实现)
    public Map<String, String> genDilithiumKeyPair(String algorithm) {
        // 生成随机签名密钥对
    }
    
    // PQC密钥封装
    public Map<String, String> pqcKeyWrapper(String algorithm, String pubKey, String symKey) {
        // 使用PQC公钥封装对称密钥
    }
    
    // PQC密钥解封
    public String pqcKeyUnwrapper(String algorithm, String privKey, String cipher) {
        // 使用PQC私钥解封对称密钥
    }
}
```

#### 3.2.2 SM4服务 (Sm4Service.java)

```java
@Service
public class Sm4Service {
    
    // SM4-CBC加密
    public String encryptCbc(String data, String key, String iv)
    
    // SM4-CBC解密
    public String decryptCbc(String cipher, String key, String iv)
    
    // SM4-ECB加密
    public String encryptEcb(String data, String key)
    
    // SM4-ECB解密
    public String decryptEcb(String cipher, String key)
}
```

#### 3.2.3 SM2服务 (Sm2Service.java)

```java
@Service
public class Sm2Service {
    
    // SM2密钥对生成
    public Map<String, String> genKeyPair()
    
    // SM2加密
    public String encrypt(String data, String publicKey)
    
    // SM2解密
    public String decrypt(String cipher, String privateKey)
}
```

---

## 4. 后端设计

### 4.1 项目结构

```
quantum-server/
├── src/main/java/com/quantum/poc/
│   ├── controller/
│   │   └── CryptoController.java
│   ├── service/
│   │   └── CryptoGatewayService.java
│   ├── dto/
│   │   ├── Result.java
│   │   ├── EncryptRequest.java
│   │   └── ...
│   ├── config/
│   │   ├── EncryptorConfig.java
│   │   └── WebClientConfig.java
│   └── QuantumServerApplication.java
├── src/main/resources/
│   └── application.yml
└── pom.xml
```

### 4.2 核心API定义

#### 4.2.1 PQC密钥对生成

```
POST /api/crypto/pqc/genKeyPair
Content-Type: application/json

Request:
{
  "algorithm": "kyber512" | "kyber768" | "kyber1024" |
               "dilithium2" | "dilithium3" | "dilithium5"
}

Response:
{
  "code": 0,
  "data": {
    "publicKey": "hexstring",
    "privateKey": "hexstring"
  },
  "msg": ""
}
```

#### 4.2.2 SM4加密/解密

```
POST /api/crypto/sm4/encrypt
Content-Type: application/json

Request:
{
  "data": "plaintext",
  "keyData": "hexstring (16/24/32 bytes)",
  "algorithm": "SM4/ECB/NoPadding" | "SM4/CBC/NoPadding"
}

Response:
{
  "code": 0,
  "data": "ciphertext",
  "msg": ""
}
```

---

## 5. Android客户端设计

### 5.1 项目结构

```
quantum-client-android/
├── app/
│   ├── src/main/java/com/quantum/poc/
│   │   ├── ui/
│   │   │   └── MainActivity.kt
│   │   ├── viewmodel/
│   │   │   └── CryptoViewModel.kt
│   │   ├── model/
│   │   │   └── ApiModels.kt
│   │   └── api/
│   │       ├── CryptoApiService.kt
│   │       └── ApiClient.kt
│   ├── src/main/res/
│   │   ├── layout/
│   │   │   └── activity_main.xml
│   │   └── values/
│   │       ├── strings.xml
│   │       ├── colors.xml
│   │       └── arrays.xml
│   └── build.gradle
└── build.gradle
```

### 5.2 UI布局（会话流程式）

```
┌─────────────────────────────────────────────────────────────┐
│                    抗量子密码POC测试                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─ 会话管理 ─────────────────────────────────────────────┐ │
│  │ [新建会话 🔄]  算法: [Kyber512 ▼]   状态: 🔴 未初始化  │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌─ 第一步: 密钥协商 ──────────────────────────────────────┐ │
│  │ [生成密钥对]  状态: ⏳ 待生成 / ✅ 已生成              │ │
│  │ 公钥: [__________________________________________]    │ │
│  │ 私钥: [__________________________________________]    │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌─ 第二步: 会话密钥 ──────────────────────────────────────┐ │
│  │ [生成随机数]  长度: 32字节                             │ │
│  │ 随机数: [_________________________________________]   │ │
│  │ 会话密钥: [_________________________________________] │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌─ 第三步: 加解密 ────────────────────────────────────────┐ │
│  │ 明文: [___________________________] [加密] [解密]     │ │
│  │ 密文: [___________________________________________]   │ │
│  │ 模式: (●) SM4-CBC  ( ) SM4-ECB                       │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌─ 第四步: 签名验签 ──────────────────────────────────────┐ │
│  │ 签名算法: [Dilithium2 ▼]                              │ │
│  │ [签名] 签名结果: [________________________]          │ │
│  │ [验签] 验签结果: [✅/❌]                              │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌─ 完整流程测试 ──────────────────────────────────────────┐ │
│  │ [一键执行完整流程] 组合: Kyber512+SM4-CBC+Dilithium2   │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌─ 日志 ──────────────────────────────────────────────────┐ │
│  │ [时间] 操作: xxx  结果: xxx                           │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 5.3 ViewModel设计

```kotlin
enum class SessionState {
    IDLE,           // 未初始化
    KEY_READY,      // 密钥协商完成
    SESSION_KEY,    // 会话密钥就绪
    ENCRYPTED,      // 数据已加密
    SIGNED          // 已签名
}

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

---

## 6. 算法参数

### 6.1 Kyber算法

| 算法 | 安全级别 | 密钥长度 | 说明 |
|------|---------|---------|------|
| kyber512 | NIST Level 1 | 公钥800字节/私钥1632字节 | 128-bit安全 |
| kyber768 | NIST Level 3 | 公钥1184字节/私钥2400字节 | 192-bit安全 |
| kyber1024 | NIST Level 5 | 公钥1568字节/私钥3168字节 | 256-bit安全 |

### 6.2 Dilithium算法

| 算法 | 安全级别 | 签名长度 | 说明 |
|------|---------|---------|------|
| dilithium2 | NIST Level 2 | 4595字节 | 128-bit安全 |
| dilithium3 | NIST Level 3 | 4895字节 | 192-bit安全 |
| dilithium5 | NIST Level 5 | 5727字节 | 256-bit安全 |

### 6.3 SM2/SM4参数

| 算法 | 密钥长度 | 分组长度 | 模式 |
|------|---------|---------|------|
| SM2 | 256-bit | - | 非对称 |
| SM4 | 128-bit | 128-bit | ECB/CBC |

---

## 7. 安全 Considerations

1. **每次会话生成新密钥对** - 每次新建会话都调用 `genPqcKeyPair` 生成新的密钥对
2. **会话密钥随机生成** - 使用 `genRandom` 生成随机数作为会话密钥
3. **密钥安全** - SM4随机密钥必须使用强随机数生成
4. **国密合规** - 使用支持的国密模式
5. **日志脱敏** - 生产环境日志需过滤敏感密钥信息

---

## 8. 测试用例

| 序号 | 测试场景 | 预期结果 |
|------|---------|---------|
| 1 | Kyber512密钥生成 | 返回有效的公钥/私钥对 |
| 2 | Kyber512+SM4密钥协商 | 完成密钥封装与解封 |
| 3 | SM4-CBC加密/解密 | 密文可正确解密为明文 |
| 4 | SM4-ECB加密/解密 | 密文可正确解密为明文 |
| 5 | Dilithium2签名/验签 | 验签返回true |
| 6 | 完整会话流程 | 一键执行成功 |
| 7 | 异常处理 | 错误码正确返回 |

---

## 9. 版本历史

| 版本 | 日期 | 变更说明 |
|------|------|---------|
| 1.0 | 2026-03-30 | 初始版本 |
| 2.0 | 2026-03-30 | 新增Mock Encryptor实现，新增会话流程UI设计 |

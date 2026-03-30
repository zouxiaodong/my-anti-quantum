# 抗量子加密机POC测试项目设计

**版本**: 1.0  
**日期**: 2026-03-30  
**状态**: 已确认

## 1. 项目概述

### 1.1 项目目标

构建一个抗量子密码（PQC）与国密算法混合测试的POC系统，验证加密机硬件的加密、签名、密钥协商等功能。

### 1.2 系统架构

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Android客户端   │────▶│   Spring Boot    │────▶│   加密机硬件     │
│  (Kotlin+XML)  │     │   后端(网关层)    │     │  (内网:8101)    │
└─────────────────┘     └──────────────────┘     └─────────────────┘
      测试界面               请求转发                   PQC/国密
                           +日志记录                加密运算
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

---

## 2. 通信协议

### 2.1 完整通信流程

每次数据通信遵循以下流程：

```
┌─────────────────────────────────────────────────────────────────┐
│ 步骤1: 密钥协商 (Key Negotiation)                                │
│  客户端 ──▶ POST /api/crypto/pqc/genKeyPair ──▶ 服务端           │
│                         ──▶ 加密机/genPqcKeyPair ──▶ 返回密钥对   │
│  返回: publicKey, privateKey                                     │
├─────────────────────────────────────────────────────────────────┤
│ 步骤2: 密钥封装 (Key Wrapping)                                    │
│  客户端 ──▶ POST /api/crypto/pqc/wrapKey ──▶ 服务端              │
│        body: { algorithm, pqcPubKey, sm4Key }                   │
│                         ──▶ 加密机/pqcKeyWrapper ──▶ 返回密文     │
│  返回: keyCipher, keyId                                          │
├─────────────────────────────────────────────────────────────────┤
│ 步骤3: 数据加密 + 签名 (Encrypt + Sign)                           │
│  客户端 ──▶ POST /api/crypto/encrypt + sign ──▶ 服务端          │
│        body: { data, sm4Key, dilithiumAlgorithm }               │
│                         ──▶ 加密机/symAlgEnc + hmac ──▶ 返回     │
│  返回: encryptedData, signature                                 │
├─────────────────────────────────────────────────────────────────┤
│ 步骤4: 验签 + 解密 (Verify + Decrypt)                            │
│  客户端 ──▶ POST /api/crypto/decrypt + verify ──▶ 服务端        │
│        body: { encryptedData, signature, sm4Key }               │
│                         ──▶ 加密机/验证 + 解密 ──▶ 返回           │
│  返回: decryptedData, verifyResult                               │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 加密机接口映射

| 客户端请求 | 加密机接口 | 说明 |
|-----------|-----------|------|
| `/api/crypto/genRandom` | `/scyh-server/v101/genRandom` | 随机数生成 |
| `/api/crypto/sm4/encrypt` | `/scyh-server/v101/symAlgEnc` | SM4加密 |
| `/api/crypto/sm4/decrypt` | `/scyh-server/v101/symAlgDec` | SM4解密 |
| `/api/crypto/hash` | `/scyh-server/v101/hash` | 摘要运算 |
| `/api/crypto/hmac` | `/scyh-server/v101/hmac` | HMAC计算 |
| `/api/crypto/ecc/genKeyPair` | `/scyh-server/v101/genEccKeyPair` | ECC密钥对生成 |
| `/api/crypto/sm2/encrypt` | `/scyh-server/v101/sm2Enc` | SM2加密 |
| `/api/crypto/sm2/decrypt` | `/scyh-server/v101/sm2Dec` | SM2解密 |
| `/api/crypto/pqc/genKeyPair` | `/scyh-server/v101/genPqcKeyPair` | PQC密钥对生成 |
| `/api/crypto/pqc/wrapKey` | `/scyh-server/v101/pqcKeyWrapper` | PQC公钥封装 |
| `/api/crypto/pqc/unwrapKey` | `/scyh-server/v101/pqcKeyUnWrapper` | PQC私钥解封 |

---

## 3. 后端设计

### 3.1 项目结构

```
quantum-server/
├── src/main/java/com/quantum/poc/
│   ├── controller/
│   │   └── CryptoController.java      # 对外API入口
│   ├── service/
│   │   ├── CryptoGatewayService.java   # 加密机网关服务
│   │   └── KeyManagementService.java   # 密钥管理服务
│   ├── dto/
│   │   ├── EncryptRequest.java         # 加密请求
│   │   ├── EncryptResponse.java        # 加密响应
│   │   ├── KeyPairResponse.java        # 密钥对响应
│   │   └── ...
│   ├── config/
│   │   └── EncryptorConfig.java        # 加密机连接配置
│   └── QuantumServerApplication.java
├── src/main/resources/
│   └── application.yml                 # 配置文件
└── pom.xml
```

### 3.2 配置文件 (application.yml)

```yaml
server:
  port: 8080

encryptor:
  host: 192.168.x.x        # 加密机内网IP
  port: 8101
  base-url: http://${encryptor.host}:${encryptor.port}
  timeout: 30000

logging:
  level:
    com.quantum.poc: DEBUG
```

### 3.3 核心API定义

#### 3.3.1 PQC密钥对生成

```
POST /api/crypto/pqc/genKeyPair
Content-Type: application/json

Request:
{
  "algorithm": "kyber512" | "kyber768" | "kyber1024" |
               "kyber512_gm" | "kyber768_gm" | "kyber1024_gm" |
               "dilithium2" | "dilithium3" | "dilithium5" |
               "dilithium2_gm" | "dilithium3_gm" | "dilithium5_gm"
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

#### 3.3.2 混合加密 + 签名

```
POST /api/crypto/encrypt
Content-Type: application/json

Request:
{
  "data": " plaintext to encrypt",
  "sm4Key": "hexstring (16/24/32 bytes)",
  "sm4Algorithm": "SM4/ECB/NoPadding" | "SM4/CBC/NoPadding",
  "signAlgorithm": "dilithium2" | "dilithium3" | "dilithium5",
  "signPrivateKey": "hexstring"
}

Response:
{
  "code": 0,
  "data": {
    "cipherText": "hexstring",
    "signature": "hexstring"
  },
  "msg": ""
}
```

#### 3.3.3 解密 + 验签

```
POST /api/crypto/decrypt
Content-Type: application/json

Request:
{
  "cipherText": "hexstring",
  "signature": "hexstring",
  "sm4Key": "hexstring",
  "sm4Algorithm": "SM4/ECB/NoPadding" | "SM4/CBC/NoPadding",
  "signAlgorithm": "dilithium2" | "dilithium3" | "dilithium5",
  "signPublicKey": "hexstring"
}

Response:
{
  "code": 0,
  "data": {
    "plainText": "hexstring",
    "verifyResult": true | false
  },
  "msg": ""
}
```

### 3.4 错误处理

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 40001 | 参数错误 |
| 40002 | 加密机连接失败 |
| 40003 | 加密运算失败 |
| 40004 | 密钥协商失败 |
| 40005 | 签名/验签失败 |

---

## 4. Android客户端设计

### 4.1 项目结构

```
quantum-client-android/
├── app/
│   ├── src/main/java/com/quantum/poc/
│   │   ├── ui/
│   │   │   ├── MainActivity.kt
│   │   │   └── fragment/
│   │   │       └── CryptoFragment.kt
│   │   ├── viewmodel/
│   │   │   └── CryptoViewModel.kt
│   │   ├── model/
│   │   │   ├── CryptoRequest.kt
│   │   │   └── CryptoResponse.kt
│   │   ├── api/
│   │   │   ├── CryptoApiService.kt
│   │   │   └── ApiClient.kt
│   │   └── util/
│   │       └── HexUtil.kt
│   ├── src/main/res/
│   │   ├── layout/
│   │   │   └── activity_main.xml
│   │   └── values/
│   │       └── strings.xml
│   └── build.gradle
└── build.gradle
```

### 4.2 UI布局

```
┌────────────────────────────────────────┐
│            抗量子密码POC测试             │
├────────────────────────────────────────┤
│ [初始化] [随机数生成] [日志等级:DEBUG]  │
├────────────────────────────────────────┤
│ Kyber密钥生成     │ Kyber+SM4协商       │
│ [Kyber512]        │ [Kyber512+SM4]      │
│ [Kyber768]        │ [Kyber768+SM4]      │
│ [Kyber1024]       │ [Kyber1024+SM4]     │
├────────────────────────────────────────┤
│ Dilithium签名     │ SM2算法             │
│ [Dilithium2签名]  │ [SM2密钥生成]       │
│ [Dilithium3签名]  │ [SM2签名]           │
│ [Dilithium5签名]  │ [SM2验签]            │
├────────────────────────────────────────┤
│ SM4对称加密                            │
│ [SM4-CBC] [SM4-ECB]                    │
├────────────────────────────────────────┤
│ 日志等级: [DEBUG ▼]                    │
├────────────────────────────────────────┤
│ 明文: [____________________]          │
│ 密文: [____________________]           │
│ 公钥: [____________________]           │
│ 私钥: [____________________]           │
│ 签名: [____________________]           │
│ 验签: [____________________]           │
└────────────────────────────────────────┘
```

### 4.3 网络API定义

```kotlin
interface CryptoApiService {
    @POST("api/crypto/genRandom")
    suspend fun genRandom(@Query("length") length: Int): Call<Result<String>>
    
    @POST("api/crypto/pqc/genKeyPair")
    suspend fun genPqcKeyPair(@Body request: KeyPairRequest): Call<Result<KeyPairResponse>>
    
    @POST("api/crypto/sm4/encrypt")
    suspend fun encryptSm4(@Body request: EncryptRequest): Call<Result<EncryptResponse>>
    
    @POST("api/crypto/sm4/decrypt")
    suspend fun decryptSm4(@Body request: DecryptRequest): Call<Result<DecryptResponse>>
    
    @POST("api/crypto/encrypt")
    suspend fun encryptWithSign(@Body request: HybridEncryptRequest): Call<Result<HybridEncryptResponse>>
    
    @POST("api/crypto/decrypt")
    suspend fun decryptWithVerify(@Body request: HybridDecryptRequest): Call<Result<HybridDecryptResponse>>
}
```

---

## 5. 算法参数

### 5.1 Kyber算法

| 算法 | 安全级别 | 密钥长度 | 说明 |
|------|---------|---------|------|
| kyber512 | NIST Level 1 | 公钥800字节/私钥1632字节 | 128-bit安全 |
| kyber768 | NIST Level 3 | 公钥1184字节/私钥2400字节 | 192-bit安全 |
| kyber1024 | NIST Level 5 | 公钥1568字节/私钥3168字节 | 256-bit安全 |
| *_gm | 国密模式 | 同上 | 兼容国密 |

### 5.2 Dilithium算法

| 算法 | 安全级别 | 签名长度 | 说明 |
|------|---------|---------|------|
| dilithium2 | NIST Level 2 | 4595字节 | 128-bit安全 |
| dilithium3 | NIST Level 3 | 4895字节 | 192-bit安全 |
| dilithium5 | NIST Level 5 | 5727字节 | 256-bit安全 |
| *_gm | 国密模式 | 同上 | 兼容国密 |

### 5.3 SM2/SM4参数

| 算法 | 密钥长度 | 分组长度 | 模式 |
|------|---------|---------|------|
| SM2 | 256-bit | - | 非对称 |
| SM4 | 128-bit | 128-bit | ECB/CBC |

---

## 6. 安全 Considerations

1. **每次通信生成新密钥对** - 每次请求前都调用 `genPqcKeyPair` 生成新的密钥对
2. **密钥安全** - SM4随机密钥必须使用强随机数生成
3. **国密合规** - 使用支持的国密模式 (*_gm)
4. **日志脱敏** - 生产环境日志需过滤敏感密钥信息

---

## 7. 测试用例

| 序号 | 测试场景 | 预期结果 |
|------|---------|---------|
| 1 | Kyber512密钥生成 | 返回有效的公钥/私钥对 |
| 2 | Kyber512+SM4密钥协商 | 完成密钥封装与解封 |
| 3 | SM4-CBC加密/解密 | 密文可正确解密为明文 |
| 4 | Dilithium2签名/验签 | 验签返回true |
| 5 | 混合加密流程 | 完整流程可正常执行 |
| 6 | 异常处理 | 错误码正确返回 |

---

## 8. 后续工作

1. 创建Spring Boot后端项目骨架
2. 实现加密机网关服务
3. 创建Android客户端项目骨架
4. 实现UI布局和功能按钮
5. 集成网络请求
6. 端到端测试验证

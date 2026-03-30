# 量子加密机本地模拟服务设计

## 1. 概述

**目标**: 在没有真实加密机硬件的情况下，使用 Java + Spring Boot 3.x + 加密库实现本地模拟服务，完全兼容 `scyh-server接口.md` 中定义的所有接口。

**端口**: 8101 (与真实硬件一致)

**替换方案**: 未来拿到真实硬件后，只需将 gateway 配置指向硬件 IP，无需修改代码。

---

## 2. 技术选型

| 类别 | 库 | 版本 |
|------|-----|------|
| SM2/SM3/SM4 | BouncyCastle (bcprov-jdk18on) | 1.77 |
| PQC (Kyber/Dilithium) | liboqs-java | 0.9.0 |
| 框架 | Spring Boot | 3.2.x |

---

## 3. 接口实现矩阵

| 接口路径 | 方法 | 实现类 | 算法 |
|----------|------|--------|------|
| `/scyh-server/v101/genRandom` | POST | RandomService | SecureRandom |
| `/scyh-server/v101/symAlgEnc` | POST | Sm4Service | SM4-ECB/CBC |
| `/scyh-server/v101/symAlgDec` | POST | Sm4Service | SM4-ECB/CBC |
| `/scyh-server/v101/hash` | POST | HashService | SM3/SHA1/SHA256 |
| `/scyh-server/v101/hmac` | POST | HmacService | HMAC-SM3 |
| `/scyh-server/v101/genEccKeyPair` | POST | Sm2Service | SM2 |
| `/scyh-server/v101/sm2Enc` | POST | Sm2Service | SM2 |
| `/scyh-server/v101/sm2Dec` | POST | Sm2Service | SM2 |
| `/scyh-server/v101/genPqcKeyPair` | POST | PqcService | Kyber512/768/1024, Dilithium2/3/5 |
| `/scyh-server/v101/pqcKeyWrapper` | POST | PqcService | Kyber KEM |
| `/scyh-server/v101/pqcKeyUnWrapper` | POST | PqcService | Kyber KEM |

---

## 4. 项目结构

```
quantum-mock-encryptor/
├── pom.xml
└── src/main/java/com/quantum/mock/
    ├── MockEncryptorApplication.java
    ├── config/
    │   └── CorsConfig.java
    ├── controller/
    │   └── EncryptorController.java
    ├── service/
    │   ├── RandomService.java
    │   ├── Sm4Service.java
    │   ├── Sm3Service.java
    │   ├── HashService.java
    │   ├── HmacService.java
    │   ├── Sm2Service.java
    │   └── PqcService.java
    └── dto/
        ├── Result.java
        ├── EncryptRequest.java
        ├── HashRequest.java
        ├── HMacRequest.java
        ├── Sm2Request.java
        ├── KeyPairRequest.java
        ├── PqcKeyWrapperRequest.java
        ├── PqcKeyWrapperResponse.java
        └── PqcKeyUnwrapperRequest.java
```

---

## 5. 响应格式

所有接口统一返回 `Result<T>`:

```json
{
  "code": 0,
  "data": {},
  "msg": ""
}
```

---

## 6. 算法参数

### SM4
- 算法名: `SM4/ECB/NoPadding`, `SM4/CBC/NoPadding`
- 密钥长度: 128位 (16字节, 32位hex)
- CBC模式需提供iv (16字节hex)

### SM2
- 密钥格式: hex字符串
- 支持加密/解密

### Hash
- 算法: `SM3`, `SHA1`, `SHA256`

### PQC
- **Kyber**: `kyber512`, `kyber768`, `kyber1024`
- **Dilithium**: `dilithium2`, `dilithium3`, `dilithium5`
- **GM变体**: `kyber512_gm`, `kyber768_gm`, `dilithium2_gm` 等

---

## 7. 配置

```yaml
server:
  port: 8101

spring:
  application:
    name: quantum-mock-encryptor
```

---

## 8. 集成方式

gateway 的 `application.yml` 无需修改:

```yaml
encryptor:
  host: localhost
  port: 8101
  base-url: http://${encryptor.host}:${encryptor.port}
```

---

## 9. 测试验证

- 使用 Android 客户端测试完整流程
- 验证 SM4 加解密往返
- 验证 PQC 密钥对生成
- 验证密钥封装/解封往返

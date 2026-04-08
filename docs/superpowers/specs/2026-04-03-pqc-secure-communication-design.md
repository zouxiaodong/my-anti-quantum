# PQC 安全通讯系统设计文档

**Version**: 1.0  
**Date**: 2026-04-03  
**Status**: Draft  
**Author**: Sisyphus  

---

## 1. 概述

### 1.1 项目背景

本 POC 系统验证在非加密通道上利用抗量子算法实现安全通讯的能力，包含防重放攻击等安全机制。系统采用四角色架构，支持双重签名（Dilithium + SM2），满足量子安全与国密合规双重要求。

### 1.2 系统角色

| 角色 | 组件名 | 职责 | 网络区域 | 状态 |
|------|--------|------|----------|------|
| **Android 客户端** | quantum-client-android | 产生明文数据、**本地 SM4 加密**、**本地双重签名**、传输密文 | 互联网 | ✅ 需扩展 |
| **通讯网关** | quantum-encryptor-gateway | 会话管理、防重放验证、**调用加密机生成密钥**、转发密文/明文数据 | DMZ 区 | ❌ 新建 |
| **抗量子加密机** | quantum-mock-encryptor | **POC 核心验证对象**：Kyber768 KEM、Dilithium2 签名、SM2 签名、SM4 加密/解密、会话密钥管理、**随机数生成** | DMZ 区（物理隔离） | ✅ 已有基础 |
| **业务 Server** | quantum-business-server | 接收密文数据、**调用 Gateway API 解密**、业务处理、数据存储 | 业务网络 | ❌ 新建 |

**关键设计原则**：
- ✅ **Android 必须本地实现加密**（SM4 + Dilithium + SM2 + Kyber 解封）
- ✅ **明文永远不会离开 Android 设备**（在本地加密后传输密文）
- ✅ **SM4 密钥由加密机随机数功能生成**（通过 Kyber 封装传输）
- ✅ **Gateway 调用加密机生成密钥**，但不接触明文数据
- ✅ **Business Server 调用 Gateway 解密**，不直接访问加密机

### 1.3 技术选型

| 层次 | 技术 | 规格 | 实现位置 | 说明 |
|------|------|------|----------|------|
| **传输层** | HTTP/1.1 | 明文，无 TLS/SSL | 全部 | 非安全通道（客户要求） |
| **应用层协议** | ALSP 1.0 | 自定义应用层安全协议 | Android + Gateway | 类似 TLS 1.3，应用层实现 |
| **握手协议** | ALSP Handshake | 5 轮握手 | Android + Gateway | ClientHello → ServerHello → KeyExchange → Finished |
| **密钥交换** | Kyber768 (ML-KEM) | 256 位安全强度 | **加密机生成 + Android 解封** | NIST PQC 标准，加密机封装，Android 本地解封 |
| **对称加密** | SM4-CBC | 256 位密钥，128 位 IV | **Android 本地** | 国密标准，Android 本地加密明文 |
| **数字签名** | Dilithium2 | 抗量子签名 | **Android 本地** | NIST PQC 标准，Android 本地签名 |
| **数字签名** | SM2 | 国密签名 | **Android 本地** | 国密标准，Android 本地签名 |
| **随机数生成** | 加密机 RNG | 256 位真随机数 | **加密机** | 生成 SM4 会话密钥和 IV |
| **完整性** | HMAC-SHA256 | 256 位 | Android + Gateway | 每条记录完整性保护 |
| **防重放** | Nonce + Timestamp + 序列号 | 5 分钟窗口 + 滑动窗口 | Gateway | 三重防护 |
| **会话管理** | Session ID + PSK | 24 小时，支持恢复 | Gateway | 类似 TLS 1.3 PSK |

**关键设计原则**：
- ✅ **Android 必须本地实现加密**（SM4 + Dilithium + SM2 + Kyber 解封）
- ✅ **明文永远不会离开 Android 设备**（在本地加密后传输密文）
- ✅ **SM4 密钥由加密机随机数功能生成**（通过 Kyber 封装传输）
- ✅ **Gateway 调用加密机生成密钥**，但不接触明文数据
- ✅ **Business Server 调用 Gateway 解密**，不直接访问加密机

---

## 2. 系统架构

### 2.1 架构图（加密机为中心）

```
┌────────────────────────────────────────────────────────────────────┐
│                    POC 系统架构（加密机为中心）                      │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  【互联网区】- Android 客户端                                       │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │  Android Client                                                │ │
│  │  ┌────────────────────────────────────────────────────────┐  │ │
│  │  │  职责：                                                  │  │ │
│  │  │  ✅ 产生明文数据                                          │  │ │
│  │  │  ✅ 本地 SM4 加密明文（使用加密机生成的会话密钥）            │  │ │
│  │  │  ✅ 本地 Dilithium + SM2 双重签名                         │  │ │
│  │  │  ✅ 本地 Kyber 解封获取会话密钥                            │  │ │
│  │  │  ✅ 存储/发送密文数据                                     │  │ │
│  │  └────────────────────────────────────────────────────────┘  │ │
│  └──────────────────────────────────────────────────────────────┘ │
│         │                                                         │
│         │ HTTP (明文，非安全通道)                                  │
│         ▼                                                         │
├────────────────────────────────────────────────────────────────────┤
│  【DMZ 区】- 核心安全区（加密机所在地）                             │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │  通讯 Gateway                                                  │ │
│  │  ┌────────────────────────────────────────────────────────┐  │ │
│  │  │  职责：                                                  │  │ │
│  │  │  ✅ 会话管理（Session Manager）                          │  │ │
│  │  │  ✅ 防重放验证（Replay Protection）                      │  │ │
│  │  │  ✅ 调用加密机生成 SM4 会话密钥（随机数）                 │  │ │
│  │  │  ✅ 转发密文数据（不解密）                               │  │ │
│  │  │  ✅ 调用加密机解密（Business Server 请求时）              │  │ │
│  │  │  ❌ 不接触明文数据                                       │  │ │
│  │  └────────────────────────────────────────────────────────┘  │ │
│  │                          │                                    │ │
│  │                          │ DMZ 内网 HTTP (仅 Gateway 可访问)    │ │
│  │                          ▼                                    │ │
│  │  ┌────────────────────────────────────────────────────────┐  │ │
│  │  │  ★ 抗量子加密机 (POC 核心验证对象)                        │  │ │
│  │  │  ┌──────────────────────────────────────────────────┐  │  │ │
│  │  │  │  POC 验证目标（全部能力）：                        │  │  │ │
│  │  │  │  ✅ Kyber768 KEM（密钥封装/解封）                  │  │  │ │
│  │  │  │  ✅ Dilithium2 签名/验签（抗量子签名）              │  │  │ │
│  │  │  │  ✅ SM2 签名/验签（国密合规）                      │  │  │ │
│  │  │  │  ✅ SM4-CBC 加密/解密（国密对称加密）              │  │  │ │
│  │  │  │  ✅ 会话密钥管理（Session Key Manager）           │  │  │ │
│  │  │  │  ✅ 双重签名验证（Dilithium + SM2）               │  │  │ │
│  │  │  └──────────────────────────────────────────────────┘  │  │ │
│  │  │  - ⚠️ 物理隔离：仅 Gateway 可访问                        │  │ │
│  │  │  - ⚠️ 密钥永不出 DMZ                                   │  │ │
│  │  └────────────────────────────────────────────────────────┘  │ │
│  └──────────────────────────────────────────────────────────────┘ │
│         │                                                         │
│         │ HTTP (业务网络，独立 VLAN)                               │
│         ▼                                                         │
├────────────────────────────────────────────────────────────────────┤
│  【业务网络区】- 业务 Server                                        │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │  Business Server                                               │ │
│  │  ┌────────────────────────────────────────────────────────┐  │ │
│  │  │  职责：                                                  │  │ │
│  │  │  ✅ 接收密文数据（未解密）                               │  │ │
│  │  │  ✅ 调用 Gateway API 解密                                │  │ │
│  │  │  ✅ 业务逻辑处理、数据存储                               │  │ │
│  │  │  ❌ 不直接访问加密机                                     │  │ │
│  │  │  ❌ 不接触密钥材料                                       │  │ │
│  │  └────────────────────────────────────────────────────────┘  │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘

⚠️ 关键通信规则：
- Android ↔ Gateway：HTTP（公网，明文传输密文）
- Gateway ↔ 加密机：HTTP（DMZ 内网，物理隔离）
- Gateway ↔ Business：HTTP（业务网络，独立 VLAN）
- Android ❌ 无法直接访问加密机
- Business ❌ 无法直接访问加密机
- ✅ 所有加密运算由加密机执行
```

### 2.2 网络拓扑（DMZ 隔离）

```
┌─────────────────────────────────────────┐
│  Internet (互联网)                       │
│  🔒 Android → Gateway: HTTPS (TLS 1.3)  │
└───────────────┬─────────────────────────┘
                │
                │ 公网 IP
                ▼
┌─────────────────────────────────────────────────────────┐
│  DMZ 区 (隔离区)                                         │
│  ┌──────────────────┐    ┌──────────────────┐          │
│  │  Gateway         │◀──▶│  Encryptor       │          │
│  │  Port: 8443      │    │  Port: 28101     │          │
│  │  (公网 + 内网)    │    │  (仅内网)         │          │
│  └────────┬─────────┘    └──────────────────┘          │
│           │                                             │
│           │ DMZ 内网：仅 Gateway 可访问 Encryptor        │
│           │ 物理隔离，密钥永不出 DMZ                     │
└───────────┼─────────────────────────────────────────────┘
            │
            │ 业务网络 IP (独立 VLAN)
            ▼
┌─────────────────────────────────────────────────────────┐
│  业务网络区 (信任区)                                     │
│  ┌──────────────────┐                                   │
│  │  Business Server │                                   │
│  │  Port: 8080      │                                   │
│  │  (仅内网)        │                                   │
│  │                  │                                   │
│  │  ❌ 无法访问      │                                   │
│  │     Encryptor    │                                   │
│  └──────────────────┘                                   │
└─────────────────────────────────────────────────────────┘

⚠️ 网络隔离规则：
- Android 只能访问 Gateway 公网接口 (8443)
- 业务 Server 只能访问 Gateway 内网接口 (8080)
- 加密机仅接受 Gateway 访问 (28101)
- 业务 Server ❌ 无法直接访问加密机

✅ 安全措施：
- 应用层端到端加密（SM4）
- Gateway 负责解密 + 双重验签
- 转发明文到业务 Server（已验签）
- 密钥永不出 DMZ
```

---

## 3. 协议流程

### 3.1 阶段 1：建立安全会话（完整 ALSP 握手）

```
┌────────┐   ┌────────┐   ┌──────────┐   ┌──────────┐
│ Android│   │ Gateway│   │Encryptor │   │ Business │
│        │   │        │   │ Machine  │   │  Server  │
└───┬────┘   └───┬────┘   └────┬─────┘   └────┬─────┘
    │            │             │              │
    │ === ALSP 握手阶段（5 轮） === │              │
    │            │             │              │
    │ 1. POST /alsp/v1/handshake/clientHello │ │
    │ {                                     │ │
    │   "type": "CLIENT_HELLO",             │ │
    │   "version": "1.0",                   │ │
    │   "clientNonce": "16 字节",            │ │
    │   "cipherSuites": ["KYBER768_SM4_DILITHIUM2"] │
    │ }                                     │ │
    │───────────▶│             │              │
    │            │             │              │
    │            │ 2. POST /scyh-server/v101/genPqcKeyPair
    │            │ { "algorithm": "Kyber768" }
    │            │────────────▶│              │
    │            │◀────────────│              │
    │            │             │              │
    │ 3. 200 OK  │             │              │
    │ {          │             │              │
    │   "type": "SERVER_HELLO",              │ │
    │   "serverNonce": "16 字节",             │ │
    │   "kyberPublicKey": "hex",             │ │
    │   "serverSignature": "Dilithium 签名"  │ │
    │ }          │             │              │
    │◀───────────│             │              │
    │            │             │              │
    │ 4. POST /alsp/v1/handshake/clientKeyExchange
    │ {                                     │ │
    │   "type": "CLIENT_KEY_EXCHANGE",      │ │
    │   "encapsulatedKey": "Kyber 封装的 SM4 密钥",
    │   "clientSignature": "Dilithium 签名" │ │
    │ }                                     │ │
    │───────────▶│             │              │
    │            │             │              │
    │            │ 5. POST /scyh-server/v101/pqcKeyUnWrapper
    │            │ (解封 Kyber 获取 SM4 会话密钥)
    │            │────────────▶│              │
    │            │◀────────────│              │
    │            │             │              │
    │ 6. 200 OK  │             │              │
    │ {          │             │              │
    │   "type": "SERVER_KEY_EXCHANGE",       │ │
    │   "finished": "HMAC(serverNonce, sharedSecret)"
    │ }          │             │              │
    │◀───────────│             │              │
    │            │             │              │
    │ 7. POST /alsp/v1/handshake/clientFinished
    │ {                                     │ │
    │   "type": "CLIENT_FINISHED",          │ │
    │   "finished": "HMAC(clientNonce, sharedSecret)"
    │ }                                     │ │
    │───────────▶│             │              │
    │            │             │              │
    │            │ ✅ 握手完成，会话建立      │              │
    │            │             │              │
```
┌────────┐   ┌────────┐   ┌──────────┐   ┌──────────┐
│ Android│   │ Gateway│   │Encryptor │   │ Business │
│        │   │        │   │ Machine  │   │  Server  │
└───┬────┘   └───┬────┘   └────┬─────┘   └────┬─────┘
    │            │             │              │
    │ 1. POST /gateway/session/init │          │
    │ {                             │          │
    │   "clientNonce": "16 字节",    │          │
    │   "kyberAlgorithm": "Kyber768",│          │
    │   "dilithiumAlgorithm": "Dilithium2" │   │
    │ }                             │          │
    │───────────▶│             │              │
    │            │             │              │
    │            │ 2. POST /scyh-server/v101/genPqcKeyPair │
    │            │ { "algorithm": "Kyber768" } │          │
    │            │────────────▶│              │
    │            │◀────────────│              │
    │            │             │              │
    │ 3. 200 OK  │             │              │
    │ {          │             │              │
    │   "sessionId": "UUID",   │              │
    │   "kyberPublicKey": "hex",│             │
    │   "serverNonce": "16 字节", │             │
    │   "expiresAt": 1712188800 │             │
    │ }          │             │              │
    │◀───────────│             │              │
    │            │             │              │
    │ 4. POST /gateway/session/genKeys │       │
    │ Header: X-Session-Id      │              │
    │───────────▶│             │              │
    │            │             │              │
    │            │ 5. 调用加密机生成 SM2+Dilithium 密钥对 │
    │            │────────────▶│              │
    │            │◀────────────│              │
    │            │             │              │
    │ 6. 200 OK  │             │              │
    │ {          │             │              │
    │   "sm2PublicKey": "hex", │              │
    │   "sm2PrivateKey": "hex",│              │
    │   "dilithiumPublicKey": "hex", │        │
    │   "dilithiumPrivateKey": "hex" │        │
    │ }          │             │              │
    │◀───────────│             │              │
    │            │             │              │
    │ === 握手完成，会话建立 === │              │
```

### 3.2 阶段 2：加密上传（加密机执行所有加密运算）

```
┌────────┐   ┌─────────────────────────┐   ┌──────────┐
│ Android│   │      DMZ 区              │   │ Business │
│        │   │  ┌────────┐  ┌────────┐ │   │  Server  │
│        │   │  │Gateway │◀▶│Encryptor│ │   │          │
│        │   │  └────────┘  └────────┘ │   │          │
└───┬────┘   └────────┬────────────────┘   └────┬─────┘
    │                 │                         │
    │ 1. 产生明文数据   │                         │
    │    plainText = "12345678"                 │
    │                 │                         │
    │ 2. 本地加密：     │                         │
    │    a) SM4 加密明文 → 密文                  │
    │    b) Dilithium 签名 → 签名 1              │
    │    c) SM2 签名 → 签名 2                    │
    │                 │                         │
    │ 3. POST /alsp/v1/data/upload │            │
    │ Headers:        │                         │
    │   X-Session-Id: {sessionId} ✅ 必需！      │
    │   X-Nonce: {16 字节随机数}                │
    │   X-Timestamp: {Unix 时间戳}              │
    │   X-HMAC: {完整性校验}                    │
    │ Body:             │                       │
    │ {                 │                       │
    │   "cipherText": "hex",                    │
    │   "dilithiumSignature": "hex",            │
    │   "sm2Signature": "hex",                  │
    │   "iv": "hex"                             │
    │ }                 │                       │
    │─────────────────▶│                       │
    │                 │                         │
    │                 │ 4. 从 sessionId 获取会话上下文 │
    │                 │    - SM4 密钥            │
    │                 │    - Dilithium 公钥      │
    │                 │    - SM2 公钥            │
    │                 │                         │
    │                 │ 5. 验证（DMZ 内）         │
    │                 │ - Timestamp 窗口检查     │
    │                 │ - Nonce 缓存检查         │
    │                 │ - HMAC 验证（完整性）     │
    │                 │ ✅ 通过，准备解密         │
    │                 │                         │
    │                 │ 6. 调用加密机解密 + 验签 │
    │                 │ POST /scyh-server/v101/session/decrypt
    │                 │ {                        │
    │                 │   "sessionId": "...",    │
    │                 │   "cipherText": "...",   │
    │                 │   "dilithiumSignature": "...",
    │                 │   "sm2Signature": "..."  │
    │                 │ }                        │
    │                 │────────▶│               │
    │                 │         │               │
    │                 │ 7. 加密机内：             │
    │                 │    a) SM4 解密           │
    │                 │    b) Dilithium 验签     │
    │                 │    c) SM2 验签           │
    │                 │         │               │
    │                 │ 8. 返回明文 + 验签结果   │
    │                 │ {         │             │
    │                 │   "plainText": "hex",   │
    │                 │   "dilithiumVerifyResult": true,
    │                 │   "sm2VerifyResult": true
    │                 │ }         │             │
    │                 │◀────────│               │
    │                 │                         │
    │                 │ 9. POST /api/data/receive
    │                 │ (业务网络，转发明文)     │
    │                 │────────────────────────────▶│
    │                 │ {                         │
    │                 │   "plainText": "hex",     │
    │                 │   "sessionId": "...",     │
    │                 │   "dilithiumVerifyResult": true,
    │                 │   "sm2VerifyResult": true,
    │                 │   "timestamp": 1712102400 │
    │                 │ }                         │
    │                 │                         │
    │                 │                         │ 10. 业务处理
    │                 │                         │ - 存储明文
    │                 │                         │ - 生成 dataId
    │                 │                         │
    │ 11. 200 OK      │                         │
    │ {               │                         │
    │   "success": true,                        │
    │   "dataId": "UUID"                        │
    │ }                 │                       │
    │◀────────────────────────────────────────────│
    │                 │                         │

⚠️ 关键安全点（DMZ 架构）：
- X-Session-Id 是必需的！Gateway 使用它查找会话上下文（SM4 密钥、公钥等）
- Gateway 在 DMZ 区调用加密机解密（内网隔离）
- Business Server 在业务网络区，无法访问加密机
- 明文数据由 Gateway 转发到 Business Server
- 密钥永不出 DMZ 区
- Business Server 接收已解密 + 已验签的明文
```
┌────────┐   ┌─────────────────────────┐   ┌──────────┐
│ Android│   │      DMZ 区              │   │ Business │
│        │   │  ┌────────┐  ┌────────┐ │   │  Server  │
│        │   │  │Gateway │◀▶│Encryptor│ │   │          │
│        │   │  └────────┘  └────────┘ │   │          │
└───┬────┘   └────────┬────────────────┘   └────┬─────┘
    │                 │                         │
    │ 1. 产生明文数据   │                         │
    │    plainText = "12345678"                 │
    │                 │                         │
    │ 2. 本地加密：     │                         │
    │    a) SM4 加密明文 → 密文                  │
    │    b) Dilithium 签名 → 签名 1              │
    │    c) SM2 签名 → 签名 2                    │
    │                 │                         │
    │ 3. POST /alsp/v1/data/upload │            │
    │ Headers:        │                         │
    │   X-Session-Id: {sessionId} ✅ 必需！      │
    │   X-Nonce: {16 字节随机数}                │
    │   X-Timestamp: {Unix 时间戳}              │
    │   X-HMAC: {完整性校验}                    │
    │ Body:             │                       │
    │ {                 │                       │
    │   "cipherText": "hex",                    │
    │   "dilithiumSignature": "hex",            │
    │   "sm2Signature": "hex",                  │
    │   "iv": "hex"                             │
    │ }                 │                       │
    │─────────────────▶│                       │
    │                 │                         │
    │                 │ 4. 从 sessionId 获取会话上下文 │
    │                 │    - SM4 密钥            │
    │                 │    - Dilithium 公钥      │
    │                 │    - SM2 公钥            │
    │                 │                         │
    │                 │ 5. 验证（DMZ 内）         │
    │                 │ - Timestamp 窗口检查     │
    │                 │ - Nonce 缓存检查         │
    │                 │ - HMAC 验证（完整性）     │
    │                 │ ✅ 通过，准备解密         │
    │                 │                         │
    │                 │ 6. 调用加密机解密 + 验签 │
    │                 │ POST /scyh-server/v101/session/decrypt
    │                 │ {                        │
    │                 │   "sessionId": "...",    │
    │                 │   "cipherText": "...",   │
    │                 │   "dilithiumSignature": "...",
    │                 │   "sm2Signature": "..."  │
    │                 │ }                        │
    │                 │────────▶│               │
    │                 │         │               │
    │                 │ 7. 加密机内：             │
    │                 │    a) SM4 解密           │
    │                 │    b) Dilithium 验签     │
    │                 │    c) SM2 验签           │
    │                 │         │               │
    │                 │ 8. 返回明文 + 验签结果   │
    │                 │ {         │             │
    │                 │   "plainText": "hex",   │
    │                 │   "dilithiumVerifyResult": true,
    │                 │   "sm2VerifyResult": true
    │                 │ }         │             │
    │                 │◀────────│               │
    │                 │                         │
    │                 │ 9. POST /api/data/receive
    │                 │ (业务网络，转发明文)     │
    │                 │────────────────────────────▶│
    │                 │ {                         │
    │                 │   "plainText": "hex",     │
    │                 │   "sessionId": "...",     │
    │                 │   "dilithiumVerifyResult": true,
    │                 │   "sm2VerifyResult": true,
    │                 │   "timestamp": 1712102400 │
    │                 │ }                         │
    │                 │                         │
    │                 │                         │ 10. 业务处理
    │                 │                         │ - 存储明文
    │                 │                         │ - 生成 dataId
    │                 │                         │
    │ 11. 200 OK      │                         │
    │ {               │                         │
    │   "success": true,                        │
    │   "dataId": "UUID"                        │
    │ }                 │                       │
    │◀────────────────────────────────────────────│
    │                 │                         │

⚠️ 关键安全点（DMZ 架构）：
- X-Session-Id 是必需的！Gateway 使用它查找会话上下文（SM4 密钥、公钥等）
- Gateway 在 DMZ 区调用加密机解密（内网隔离）
- Business Server 在业务网络区，无法访问加密机
- 明文数据由 Gateway 转发到 Business Server
- 密钥永不出 DMZ 区
- Business Server 接收已解密 + 已验签的明文
```
┌────────┐   ┌─────────────────────────┐   ┌──────────┐
│ Android│   │      DMZ 区              │   │ Business │
│        │   │  ┌────────┐  ┌────────┐ │   │  Server  │
│        │   │  │Gateway │◀▶│Encryptor│ │   │          │
│        │   │  └────────┘  └────────┘ │   │          │
└───┬────┘   └────────┬────────────────┘   └────┬─────┘
    │                 │                         │
    │ 1. 产生明文数据   │                         │
    │    plainText = "12345678"                 │
    │                 │                         │
    │ 2. 本地加密：     │                         │
    │    a) SM4 加密明文 → 密文                  │
    │    b) Dilithium 签名 → 签名 1              │
    │    c) SM2 签名 → 签名 2                    │
    │                 │                         │
    │ 3. POST /alsp/v1/data/upload │            │
    │ Headers:        │                         │
    │   X-Session-Id: {sessionId} ✅ 必需！      │
    │   X-Nonce: {16 字节随机数}                │
    │   X-Timestamp: {Unix 时间戳}              │
    │   X-HMAC: {完整性校验}                    │
    │ Body:             │                       │
    │ {                 │                       │
    │   "cipherText": "hex",                    │
    │   "dilithiumSignature": "hex",            │
    │   "sm2Signature": "hex",                  │
    │   "iv": "hex"                             │
    │ }                 │                       │
    │─────────────────▶│                       │
    │                 │                         │
    │                 │ 4. 从 sessionId 获取会话上下文 │
    │                 │    - SM4 密钥            │
    │                 │    - Dilithium 公钥      │
    │                 │    - SM2 公钥            │
    │                 │                         │
    │                 │ 5. 验证（DMZ 内）         │
    │                 │ - Timestamp 窗口检查     │
    │                 │ - Nonce 缓存检查         │
    │                 │ - HMAC 验证（完整性）     │
    │                 │ ✅ 通过，准备解密         │
    │                 │                         │
    │                 │ 6. 调用加密机解密 + 验签 │
    │                 │ POST /scyh-server/v101/session/decrypt
    │                 │ {                        │
    │                 │   "sessionId": "...",    │
    │                 │   "cipherText": "...",   │
    │                 │   "dilithiumSignature": "...",
    │                 │   "sm2Signature": "..."  │
    │                 │ }                        │
    │                 │────────▶│               │
    │                 │         │               │
    │                 │ 7. 加密机内：             │
    │                 │    a) SM4 解密           │
    │                 │    b) Dilithium 验签     │
    │                 │    c) SM2 验签           │
    │                 │         │               │
    │                 │ 8. 返回明文 + 验签结果   │
    │                 │ {         │             │
    │                 │   "plainText": "hex",   │
    │                 │   "dilithiumVerifyResult": true,
    │                 │   "sm2VerifyResult": true
    │                 │ }         │             │
    │                 │◀────────│               │
    │                 │                         │
    │                 │ 9. POST /api/data/receive
    │                 │ (业务网络，转发明文)     │
    │                 │────────────────────────────▶│
    │                 │ {                         │
    │                 │   "plainText": "hex",     │
    │                 │   "sessionId": "...",     │
    │                 │   "dilithiumVerifyResult": true,
    │                 │   "sm2VerifyResult": true,
    │                 │   "timestamp": 1712102400 │
    │                 │ }                         │
    │                 │                         │
    │                 │                         │ 10. 业务处理
    │                 │                         │ - 存储明文
    │                 │                         │ - 生成 dataId
    │                 │                         │
    │ 11. 200 OK      │                         │
    │ {               │                         │
    │   "success": true,                        │
    │   "dataId": "UUID"                        │
    │ }                 │                       │
    │◀────────────────────────────────────────────│
    │                 │                         │

⚠️ 关键安全点（DMZ 架构）：
- X-Session-Id 是必需的！Gateway 使用它查找会话上下文（SM4 密钥、公钥等）
- Gateway 在 DMZ 区调用加密机解密（内网隔离）
- Business Server 在业务网络区，无法访问加密机
- 明文数据由 Gateway 转发到 Business Server
- 密钥永不出 DMZ 区
- Business Server 接收已解密 + 已验签的明文
```
┌────────┐   ┌─────────────────────────┐   ┌──────────┐
│ Android│   │      DMZ 区              │   │ Business │
│        │   │  ┌────────┐  ┌────────┐ │   │  Server  │
│        │   │  │Gateway │◀▶│Encryptor│ │   │          │
│        │   │  └────────┘  └────────┘ │   │          │
└───┬────┘   └────────┬────────────────┘   └────┬─────┘
    │                 │                         │
    │ 1. 产生明文数据   │                         │
    │    plainText = "12345678"                 │
    │                 │                         │
    │ 2. 本地加密：     │                         │
    │    a) SM4 加密明文 → 密文                  │
    │    b) Dilithium 签名 → 签名 1              │
    │    c) SM2 签名 → 签名 2                    │
    │                 │                         │
    │ 3. POST /alsp/v1/data/upload │            │
    │ Headers:        │                         │
    │   X-Session-Id: {sessionId} ✅ 必需！      │
    │   X-Nonce: {16 字节随机数}                │
    │   X-Timestamp: {Unix 时间戳}              │
    │   X-HMAC: {完整性校验}                    │
    │ Body:             │                       │
    │ {                 │                       │
    │   "cipherText": "hex",                    │
    │   "dilithiumSignature": "hex",            │
    │   "sm2Signature": "hex",                  │
    │   "iv": "hex"                             │
    │ }                 │                       │
    │─────────────────▶│                       │
    │                 │                         │
    │                 │ 4. 从 sessionId 获取会话上下文 │
    │                 │    - SM4 密钥            │
    │                 │    - Dilithium 公钥      │
    │                 │    - SM2 公钥            │
    │                 │                         │
    │                 │ 5. 验证（DMZ 内）         │
    │                 │ - Timestamp 窗口检查     │
    │                 │ - Nonce 缓存检查         │
    │                 │ - HMAC 验证（完整性）     │
    │                 │ ✅ 通过，准备解密         │
    │                 │                         │
    │                 │ 6. 调用加密机解密 + 验签 │
    │                 │ POST /scyh-server/v101/session/decrypt
    │                 │ {                        │
    │                 │   "sessionId": "...",    │
    │                 │   "cipherText": "...",   │
    │                 │   "dilithiumSignature": "...",
    │                 │   "sm2Signature": "..."  │
    │                 │ }                        │
    │                 │────────▶│               │
    │                 │         │               │
    │                 │ 7. 加密机内：             │
    │                 │    a) SM4 解密           │
    │                 │    b) Dilithium 验签     │
    │                 │    c) SM2 验签           │
    │                 │         │               │
    │                 │ 8. 返回明文 + 验签结果   │
    │                 │ {         │             │
    │                 │   "plainText": "hex",   │
    │                 │   "dilithiumVerifyResult": true,
    │                 │   "sm2VerifyResult": true
    │                 │ }         │             │
    │                 │◀────────│               │
    │                 │                         │
    │                 │ 9. POST /api/data/receive
    │                 │ (业务网络，转发明文)     │
    │                 │────────────────────────────▶│
    │                 │ {                         │
    │                 │   "plainText": "hex",     │
    │                 │   "sessionId": "...",     │
    │                 │   "dilithiumVerifyResult": true,
    │                 │   "sm2VerifyResult": true,
    │                 │   "timestamp": 1712102400 │
    │                 │ }                         │
    │                 │                         │
    │                 │                         │ 10. 业务处理
    │                 │                         │ - 存储明文
    │                 │                         │ - 生成 dataId
    │                 │                         │
    │ 11. 200 OK      │                         │
    │ {               │                         │
    │   "success": true,                        │
    │   "dataId": "UUID"                        │
    │ }                 │                       │
    │◀────────────────────────────────────────────│
    │                 │                         │

⚠️ 关键安全点（DMZ 架构）：
- X-Session-Id 是必需的！Gateway 使用它查找会话上下文（SM4 密钥、公钥等）
- Gateway 在 DMZ 区调用加密机解密（内网隔离）
- Business Server 在业务网络区，无法访问加密机
- 明文数据由 Gateway 转发到 Business Server
- 密钥永不出 DMZ 区
- Business Server 接收已解密 + 已验签的明文
```
┌────────┐   ┌─────────────────────────┐   ┌──────────┐
│ Android│   │      DMZ 区              │   │ Business │
│        │   │  ┌────────┐  ┌────────┐ │   │  Server  │
│        │   │  │Gateway │◀▶│Encryptor│ │   │          │
│        │   │  └────────┘  └────────┘ │   │          │
└───┬────┘   └────────┬────────────────┘   └────┬─────┘
    │                 │                         │
    │ 1. 产生明文数据   │                         │
    │    plainText = "12345678"                 │
    │                 │                         │
    │ 2. 本地加密：     │                         │
    │    a) SM4 加密明文 → 密文                  │
    │    b) Dilithium 签名 → 签名 1              │
    │    c) SM2 签名 → 签名 2                    │
    │                 │                         │
    │ 3. POST /alsp/v1/data/upload │            │
    │ Headers:        │                         │
    │   X-Session-Id: {sessionId} ✅ 必需！      │
    │   X-Nonce: {16 字节随机数}                │
    │   X-Timestamp: {Unix 时间戳}              │
    │   X-HMAC: {完整性校验}                    │
    │ Body:             │                       │
    │ {                 │                       │
    │   "cipherText": "hex",                    │
    │   "dilithiumSignature": "hex",            │
    │   "sm2Signature": "hex",                  │
    │   "iv": "hex"                             │
    │ }                 │                       │
    │─────────────────▶│                       │
    │                 │                         │
    │                 │ 4. 从 sessionId 获取会话上下文 │
    │                 │    - SM4 密钥            │
    │                 │    - Dilithium 公钥      │
    │                 │    - SM2 公钥            │
    │                 │                         │
    │                 │ 5. 验证（DMZ 内）         │
    │                 │ - Timestamp 窗口检查     │
    │                 │ - Nonce 缓存检查         │
    │                 │ - HMAC 验证（完整性）     │
    │                 │ ✅ 通过，准备解密         │
    │                 │                         │
    │                 │ 6. 调用加密机解密 + 验签 │
    │                 │ POST /scyh-server/v101/session/decrypt
    │                 │ {                        │
    │                 │   "sessionId": "...",    │
    │                 │   "cipherText": "...",   │
    │                 │   "dilithiumSignature": "...",
    │                 │   "sm2Signature": "..."  │
    │                 │ }                        │
    │                 │────────▶│               │
    │                 │         │               │
    │                 │ 7. 加密机内：             │
    │                 │    a) SM4 解密           │
    │                 │    b) Dilithium 验签     │
    │                 │    c) SM2 验签           │
    │                 │         │               │
    │                 │ 8. 返回明文 + 验签结果   │
    │                 │ {         │             │
    │                 │   "plainText": "hex",   │
    │                 │   "dilithiumVerifyResult": true,
    │                 │   "sm2VerifyResult": true
    │                 │ }         │             │
    │                 │◀────────│               │
    │                 │                         │
    │                 │ 9. POST /api/data/receive
    │                 │ (业务网络，转发明文)     │
    │                 │────────────────────────────▶│
    │                 │ {                         │
    │                 │   "plainText": "hex",     │
    │                 │   "sessionId": "...",     │
    │                 │   "dilithiumVerifyResult": true,
    │                 │   "sm2VerifyResult": true,
    │                 │   "timestamp": 1712102400 │
    │                 │ }                         │
    │                 │                         │
    │                 │                         │ 10. 业务处理
    │                 │                         │ - 存储明文
    │                 │                         │ - 生成 dataId
    │                 │                         │
    │ 11. 200 OK      │                         │
    │ {               │                         │
    │   "success": true,                        │
    │   "dataId": "UUID"                        │
    │ }                 │                       │
    │◀────────────────────────────────────────────│
    │                 │                         │

⚠️ 关键安全点（DMZ 架构）：
- X-Session-Id 是必需的！Gateway 使用它查找会话上下文（SM4 密钥、公钥等）
- Gateway 在 DMZ 区调用加密机解密（内网隔离）
- Business Server 在业务网络区，无法访问加密机
- 明文数据由 Gateway 转发到 Business Server
- 密钥永不出 DMZ 区
- Business Server 接收已解密 + 已验签的明文
```

### 3.3 阶段 3：PSK 会话恢复（24 小时内）

```
┌────────┐   ┌────────┐   ┌──────────┐   ┌──────────┐
│ Android│   │ Gateway│   │Encryptor │   │ Business │
│        │   │        │   │ Machine  │   │  Server  │
└───┬────┘   └───┬────┘   └────┬─────┘   └────┬─────┘
    │            │             │              │
    │ 1. POST /alsp/v1/handshake/resume │      │
    │ {          │             │              │
    │   "type": "RESUME_SESSION", │            │
    │   "sessionId": "旧 SessionID", │         │
    │   "clientNonce": "新随机数",  │           │
    │   "pskHint": "PSK 标识"      │            │
    │ }          │             │              │
    │───────────▶│             │              │
    │            │             │              │
    │            │ 2. 验证 PSK  │              │
    │            │ - Session 未过期 (24h) │    │
    │            │ - PSK 匹配   │              │
    │            │ - 验证 Nonce  │              │
    │            │             │              │
    │ 3. 200 OK  │             │              │
    │ {          │             │              │
    │   "type": "RESUME_SUCCESS", │            │
    │   "sessionId": "新 SessionID", │         │
    │   "resumed": true,         │              │
    │   "expiresAt": 1712188800  │              │
    │ }          │             │              │
    │◀───────────│             │              │
    │            │             │              │
    │ === 会话恢复完成，可直接上传数据 === │    │
    │            │             │              │
    │ 4. 使用恢复的会话加密上传数据 │          │
    │───────────▶│             │              │
    │            │             │              │
```
┌────────┐   ┌────────┐   ┌──────────┐   ┌──────────┐
│ Android│   │ Gateway│   │Encryptor │   │ Business │
│        │   │        │   │ Machine  │   │  Server  │
└───┬────┘   └───┬────┘   └────┬─────┘   └────┬─────┘
    │            │             │              │
    │ 1. POST /gateway/session/resume │        │
    │ {          │             │              │
    │   "sessionId": "旧 SessionID", │         │
    │   "clientNonce": "16 字节",  │             │
    │   "pskHint": "PSK 标识"      │             │
    │ }          │             │              │
    │───────────▶│             │              │
    │            │             │              │
    │            │ 2. 验证 PSK  │              │
    │            │ - Session 未过期 (24h) │    │
    │            │ - PSK 匹配   │              │
    │            │             │              │
    │ 3. 200 OK  │             │              │
    │ {          │             │              │
    │   "sessionId": "新 SessionID", │         │
    │   "resumed": true,         │              │
    │   "expiresAt": 1712188800  │              │
    │ }          │             │              │
    │◀───────────│             │              │
    │            │             │              │
    │ === 会话恢复完成，可直接上传数据 === │    │
```

---

## 4. API 设计

### 4.1 通讯网关 API (quantum-encryptor-gateway)

#### 4.1.1 初始化会话

```http
POST /gateway/session/init
Content-Type: application/json

Request:
{
  "clientNonce": "a1b2c3d4e5f6g7h8i9j0",
  "kyberAlgorithm": "Kyber768",
  "dilithiumAlgorithm": "Dilithium2"
}

Response (200 OK):
{
  "code": 0,
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "kyberPublicKey": "hex 编码的 Kyber 公钥",
    "serverNonce": "x9y8z7w6v5u4t3s2r1q0p",
    "expiresAt": 1712188800
  },
  "msg": ""
}
```

#### 4.1.2 生成密钥对

```http
POST /gateway/session/genKeys
Content-Type: application/json
X-Session-Id: 550e8400-e29b-41d4-a716-446655440000

Response (200 OK):
{
  "code": 0,
  "data": {
    "sm2PublicKey": "hex",
    "sm2PrivateKey": "hex",
    "dilithiumPublicKey": "hex",
    "dilithiumPrivateKey": "hex"
  },
  "msg": ""
}
```

#### 4.1.3 上传加密数据

```http
POST /gateway/session/upload
Content-Type: application/json
X-Session-Id: 550e8400-e29b-41d4-a716-446655440000  ✅ 必需！
X-Nonce: a1b2c3d4e5f6g7h8
X-Timestamp: 1712102400
X-HMAC: hex

Request:
{
  "cipherText": "hex",
  "dilithiumSignature": "hex",
  "sm2Signature": "hex",
  "iv": "hex"
}

Response (200 OK):
{
  "code": 0,
  "data": {
    "success": true,
    "dataId": "data-uuid-123"
  },
  "msg": ""
}
```

**关键说明**：
- `X-Session-Id` 是**必需的**，Gateway 使用它来查找会话上下文
- 会话上下文包含：SM4 密钥、Dilithium 公钥、SM2 公钥等
- 没有 sessionId，Gateway 无法知道使用哪个密钥解密

#### 4.1.4 PSK 会话恢复

```http
POST /gateway/session/resume
Content-Type: application/json

Request:
{
  "sessionId": "旧 SessionID",
  "clientNonce": "新随机数",
  "pskHint": "PSK 标识符"
}

Response (200 OK):
{
  "code": 0,
  "data": {
    "sessionId": "新 SessionID",
    "resumed": true,
    "expiresAt": 1712188800
  },
  "msg": ""
}
```

### 4.2 业务 Server API (quantum-business-server)

#### 4.2.1 接收加密数据

```http
POST /api/data/receive
Content-Type: application/json
X-Session-Id: 550e8400-e29b-41d4-a716-446655440000  ✅ 必需！

Request:
{
  "cipherText": "hex",
  "dilithiumSignature": "hex",
  "sm2Signature": "hex",
  "iv": "hex"
}

Response (200 OK):
{
  "code": 0,
  "data": {
    "success": true,
    "dataId": "data-uuid-123"
  },
  "msg": ""
}
```

**关键说明**：
- `X-Session-Id` 是**必需的**，Gateway 使用它来查找会话上下文
- 会话上下文包含：SM4 密钥、Dilithium 公钥、SM2 公钥等
- 没有 sessionId，Gateway 无法知道使用哪个密钥解密

#### 4.2.2 解密并验签

```http
POST /api/crypto/decrypt-and-verify
Content-Type: application/json

Request:
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "cipherText": "hex",
  "dilithiumSignature": "hex",
  "sm2Signature": "hex",
  "iv": "hex"
}

Response (200 OK):
{
  "code": 0,
  "data": {
    "plainText": "hex",
    "dilithiumVerifyResult": true,
    "sm2VerifyResult": true
  },
  "msg": ""
}
```

#### 4.2.3 查询数据

```http
GET /api/data/{dataId}

Response (200 OK):
{
  "code": 0,
  "data": {
    "dataId": "data-uuid-123",
    "plainText": "解密后的明文",
    "timestamp": 1712102400,
    "status": "verified"
  },
  "msg": ""
}
```

### 4.3 加密机 API (quantum-mock-encryptor，扩展)

#### 4.3.1 会话解密 + 双重验签（新增）

```http
POST /scyh-server/v101/session/decrypt
Content-Type: application/json

Request:
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "cipherText": "hex",
  "dilithiumSignature": "hex",
  "sm2Signature": "hex",
  "iv": "hex"
}

Response (200 OK):
{
  "code": 0,
  "data": {
    "plainText": "hex",
    "dilithiumVerifyResult": true,
    "sm2VerifyResult": true
  },
  "msg": ""
}
```

---

## 5. 安全设计

### 5.1 防重放攻击机制

```java
public class ReplayProtectionFilter {
    
    // 时间戳窗口（5 分钟）
    private static final long TIMESTAMP_WINDOW_MS = 300000;
    
    // Nonce 缓存 TTL（5 分钟）
    private static final long NONCE_TTL_MS = 300000;
    
    // LRU 缓存用于 Nonce 检查
    private final Cache<String, Long> nonceCache = 
        Caffeine.newBuilder()
            .expireAfterWrite(NONCE_TTL_MS, TimeUnit.MILLISECONDS)
            .maximumSize(100000)
            .build();
    
    public boolean validateRequest(String sessionId, String nonce, 
                                   long timestamp, String signature) {
        long currentTime = System.currentTimeMillis();
        
        // 1. 时间戳验证（±5 分钟窗口）
        if (Math.abs(currentTime - timestamp) > TIMESTAMP_WINDOW_MS) {
            log.warn("Timestamp out of window: {}", timestamp);
            return false;
        }
        
        // 2. Nonce 缓存检查（防重放）
        if (nonceCache.getIfPresent(nonce) != null) {
            log.warn("Nonce replay detected: {}", nonce);
            return false;
        }
        nonceCache.put(nonce, currentTime);
        
        // 3. 会话有效性检查
        if (!sessionManager.isValidSession(sessionId)) {
            log.warn("Invalid session: {}", sessionId);
            return false;
        }
        
        // 4. HMAC 签名验证
        if (!verifyHMAC(signature, sessionId, nonce, timestamp)) {
            log.warn("Invalid HMAC signature");
            return false;
        }
        
        return true;
    }
    
    private boolean verifyHMAC(String signature, String sessionId, 
                               String nonce, long timestamp) {
        String expected = hmacSHA256(
            sessionId + nonce + timestamp,
            sessionManager.getSessionKey(sessionId)
        );
        return MessageDigest.isEqual(
            signature.getBytes(),
            expected.getBytes()
        );
    }
}
```

### 5.2 双重签名验证

```java
public class DualSignatureVerifier {
    
    /**
     * 双重签名验证（Dilithium + SM2）
     */
    public VerificationResult verify(
        String sessionId,
        String plainText,
        String dilithiumSignature,
        String sm2Signature
    ) {
        // 1. Dilithium 验签（抗量子）
        boolean dilithiumOk = encryptorClient.verifyDilithium(
            sessionId,
            plainText,
            dilithiumSignature
        );
        
        // 2. SM2 验签（国密）
        boolean sm2Ok = encryptorClient.verifySM2(
            sessionId,
            plainText,
            sm2Signature
        );
        
        // 3. 双重验证结果
        if (dilithiumOk && sm2Ok) {
            return VerificationResult.builder()
                .success(true)
                .message("双重验签通过")
                .build();
        } else {
            return VerificationResult.builder()
                .success(false)
                .message(String.format(
                    "验签失败：Dilithium=%s, SM2=%s",
                    dilithiumOk, sm2Ok
                ))
                .build();
        }
    }
}
```

### 5.3 会话管理（PSK 恢复）

```java
public class SessionManager {
    
    // 会话存储（支持 PSK 恢复）
    private final ConcurrentHashMap<String, SessionContext> sessions = 
        new ConcurrentHashMap<>();
    
    // PSK 映射（用于快速恢复）
    private final ConcurrentHashMap<String, String> pskMap = 
        new ConcurrentHashMap<>();
    
    // 会话过期时间（24 小时）
    private static final long SESSION_TTL_MS = 24 * 60 * 60 * 1000;
    
    public SessionContext createSession(SessionInitRequest request) {
        String sessionId = UUID.randomUUID().toString();
        String psk = generatePSK();
        
        SessionContext session = SessionContext.builder()
            .sessionId(sessionId)
            .psk(psk)
            .pskHint(generatePSKHint(psk))
            .kyberPublicKey(request.getKyberPublicKey())
            .dilithiumPublicKey(request.getDilithiumPublicKey())
            .createdAt(System.currentTimeMillis())
            .expiresAt(System.currentTimeMillis() + SESSION_TTL_MS)
            .build();
        
        sessions.put(sessionId, session);
        pskMap.put(session.getPskHint(), psk);
        
        return session;
    }
    
    public SessionContext resumeSession(String oldSessionId, String pskHint) {
        SessionContext oldSession = sessions.get(oldSessionId);
        if (oldSession == null || oldSession.isExpired()) {
            throw new SessionExpiredException("Session expired");
        }
        
        // 验证 PSK
        String expectedPSK = pskMap.get(pskHint);
        if (!expectedPSK.equals(oldSession.getPsk())) {
            throw new AuthenticationException("Invalid PSK");
        }
        
        // 创建新会话（继承 PSK）
        return createSession(oldSession.getKyberPublicKey(), 
                            oldSession.getDilithiumPublicKey(),
                            expectedPSK);
    }
}
```

---

## 6. ALSP 协议规范

### 6.1 协议分层

```
┌─────────────────────────────────────────────────────────────────┐
│  ALSP 协议栈                                                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  应用数据层 (Application Data Layer)                            │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  业务 JSON 数据                                             │  │
│  └───────────────────────────────────────────────────────────┘  │
│                           ▼                                     │
│  ALSP 记录协议 (ALSP Record Protocol)                           │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  ALSPHeader (16 字节)                                      │  │
│  │  ├─ Magic: 0xALSP (2 字节) - 协议标识                      │  │
│  │  ├─ Version: 1.0 (2 字节) - 协议版本                       │  │
│  │  ├─ Type: Handshake/Application/Alert (1 字节) - 记录类型  │  │
│  │  ├─ Sequence: 序列号 (4 字节) - 防重放                     │  │
│  │  ├─ Length: 数据长度 (4 字节)                              │  │
│  │  ├─ Flags: 标志位 (2 字节)                                 │  │
│  │  └─ Reserved: 保留 (1 字节)                                │  │
│  │                                                           │  │
│  │  ALSPRecord (可变长度)                                     │  │
│  │  ├─ Nonce: 16 字节随机数 - 防重放                           │  │
│  │  ├─ Timestamp: 8 字节时间戳 - 防重放                        │  │
│  │  ├─ EncryptedData: SM4 加密数据                            │  │
│  │  └─ HMAC: 32 字节完整性校验 (HMAC-SHA256)                  │  │
│  └───────────────────────────────────────────────────────────┘  │
│                           ▼                                     │
│  传输层：HTTP/1.1 (明文，无 TLS/SSL)                            │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  POST /alsp/v1/{endpoint}                                 │  │
│  │  Content-Type: application/alsp                           │  │
│  │  X-Session-Id: {sessionId}                                │  │
│  │  [ALSP Binary/JSON Payload]                               │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 握手协议

```java
// ALSP 握手消息类型
public enum ALSPHandshakeType {
    CLIENT_HELLO(1),
    SERVER_HELLO(2),
    CLIENT_KEY_EXCHANGE(3),
    SERVER_KEY_EXCHANGE(4),
    CLIENT_FINISHED(5),
    SERVER_FINISHED(6);
    
    private final int code;
}

// ClientHello 消息
@Data
@Builder
public class ClientHello {
    private String type = "CLIENT_HELLO";
    private String version = "1.0";
    private String clientNonce;      // 16 字节随机数 (hex)
    private List<String> cipherSuites; // 支持的密码套件
    private List<String> compression;  // 支持的压缩算法
}

// ServerHello 消息
@Data
@Builder
public class ServerHello {
    private String type = "SERVER_HELLO";
    private String version = "1.0";
    private String serverNonce;      // 16 字节随机数 (hex)
    private String selectedCipherSuite; // 选中的密码套件
    private String kyberPublicKey;   // Kyber 公钥 (hex)
    private String serverCert;       // 服务端证书/公钥
    private String serverSignature;  // 对 (clientNonce+serverNonce+pubkey) 的签名
}

// ClientKeyExchange 消息
@Data
@Builder
public class ClientKeyExchange {
    private String type = "CLIENT_KEY_EXCHANGE";
    private String encapsulatedKey;  // Kyber 加密的 SM4 会话密钥
    private String clientPublicKey;  // 客户端 SM2 公钥
    private String clientDilithiumPublicKey; // 客户端 Dilithium 公钥
    private String clientSignature;  // 对 (serverNonce+clientNonce) 的签名
}

// ServerKeyExchange 消息
@Data
@Builder
public class ServerKeyExchange {
    private String type = "SERVER_KEY_EXCHANGE";
    private String serverPublicKey;  // 服务端 SM2 公钥
    private String serverDilithiumPublicKey; // 服务端 Dilithium 公钥
    private String finished;         // HMAC(serverNonce, sharedSecret)
}

// Finished 消息
@Data
@Builder
public class Finished {
    private String type = "CLIENT_FINISHED" or "SERVER_FINISHED";
    private String finished;         // HMAC(nonce, sharedSecret)
}
```

### 6.3 记录协议

```java
// ALSP 记录头（16 字节）
@Data
@Builder
public class ALSPHeader {
    private static final String MAGIC = "0xALSP";
    
    private String magic = MAGIC;           // 2 字节
    private String version = "1.0";         // 2 字节
    private ALSPRecordType type;            // 1 字节
    private long sequence;                  // 4 字节
    private int length;                     // 4 字节
    private int flags;                      // 2 字节
    private int reserved;                   // 1 字节
}

// ALSP 记录体
@Data
@Builder
public class ALSPRecord {
    private ALSPHeader header;
    private String nonce;                   // 16 字节 (hex)
    private long timestamp;                 // 8 字节
    private String encryptedData;           // SM4 加密数据 (hex)
    private String hmac;                    // 32 字节 (hex)
}

// 记录类型
public enum ALSPRecordType {
    HANDSHAKE(0x01),
    APPLICATION_DATA(0x02),
    ALERT(0x03);
    
    private final int code;
}
```

### 6.4 警报协议

```java
// 警报消息
@Data
@Builder
public class ALSPAlert {
    private AlertLevel level;       // 警告级别
    private AlertCode code;         // 警报代码
    private String message;         // 错误描述
}

// 警报级别
public enum AlertLevel {
    WARNING(1),
    FATAL(2);
}

// 警报代码
public enum AlertCode {
    CLOSE_NOTIFY(0),
    UNEXPECTED_MESSAGE(10),
    BAD_RECORD_MAC(20),
    RECORD_OVERFLOW(22),
    HANDSHAKE_FAILURE(40),
    DECODE_ERROR(50),
    DECRYPT_ERROR(51),
    SESSION_EXPIRED(60),
    REPLAY_DETECTED(70),
    VERIFICATION_FAILED(80);
}
```

---

## 7. 数据模型

### 7.1 核心数据结构

```java
// 会话上下文
@Data
@Builder
public class SessionContext {
    private String sessionId;
    private String psk;              // PSK（用于恢复）
    private String pskHint;          // PSK 标识符
    private String kyberPublicKey;   // Kyber 公钥
    private String dilithiumPublicKey; // Dilithium 公钥
    private String sm2PublicKey;     // SM2 公钥
    private String sm2PrivateKey;    // SM2 私钥（加密存储）
    private String dilithiumPrivateKey; // Dilithium 私钥（加密存储）
    private long createdAt;
    private long expiresAt;
    private boolean resumed;         // 是否为恢复的会话
    
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}

// 上传请求
@Data
public class UploadRequest {
    @NotBlank
    private String cipherText;       // SM4 密文（hex）
    
    @NotBlank
    private String iv;               // CBC IV（hex）
    
    @NotBlank
    private String algorithm;        // SM4 模式
    
    @NotBlank
    private String dilithiumSignature; // Dilithium 签名（hex）
    
    @NotBlank
    private String sm2Signature;     // SM2 签名（hex）
}

// 解密响应
@Data
@Builder
public class DecryptResponse {
    private String plainText;        // 解密后的明文（hex）
    private boolean dilithiumVerifyResult; // Dilithium 验签结果
    private boolean sm2VerifyResult; // SM2 验签结果
    private String message;          // 错误信息（如果失败）
}

// 验签结果
@Data
@Builder
public class VerificationResult {
    private boolean success;
    private String message;
    private String plainText;
}
```

---

## 8. 实现计划

### 8.1 阶段 1：ALSP 协议库（Week 1-2）

- [ ] 创建 Spring Boot 项目结构
- [ ] 实现 Session Manager
- [ ] 实现 Replay Protection Filter
- [ ] 实现 Gateway API（/gateway/session/*）
- [ ] 集成加密机客户端
- [ ] 单元测试

### 7.2 阶段 2：业务 Server（Week 2-3）

- [ ] 创建 Spring Boot 项目结构
- [ ] 实现数据接收 API（/api/data/receive）
- [ ] 实现解密验签服务（调用加密机）
- [ ] 实现数据存储（内存/H2）
- [ ] 单元测试

### 7.3 阶段 3：加密机扩展（Week 3）

- [ ] 新增会话解密 API（/scyh-server/v101/session/decrypt）
- [ ] 实现双重签名验证逻辑
- [ ] 集成 Dilithium 验签
- [ ] 集成 SM2 验签
- [ ] 单元测试

### 7.4 阶段 4：Android 客户端集成（Week 4）

- [ ] 更新 API Client（添加 Gateway API）
- [ ] 实现双重签名生成
- [ ] 实现防重放 Nonce 生成
- [ ] 集成测试
- [ ] E2E 测试

### 7.5 阶段 5：测试与优化（Week 5）

- [ ] 性能测试
- [ ] 安全测试（防重放、中间人攻击）
- [ ] 文档完善
- [ ] POC 演示准备

---

## 8. 测试计划

### 8.1 单元测试

```java
// 防重放测试
@Test
void testReplayAttack_Prevented() {
    String nonce = generateNonce();
    long timestamp = System.currentTimeMillis();
    
    // 第一次请求（成功）
    assertTrue(replayFilter.validateRequest(sessionId, nonce, timestamp, signature));
    
    // 第二次请求（重放，失败）
    assertFalse(replayFilter.validateRequest(sessionId, nonce, timestamp, signature));
}

// 双重签名测试
@Test
void testDualSignature_VerifySuccess() {
    VerificationResult result = verifier.verify(
        sessionId, plainText, dilithiumSig, sm2Sig
    );
    
    assertTrue(result.isSuccess());
    assertTrue(result.isDilithiumVerifyResult());
    assertTrue(result.isSm2VerifyResult());
}

// PSK 恢复测试
@Test
void testPSKResume_Success() {
    SessionContext oldSession = sessionManager.createSession(request);
    SessionContext newSession = sessionManager.resumeSession(
        oldSession.getSessionId(),
        oldSession.getPskHint()
    );
    
    assertTrue(newSession.isResumed());
    assertEquals(oldSession.getPsk(), newSession.getPsk());
}
```

### 8.2 E2E 测试流程

```python
# 1. 初始化会话
session = client.init_session()

# 2. 生成密钥对
keys = client.gen_keys(session.id)

# 3. 加密 + 签名
cipher = client.sm4_encrypt(plaintext, session.key)
dilithium_sig = client.dilithium_sign(cipher, keys.dilithium_private)
sm2_sig = client.sm2_sign(cipher, keys.sm2_private)

# 4. 上传
response = client.upload(
    session_id=session.id,
    cipher_text=cipher,
    dilithium_signature=dilithium_sig,
    sm2_signature=sm2_sig
)

# 5. 验证
assert response.success == True
assert response.data_id is not None

# 6. PSK 恢复
new_session = client.resume_session(session.id)
assert new_session.resumed == True
```

---

## 9. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| **PSK 泄露** | 高 | PSK 加密存储，定期轮换 |
| **重放攻击** | 高 | Nonce+Timestamp 双重验证，5 分钟窗口 |
| **中间人攻击** | 高 | 双向认证（Challenge-Response） |
| **会话劫持** | 中 | Session ID 绑定客户端指纹 |
| **加密机单点故障** | 中 | 支持多加密机负载均衡（未来） |

---

## 10. 参考资料

1. **NIST PQC Standard**: [FIPS 203 (ML-KEM)](https://csrc.nist.gov/pubs/fips/203/final)
2. **NIST PQC Standard**: [FIPS 204 (ML-DSA)](https://csrc.nist.gov/pubs/fips/204/final)
3. **RFC 9180**: [HPKE - Hybrid Public Key Encryption](https://datatracker.ietf.org/doc/rfc9180/)
4. **TLS 1.3**: [RFC 8446](https://datatracker.ietf.org/doc/html/rfc8446)
5. **国密标准**: [GM/T 0003-2012 SM2](http://www.gmbz.org.cn/main/viewfile/20180108015852575323.html)
6. **Open Quantum Safe**: [liboqs](https://github.com/open-quantum-safe/liboqs)

---

## 11. 变更历史

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-04-03 | Sisyphus | Initial draft |

---

**文档状态**: 待审核  
**下一步**: 调用 spec-document-reviewer 进行评审

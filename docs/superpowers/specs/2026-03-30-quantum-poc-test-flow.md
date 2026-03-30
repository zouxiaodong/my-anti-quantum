# 抗量子密码POC测试流程说明

## 系统架构

```
Android客户端 → Gateway(8080) → Mock加密机(8101)
```

## 启动服务

```bash
# 1. 启动Mock加密机
cd quantum-mock-encryptor
mvn spring-boot:run

# 2. 启动Gateway
cd quantum-server  
mvn spring-boot:run
```

## 测试流程

### 流程一：SM2加解密测试

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 点击「初始化」 | 清空所有数据 |
| 2 | 填写明文 | 例: "Hello Quantum" |
| 3 | 点击「SM2密钥生成」 | 公钥/私钥显示在对应输入框 |
| 4 | 点击「SM2加密」 | 密文显示在密文输入框 |
| 5 | 点击「SM2解密」 | 明文恢复，显示在明文输入框 |

### 流程二：SM4对称加密测试

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 点击「初始化」 | 清空所有数据 |
| 2 | 填写明文 | 例: "Test Data" |
| 3 | 选择「SM4-CBC」或「SM4-ECB」 | |
| 4 | 点击「SM4加密」 | 密文显示 |
| 5 | 点击「SM4解密」 | 明文恢复 |

### 流程三：PQC密钥对生成测试

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 点击「初始化」 | 清空所有数据 |
| 2 | 选择「Kyber512」/「Kyber768」/「Kyber1024」 | |
| 3 | 点击对应「Kyber密钥生成」 | 公钥/私钥显示 |
| 4 | 切换到「Dilithium2」/「Dilithium3」/「Dilithium5」 | |
| 5 | 点击「Dilithium签名」 | 签名生成 |

### 流程四：混合加密测试(Hybrid Encrypt)

```
┌─────────────────────────────────────────────────┐
│  明文 + SM4密钥 + 签名私钥                      │
│       ↓                                        │
│  [SM4加密] → 密文                              │
│  [HMAC签名] → 签名                              │
│       ↓                                        │
│  输出: 密文 + 签名                              │
└─────────────────────────────────────────────────┘
```

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 点击「初始化」 | 清空所有数据 |
| 2 | 填写明文 | |
| 3 | 先生成SM2密钥对 | 公钥/私钥已保存 |
| 4 | 选择加密算法 | SM4-CBC |
| 5 | 点击「混合加密」 | 密文+签名生成 |
| 6 | 点击「混合解密」 | 明文恢复+验签 |

## 按钮功能对照表

| 按钮 | API调用 | 功能 |
|------|---------|------|
| 初始化 | - | 重置所有输入框 |
| 随机数生成 | POST /api/crypto/genRandom | 生成随机字节 |
| SM2密钥生成 | POST /api/crypto/ecc/genKeyPair | 生成SM2密钥对 |
| SM2加密 | POST /api/crypto/sm2/encrypt | SM2非对称加密 |
| SM2解密 | POST /api/crypto/sm2/decrypt | SM2非对称解密 |
| SM4-CBC | POST /api/crypto/sm4/encrypt | SM4对称加密(CBC模式) |
| SM4-ECB | POST /api/crypto/sm4/encrypt | SM4对称加密(ECB模式) |
| Kyber512密钥生成 | POST /api/crypto/pqc/genKeyPair | 生成Kyber512密钥对 |
| Kyber768密钥生成 | POST /api/crypto/pqc/genKeyPair | 生成Kyber768密钥对 |
| Kyber1024密钥生成 | POST /api/crypto/pqc/genKeyPair | 生成Kyber1024密钥对 |
| Dilithium2签名 | POST /api/crypto/pqc/genKeyPair | 生成Dilithium2签名密钥对 |
| 混合加密 | POST /api/crypto/encrypt | SM4加密+HMAC签名 |
| 混合解密 | POST /api/crypto/decrypt | SM4解密+HMAC验签 |

## 算法说明

### 中国商用密码(SSM)
- SM2: 非对称加密/签名(椭圆曲线)
- SM3: 哈希算法
- SM4: 对称加密(128位)

### 后量子密码(PQC)
- Kyber512/768/1024: 密钥封装机制(KEM)
- Dilithium2/3/5: 数字签名

## 快速测试脚本

```bash
cd quantum-mock-encryptor
python3 ../test_mock.py
```

## 注意事项

1. Mock加密机默认端口: 8101
2. Gateway默认端口: 8080
3. Android客户端需要连接Gateway(8080)，不是直接连Mock(8101)
4. PQC密钥封装/解封接口尚未在Android客户端实现

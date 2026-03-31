package com.quantum.poc.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CryptoSession {
    private String sessionId;
    private String kyberAlgorithm;
    private String kyberPublicKey;
    private String kyberPrivateKey;
    private String sm4SessionKey;     // 随机生成的SM4会话密钥
    private String keyCipher;        // 用Kyber加密后的会话密钥
    private String keyId;            // 密钥包装后的ID
    private String sm2PublicKey;
    private String sm2PrivateKey;
    private String dilithiumAlgorithm;
    private String dilithiumPublicKey;
    private String dilithiumPrivateKey;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean initialized;    // 是否已完成初始化
    private boolean keyWrapped;      // 是否已完成密钥包装
}
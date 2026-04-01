package com.quantum.poc.controller;

import com.quantum.poc.dto.*;
import com.quantum.poc.model.CryptoSession;
import com.quantum.poc.service.CryptoGatewayService;
import com.quantum.poc.service.SessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/crypto/session")
@CrossOrigin(origins = "*")
@Validated
public class SessionController {
    
    private static final Logger log = LoggerFactory.getLogger(SessionController.class);
    private static final String SESSION_HEADER = "X-Session-Id";
    
    private final SessionService sessionService;
    private final CryptoGatewayService cryptoGatewayService;
    
    public SessionController(SessionService sessionService, CryptoGatewayService cryptoGatewayService) {
        this.sessionService = sessionService;
        this.cryptoGatewayService = cryptoGatewayService;
    }
    
    @GetMapping("/genRandom")
    public Mono<ResponseEntity<Result<String>>> genRandom(
            @RequestParam(required = false, defaultValue = "32") Integer length) {
        log.info("========== 生成随机数 ==========");
        log.info("长度: {} 字节", length);
        
        return cryptoGatewayService.genRandom(length)
                .map(result -> {
                    log.info("随机数生成完成: {}...", result.getData().substring(0, 8));
                    return ResponseEntity.ok(result);
                });
    }
    
    @PostMapping("/init")
    public Mono<ResponseEntity<Result<SessionInitResponse>>> initSession(@Valid @RequestBody SessionInitRequest request) {
        String kyberAlg = request.getKyberAlgorithm() != null ? request.getKyberAlgorithm() : "Kyber512";
        String dilithiumAlg = request.getDilithiumAlgorithm() != null ? request.getDilithiumAlgorithm() : "Dilithium2";
        
        log.info("========== 初始化会话 ==========");
        log.info("Session Kyber算法: {}", kyberAlg);
        log.info("Session Dilithium算法: {}", dilithiumAlg);
        
        return cryptoGatewayService.genPqcKeyPair(new KeyPairRequest(kyberAlg))
                .flatMap(kyberResult -> {
                    if (kyberResult.getCode() != 0) {
                        log.error("Kyber密钥生成失败: {}", kyberResult.getMsg());
                        return Mono.just(ResponseEntity.ok(Result.error(1, "Kyber密钥生成失败: " + kyberResult.getMsg())));
                    }
                    
                    Map<String, String> kyberKeys = kyberResult.getData();
                    
                    CryptoSession session = sessionService.createSession(kyberAlg, dilithiumAlg);
                    session.setKyberPublicKey(kyberKeys.get("publicKey"));
                    session.setKyberPrivateKey(kyberKeys.get("privateKey"));
                    session.setInitialized(true);
                    sessionService.updateSession(session.getSessionId(), session);
                    
                    log.info("========== 会话初始化完成 ==========");
                    log.info("SessionID: {}", session.getSessionId());
                    log.info("Kyber公钥: {}...", session.getKyberPublicKey().substring(0, 16));
                    
                    SessionInitResponse response = new SessionInitResponse();
                    response.setSessionId(session.getSessionId());
                    response.setKyberAlgorithm(kyberAlg);
                    response.setDilithiumAlgorithm(dilithiumAlg);
                    response.setKyberPublicKey(kyberKeys.get("publicKey"));
                    response.setKyberPrivateKey(kyberKeys.get("privateKey"));
                    response.setMessage("会话创建成功，Kyber密钥对已生成");
                    
                    return Mono.just(ResponseEntity.ok(Result.success(response)));
                });
    }
    
    @GetMapping
    public ResponseEntity<Result<SessionInitResponse>> getSession(@RequestHeader(SESSION_HEADER) @NotBlank String sessionId) {
        log.info("查询会话: {}", sessionId);
        
        return sessionService.getSession(sessionId)
                .map(session -> {
                    SessionInitResponse response = new SessionInitResponse();
                    response.setSessionId(session.getSessionId());
                    response.setKyberAlgorithm(session.getKyberAlgorithm());
                    response.setDilithiumAlgorithm(session.getDilithiumAlgorithm());
                    response.setKyberPublicKey(session.getKyberPublicKey());
                    return ResponseEntity.ok(Result.success(response, "会话存在"));
                })
                .orElse(ResponseEntity.ok(Result.error(1, "会话不存在或已过期")));
    }
    
    @DeleteMapping
    public ResponseEntity<Result<String>> deleteSession(@RequestHeader(SESSION_HEADER) @NotBlank String sessionId) {
        log.info("删除会话: {}", sessionId);
        
        if (sessionService.isValidSession(sessionId)) {
            sessionService.deleteSession(sessionId);
            return ResponseEntity.ok(Result.success("会话已删除"));
        }
        return ResponseEntity.ok(Result.error(1, "会话不存在"));
    }
    
    @PostMapping("/wrapKey")
    public Mono<ResponseEntity<Result<Map<String, String>>>> wrapKey(@RequestHeader(SESSION_HEADER) @NotBlank String sessionId) {
        log.info("========== 密钥包装 ==========");
        log.info("SessionID: {}", sessionId);
        
        return Mono.fromCallable(() -> sessionService.getSession(sessionId))
                .flatMap(optSession -> {
                    if (optSession.isEmpty()) {
                        return Mono.just(ResponseEntity.ok(Result.<Map<String, String>>error(1, "会话不存在或已过期")));
                    }
                    CryptoSession session = optSession.get();
                    
                    return cryptoGatewayService.genRandom(16)
                            .flatMap(randomResult -> {
                                String sessionKey = randomResult.getData();
                                log.info("生成的SM4会话密钥: {}...", sessionKey.substring(0, 8));
                                
                                PqcKeyWrapperRequest wrapRequest = new PqcKeyWrapperRequest();
                                wrapRequest.setAlgorithm(session.getKyberAlgorithm());
                                wrapRequest.setPqcPubkey(session.getKyberPublicKey());
                                wrapRequest.setSymmetricKey(sessionKey);
                                
                                return cryptoGatewayService.pqcKeyWrapper(wrapRequest)
                                        .map(wrapResult -> {
                                            if (wrapResult.getCode() != 0) {
                                                log.error("密钥包装失败: {}", wrapResult.getMsg());
                                                return ResponseEntity.ok(Result.<Map<String, String>>error(1, "密钥包装失败: " + wrapResult.getMsg()));
                                            }
                                            
                                            session.setSm4SessionKey(sessionKey);
                                            session.setKeyCipher(wrapResult.getData().getKeyCipher());
                                            session.setKeyId(wrapResult.getData().getKeyId());
                                            session.setKeyWrapped(true);
                                            sessionService.updateSession(sessionId, session);
                                            
                                            log.info("========== 密钥包装完成 ==========");
                                            log.info("KeyCipher: {}...", wrapResult.getData().getKeyCipher().substring(0, 16));
                                            log.info("KeyID: {}", wrapResult.getData().getKeyId());
                                            
                                            Map<String, String> result = Map.of(
                                                    "sessionKey", sessionKey,
                                                    "keyCipher", wrapResult.getData().getKeyCipher(),
                                                    "keyId", wrapResult.getData().getKeyId()
                                            );
                                            return ResponseEntity.ok(Result.success(result));
                                        });
                            });
                });
    }
    
    @PostMapping("/genKeys")
    public Mono<ResponseEntity<Result<Map<String, String>>>> generateKeys(@RequestHeader(SESSION_HEADER) @NotBlank String sessionId) {
        log.info("========== 生成SM2和Dilithium密钥对 ==========");
        log.info("SessionID: {}", sessionId);
        
        return Mono.fromCallable(() -> sessionService.getSession(sessionId))
                .flatMap(optSession -> {
                    if (optSession.isEmpty()) {
                        return Mono.just(ResponseEntity.ok(Result.<Map<String, String>>error(1, "会话不存在或已过期")));
                    }
                    CryptoSession session = optSession.get();
                    
                    return cryptoGatewayService.genEccKeyPair()
                            .flatMap(eccResult -> {
                                if (eccResult.getCode() != 0) {
                                    log.error("SM2密钥生成失败: {}", eccResult.getMsg());
                                    return Mono.just(ResponseEntity.<Result<Map<String, String>>>ok(Result.error(1, "SM2密钥生成失败")));
                                }
                                
                                Map<String, String> eccData = eccResult.getData();
                                session.setSm2PublicKey(eccData.get("publicKey"));
                                session.setSm2PrivateKey(eccData.get("privateKey"));
                                
                                KeyPairRequest dilithiumRequest = new KeyPairRequest(session.getDilithiumAlgorithm());
                                return cryptoGatewayService.genPqcKeyPair(dilithiumRequest)
                                        .map(dilithiumResult -> {
                                            if (dilithiumResult.getCode() != 0) {
                                                log.error("Dilithium密钥生成失败: {}", dilithiumResult.getMsg());
                                                return ResponseEntity.<Result<Map<String, String>>>ok(Result.error(1, "Dilithium密钥生成失败"));
                                            }
                                            
                                            Map<String, String> dilithiumData = dilithiumResult.getData();
                                            session.setDilithiumPublicKey(dilithiumData.get("publicKey"));
                                            session.setDilithiumPrivateKey(dilithiumData.get("privateKey"));
                                            sessionService.updateSession(sessionId, session);
                                            
                                            log.info("========== 密钥对生成完成 ==========");
                                            log.info("SM2公钥: {}...", session.getSm2PublicKey().substring(0, 16));
                                            log.info("Dilithium公钥: {}...", session.getDilithiumPublicKey().substring(0, 16));
                                            
                                            Map<String, String> result = Map.of(
                                                    "sm2PublicKey", session.getSm2PublicKey(),
                                                    "sm2PrivateKey", session.getSm2PrivateKey(),
                                                    "dilithiumPublicKey", session.getDilithiumPublicKey(),
                                                    "dilithiumPrivateKey", session.getDilithiumPrivateKey()
                                            );
                                            return ResponseEntity.ok(Result.success(result));
                                        });
                            });
                });
    }
    
    @PostMapping("/encrypt")
    public Mono<ResponseEntity<Result<SessionEncryptResponse>>> encrypt(
            @RequestHeader(SESSION_HEADER) @NotBlank String sessionId,
            @Valid @RequestBody SessionEncryptRequest request) {
        log.info("========== 会话加密 ==========");
        log.info("SessionID: {}", sessionId);
        log.info("待加密数据: {}", request.getData());
        
        return Mono.fromCallable(() -> sessionService.getSession(sessionId))
                .flatMap(optSession -> {
                    if (optSession.isEmpty()) {
                        return Mono.just(ResponseEntity.ok(Result.<SessionEncryptResponse>error(1, "会话不存在或已过期")));
                    }
                    CryptoSession session = optSession.get();
                    
                    if (session.getSm4SessionKey() == null) {
                        return Mono.just(ResponseEntity.ok(Result.<SessionEncryptResponse>error(1, "会话密钥未生成，请先调用 wrapKey")));
                    }
                    
                    EncryptRequest encRequest = new EncryptRequest();
                    encRequest.setData(request.getData());
                    encRequest.setKeyData(session.getSm4SessionKey());
                    encRequest.setAlgorithm(request.getSm4Algorithm() != null ? request.getSm4Algorithm() : "SM4/CBC/NoPadding");
                    encRequest.setIv(request.getIv());
                    
                    return cryptoGatewayService.sm4Encrypt(encRequest)
                            .flatMap(encResult -> {
                                log.info("SM4加密完成, 密文: {}...", encResult.getData().substring(0, 16));
                                
                                Sm2Request signRequest = new Sm2Request();
                                signRequest.setData(encResult.getData());
                                signRequest.setPrivateKey(session.getSm2PrivateKey());
                                signRequest.setAlgorithm("SM3withSM2");
                                
                                return cryptoGatewayService.sm2Sign(signRequest)
                                        .map(signResult -> {
                                            String signature = signResult.getData();
                                            log.info("SM2签名完成, 签名: {}...", signature.substring(0, 16));
                                            
                                            SessionEncryptResponse response = new SessionEncryptResponse();
                                            response.setCipherText(encResult.getData());
                                            response.setSignature(signature);
                                            
                                            log.info("========== 加密完成 ==========");
                                            return ResponseEntity.ok(Result.success(response));
                                        });
                            });
                });
    }
    
    @PostMapping("/decrypt")
    public Mono<ResponseEntity<Result<SessionDecryptResponse>>> decrypt(
            @RequestHeader(SESSION_HEADER) @NotBlank String sessionId,
            @Valid @RequestBody SessionDecryptRequest request) {
        log.info("========== 会话解密+验签 ==========");
        log.info("SessionID: {}", sessionId);
        log.info("密文: {}...", request.getCipherText() != null ? request.getCipherText().substring(0, 16) : "null");
        
        return Mono.fromCallable(() -> sessionService.getSession(sessionId))
                .flatMap(optSession -> {
                    if (optSession.isEmpty()) {
                        return Mono.just(ResponseEntity.ok(Result.<SessionDecryptResponse>error(1, "会话不存在或已过期")));
                    }
                    CryptoSession session = optSession.get();
                    
                    if (session.getSm4SessionKey() == null) {
                        return Mono.just(ResponseEntity.ok(Result.<SessionDecryptResponse>error(1, "会话密钥未生成")));
                    }
                    
                    Sm2VerifyRequest verifyRequest = new Sm2VerifyRequest();
                    verifyRequest.setData(request.getCipherText());
                    verifyRequest.setSignature(request.getSignature());
                    verifyRequest.setPublicKey(session.getSm2PublicKey());
                    verifyRequest.setAlgorithm("SM3withSM2");
                    
                    return cryptoGatewayService.sm2Verify(verifyRequest)
                            .flatMap(verifyResult -> {
                                log.info("SM2验签结果: {}", verifyResult.getCode() == 0 ? "成功" : "失败");
                                
                                if (verifyResult.getCode() != 0) {
                                    SessionDecryptResponse response = new SessionDecryptResponse();
                                    response.setPlainText(null);
                                    response.setSm2VerifyResult(false);
                                    return Mono.just(ResponseEntity.ok(Result.success(response, "SM2验签失败")));
                                }
                                
                                EncryptRequest decRequest = new EncryptRequest();
                                decRequest.setData(request.getCipherText());
                                decRequest.setKeyData(session.getSm4SessionKey());
                                decRequest.setAlgorithm(request.getSm4Algorithm() != null ? request.getSm4Algorithm() : "SM4/CBC/NoPadding");
                                decRequest.setIv(request.getIv());
                                
                                return cryptoGatewayService.sm4Decrypt(decRequest)
                                        .map(decResult -> {
                                            log.info("SM4解密完成, 明文: {}", decResult.getData());
                                            
                                            SessionDecryptResponse response = new SessionDecryptResponse();
                                            response.setPlainText(decResult.getData());
                                            response.setSm2VerifyResult(true);
                                            
                                            log.info("========== 解密+验签完成 ==========");
                                            return ResponseEntity.ok(Result.success(response));
                                        });
                            });
                });
    }
}
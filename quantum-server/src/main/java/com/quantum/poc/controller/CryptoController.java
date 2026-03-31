package com.quantum.poc.controller;

import com.quantum.poc.dto.*;
import com.quantum.poc.service.CryptoGatewayService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/crypto")
@CrossOrigin(origins = "*")
public class CryptoController {
    
    private static final Logger log = LoggerFactory.getLogger(CryptoController.class);
    
    private final CryptoGatewayService cryptoGatewayService;
    
    public CryptoController(CryptoGatewayService cryptoGatewayService) {
        this.cryptoGatewayService = cryptoGatewayService;
    }
    
    @PostMapping("/genRandom")
    public Mono<ResponseEntity<Result<String>>> genRandom(@RequestParam(required = false, defaultValue = "32") Integer length) {
        log.info("[Controller] 收到 genRandom 请求, length={}", length);
        return cryptoGatewayService.genRandom(length)
                .map(result -> {
                    log.info("[Controller] genRandom 响应: code={}", result.getCode());
                    return ResponseEntity.ok(result);
                });
    }
    
    @PostMapping("/sm4/encrypt")
    public Mono<ResponseEntity<Result<String>>> sm4Encrypt(@Valid @RequestBody EncryptRequest request) {
        log.info("[Controller] 收到 sm4Encrypt 请求, algorithm={}", request.getAlgorithm());
        return cryptoGatewayService.sm4Encrypt(request)
                .map(result -> {
                    log.info("[Controller] sm4Encrypt 响应: code={}", result.getCode());
                    return ResponseEntity.ok(result);
                });
    }
    
    @PostMapping("/sm4/decrypt")
    public Mono<ResponseEntity<Result<String>>> sm4Decrypt(@Valid @RequestBody EncryptRequest request) {
        log.info("[Controller] 收到 sm4Decrypt 请求, algorithm={}", request.getAlgorithm());
        return cryptoGatewayService.sm4Decrypt(request)
                .map(result -> {
                    log.info("[Controller] sm4Decrypt 响应: code={}", result.getCode());
                    return ResponseEntity.ok(result);
                });
    }
    
    @PostMapping("/hash")
    public Mono<ResponseEntity<Result<String>>> hash(@Valid @RequestBody HashRequest request) {
        log.info("[Controller] 收到 hash 请求, algorithm={}", request.getAlgorithm());
        return cryptoGatewayService.hash(request)
                .map(result -> {
                    log.info("[Controller] hash 响应: code={}", result.getCode());
                    return ResponseEntity.ok(result);
                });
    }
    
    @PostMapping("/hmac")
    public Mono<ResponseEntity<Result<String>>> hmac(@Valid @RequestBody HMacRequest request) {
        log.info("[Controller] 收到 hmac 请求");
        return cryptoGatewayService.hmac(request)
                .map(result -> {
                    log.info("[Controller] hmac 响应: code={}", result.getCode());
                    return ResponseEntity.ok(result);
                });
    }
    
    @PostMapping("/ecc/genKeyPair")
    public Mono<ResponseEntity<Result<Map>>> genEccKeyPair() {
        log.info("[Controller] 收到 genEccKeyPair 请求");
        return cryptoGatewayService.genEccKeyPair()
                .map(result -> {
                    log.info("[Controller] genEccKeyPair 响应: code={}", result.getCode());
                    return ResponseEntity.ok(result);
                });
    }
    
    @PostMapping("/sm2/encrypt")
    public Mono<ResponseEntity<Result<String>>> sm2Encrypt(@Valid @RequestBody Sm2Request request) {
        log.info("[Controller] 收到 sm2Encrypt 请求");
        return cryptoGatewayService.sm2Encrypt(request)
                .map(result -> {
                    log.info("[Controller] sm2Encrypt 响应: code={}", result.getCode());
                    return ResponseEntity.ok(result);
                });
    }
    
    @PostMapping("/sm2/decrypt")
    public Mono<ResponseEntity<Result<String>>> sm2Decrypt(@Valid @RequestBody Sm2Request request) {
        log.info("[Controller] 收到 sm2Decrypt 请求");
        return cryptoGatewayService.sm2Decrypt(request)
                .map(result -> {
                    log.info("[Controller] sm2Decrypt 响应: code={}", result.getCode());
                    return ResponseEntity.ok(result);
                });
    }
    
    @PostMapping("/sm2/sign")
    public Mono<ResponseEntity<Result<String>>> sm2Sign(@Valid @RequestBody Sm2Request request) {
        log.info("[Controller] 收到 sm2Sign 请求");
        return cryptoGatewayService.sm2Sign(request)
                .map(result -> {
                    log.info("[Controller] sm2Sign 响应: code={}", result.getCode());
                    return ResponseEntity.ok(result);
                });
    }
    
    @PostMapping("/sm2/verify")
    public Mono<ResponseEntity<Result<String>>> sm2Verify(@Valid @RequestBody Sm2VerifyRequest request) {
        log.info("[Controller] 收到 sm2Verify 请求");
        return cryptoGatewayService.sm2Verify(request)
                .map(result -> {
                    log.info("[Controller] sm2Verify 响应: code={}", result.getCode());
                    return ResponseEntity.ok(result);
                });
    }
    
    @PostMapping("/pqc/genKeyPair")
    public Mono<ResponseEntity<Result<Map>>> genPqcKeyPair(@Valid @RequestBody KeyPairRequest request) {
        log.info("[Controller] 收到 genPqcKeyPair 请求, algorithm={}", request.getAlgorithm());
        return cryptoGatewayService.genPqcKeyPair(request)
                .map(result -> {
                    log.info("[Controller] genPqcKeyPair 响应: code={}", result.getCode());
                    return ResponseEntity.ok(result);
                });
    }
    
    @PostMapping("/pqc/wrapKey")
    public Mono<ResponseEntity<Result<PqcKeyWrapperResponse>>> pqcKeyWrapper(@Valid @RequestBody PqcKeyWrapperRequest request) {
        log.info("[Controller] 收到 pqcKeyWrapper 请求, algorithm={}", request.getAlgorithm());
        return cryptoGatewayService.pqcKeyWrapper(request)
                .map(result -> {
                    log.info("[Controller] pqcKeyWrapper 响应: code={}", result.getCode());
                    return ResponseEntity.ok(result);
                });
    }
    
    @PostMapping("/pqc/unwrapKey")
    public Mono<ResponseEntity<Result<String>>> pqcKeyUnwrapper(@Valid @RequestBody PqcKeyUnwrapperRequest request) {
        log.info("[Controller] 收到 pqcKeyUnwrapper 请求, algorithm={}", request.getAlgorithm());
        return cryptoGatewayService.pqcKeyUnwrapper(request)
                .map(result -> {
                    log.info("[Controller] pqcKeyUnwrapper 响应: code={}", result.getCode());
                    return ResponseEntity.ok(result);
                });
    }
    
    @PostMapping("/encrypt")
    public Mono<ResponseEntity<Result<HybridEncryptResponse>>> hybridEncrypt(@Valid @RequestBody HybridEncryptRequest request) {
        log.info("[Controller] 收到 hybridEncrypt 请求, sm4Algorithm={}", request.getSm4Algorithm());
        EncryptRequest encRequest = new EncryptRequest();
        encRequest.setData(request.getData());
        encRequest.setKeyData(request.getSm4Key());
        encRequest.setAlgorithm(request.getSm4Algorithm());
        
        return cryptoGatewayService.sm4Encrypt(encRequest)
                .flatMap(encResult -> {
                    HMacRequest hmacRequest = new HMacRequest();
                    hmacRequest.setData(encResult.getData());
                    hmacRequest.setKey(request.getSignPrivateKey());
                    
                    return cryptoGatewayService.hmac(hmacRequest)
                            .map(hmacResult -> {
                                HybridEncryptResponse response = new HybridEncryptResponse();
                                response.setCipherText(encResult.getData());
                                response.setSignature(hmacResult.getData());
                                log.info("[Controller] hybridEncrypt 响应成功");
                                return ResponseEntity.ok(Result.success(response));
                            });
                });
    }
    
    @PostMapping("/decrypt")
    public Mono<ResponseEntity<Result<HybridDecryptResponse>>> hybridDecrypt(@Valid @RequestBody HybridDecryptRequest request) {
        log.info("========== 服务端解密验证流程开始 ==========");
        log.info("[1/3] 收到密文: {}... (长度: {})", 
                request.getCipherText() != null ? request.getCipherText().substring(0, Math.min(32, request.getCipherText().length())) : "null",
                request.getCipherText() != null ? request.getCipherText().length() : 0);
        log.info("[1/3] 收到密钥(SM4): {}...", request.getSm4Key() != null ? request.getSm4Key().substring(0, 8) : "null");
        log.info("[1/3] 收到签名: {}...", request.getSignature() != null ? request.getSignature().substring(0, 16) : "null");
        
        HMacRequest hmacRequest = new HMacRequest();
        hmacRequest.setData(request.getCipherText());
        hmacRequest.setKey(request.getSignPublicKey());
        
        return cryptoGatewayService.hmac(hmacRequest)
                .flatMap(hmacResult -> {
                    log.info("[2/3] 验签中... 客户端签名: {}", request.getSignature());
                    log.info("[2/3] 服务端计算签名: {}...", hmacResult.getData() != null ? hmacResult.getData().substring(0, 16) : "null");
                    
                    boolean verifyResult = hmacResult.getData() != null 
                            && hmacResult.getData().equals(request.getSignature());
                    
                    log.info("[2/3] 验签结果: {}", verifyResult ? "✅ 成功" : "❌ 失败");
                    
                    if (!verifyResult) {
                        log.info("========== 服务端解密验证流程结束(验签失败) ==========");
                        HybridDecryptResponse response = new HybridDecryptResponse();
                        response.setVerifyResult(false);
                        return Mono.just(ResponseEntity.ok(Result.success(response, "验签失败")));
                    }
                    
                    EncryptRequest decRequest = new EncryptRequest();
                    decRequest.setData(request.getCipherText());
                    decRequest.setKeyData(request.getSm4Key());
                    decRequest.setAlgorithm(request.getSm4Algorithm());
                    
                    return cryptoGatewayService.sm4Decrypt(decRequest)
                            .map(decResult -> {
                                log.info("[3/3] 解密成功!");
                                log.info("[3/3] 解密后明文: {}", decResult.getData());
                                log.info("========== 服务端解密验证流程结束 ==========");
                                
                                HybridDecryptResponse response = new HybridDecryptResponse();
                                response.setPlainText(decResult.getData());
                                response.setVerifyResult(true);
                                return ResponseEntity.ok(Result.success(response));
                            });
                });
    }
    
    @PostMapping("/test/verify")
    public Mono<ResponseEntity<Result<Map<String, String>>>> testVerify(@Valid @RequestBody TestVerifyRequest request) {
        log.info("==================== POC验证测试端点 ====================");
        log.info("【步骤1】密钥交换 (Kyber)");
        log.info("  - 接收加密的会话密钥(KeyCipher): {}...", 
                request.getKeyCipher() != null ? request.getKeyCipher().substring(0, 16) : "null");
        log.info("  - KeyID: {}", request.getKeyId());
        
        PqcKeyUnwrapperRequest unwrapRequest = new PqcKeyUnwrapperRequest();
        unwrapRequest.setAlgorithm(request.getAlgorithm());
        unwrapRequest.setCipherText(request.getKeyCipher());
        unwrapRequest.setPqcPrikey(request.getPqcPrivateKey());
        
        return cryptoGatewayService.pqcKeyUnwrapper(unwrapRequest).flatMap(unwrapResult -> {
            String sessionKey = unwrapResult.getData();
            log.info("【步骤1】密钥交换完成 - 解密后会话密钥: {}...", sessionKey.substring(0, 8));
            
            log.info("【步骤2】SM4解密");
            log.info("  - 接收密文: {}...", 
                    request.getCipherText() != null ? request.getCipherText().substring(0, 16) : "null");
            log.info("  - 使用会话密钥: {}...", sessionKey.substring(0, 8));
            
            EncryptRequest decRequest = new EncryptRequest();
            decRequest.setData(request.getCipherText());
            decRequest.setKeyData(sessionKey);
            decRequest.setAlgorithm(request.getSm4Algorithm() != null ? request.getSm4Algorithm() : "SM4/CBC/NoPadding");
            decRequest.setIv(request.getIv());
            
            return cryptoGatewayService.sm4Decrypt(decRequest)
                    .flatMap(decResult -> {
                        String plainText = decResult.getData();
                        log.info("【步骤2】SM4解密完成");
                        log.info("  - 解密后明文: {}", plainText);
                        
                        log.info("【步骤3】Dilithium验签");
                        log.info("  - 原始数据(Hex): {}", request.getData());
                        log.info("  - 接收签名: {}...", 
                                request.getSignature() != null ? request.getSignature().substring(0, 16) : "null");
                        
                        HMacRequest hmacRequest = new HMacRequest();
                        hmacRequest.setData(request.getCipherText());
                        hmacRequest.setKey(request.getSignPublicKey());
                        
                        return cryptoGatewayService.hmac(hmacRequest)
                                .map(hmacResult -> {
                                    boolean verifyResult = hmacResult.getData() != null 
                                            && hmacResult.getData().equals(request.getSignature());
                                    
                                    log.info("【步骤3】Dilithium验签结果: {}", verifyResult ? "✅ 成功" : "❌ 失败");
                                    log.info("==================== POC验证测试完成 ====================");
                                    
                                    Map<String, String> result = Map.of(
                                            "plainText", plainText,
                                            "signatureVerify", String.valueOf(verifyResult),
                                            "sessionKey", sessionKey
                                    );
                                    return ResponseEntity.ok(Result.success(result));
                                });
                    });
        });
    }
}

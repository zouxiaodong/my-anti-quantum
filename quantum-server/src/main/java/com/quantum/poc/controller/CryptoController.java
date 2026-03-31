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
        log.info("[Controller] 收到 hybridDecrypt 请求, sm4Algorithm={}", request.getSm4Algorithm());
        HMacRequest hmacRequest = new HMacRequest();
        hmacRequest.setData(request.getCipherText());
        hmacRequest.setKey(request.getSignPublicKey());
        
        return cryptoGatewayService.hmac(hmacRequest)
                .flatMap(hmacResult -> {
                    boolean verifyResult = hmacResult.getData() != null 
                            && hmacResult.getData().equals(request.getSignature());
                    
                    if (!verifyResult) {
                        HybridDecryptResponse response = new HybridDecryptResponse();
                        response.setVerifyResult(false);
                        log.info("[Controller] hybridDecrypt 验签失败");
                        return Mono.just(ResponseEntity.ok(Result.success(response, "验签失败")));
                    }
                    
                    EncryptRequest decRequest = new EncryptRequest();
                    decRequest.setData(request.getCipherText());
                    decRequest.setKeyData(request.getSm4Key());
                    decRequest.setAlgorithm(request.getSm4Algorithm());
                    
                    return cryptoGatewayService.sm4Decrypt(decRequest)
                            .map(decResult -> {
                                HybridDecryptResponse response = new HybridDecryptResponse();
                                response.setPlainText(decResult.getData());
                                response.setVerifyResult(true);
                                log.info("[Controller] hybridDecrypt 响应成功");
                                return ResponseEntity.ok(Result.success(response));
                            });
                });
    }
}

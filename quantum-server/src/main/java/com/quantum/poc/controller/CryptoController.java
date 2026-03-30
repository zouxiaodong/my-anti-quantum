package com.quantum.poc.controller;

import com.quantum.poc.dto.*;
import com.quantum.poc.service.CryptoGatewayService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/crypto")
@CrossOrigin(origins = "*")
public class CryptoController {
    
    private final CryptoGatewayService cryptoGatewayService;
    
    public CryptoController(CryptoGatewayService cryptoGatewayService) {
        this.cryptoGatewayService = cryptoGatewayService;
    }
    
    @PostMapping("/genRandom")
    public Mono<ResponseEntity<Result<String>>> genRandom(@RequestParam(required = false, defaultValue = "32") Integer length) {
        return cryptoGatewayService.genRandom(length)
                .map(ResponseEntity::ok);
    }
    
    @PostMapping("/sm4/encrypt")
    public Mono<ResponseEntity<Result<String>>> sm4Encrypt(@Valid @RequestBody EncryptRequest request) {
        return cryptoGatewayService.sm4Encrypt(request)
                .map(ResponseEntity::ok);
    }
    
    @PostMapping("/sm4/decrypt")
    public Mono<ResponseEntity<Result<String>>> sm4Decrypt(@Valid @RequestBody EncryptRequest request) {
        return cryptoGatewayService.sm4Decrypt(request)
                .map(ResponseEntity::ok);
    }
    
    @PostMapping("/hash")
    public Mono<ResponseEntity<Result<String>>> hash(@Valid @RequestBody HashRequest request) {
        return cryptoGatewayService.hash(request)
                .map(ResponseEntity::ok);
    }
    
    @PostMapping("/hmac")
    public Mono<ResponseEntity<Result<String>>> hmac(@Valid @RequestBody HMacRequest request) {
        return cryptoGatewayService.hmac(request)
                .map(ResponseEntity::ok);
    }
    
    @PostMapping("/ecc/genKeyPair")
    public Mono<ResponseEntity<Result<Map>>> genEccKeyPair() {
        return cryptoGatewayService.genEccKeyPair()
                .map(ResponseEntity::ok);
    }
    
    @PostMapping("/sm2/encrypt")
    public Mono<ResponseEntity<Result<String>>> sm2Encrypt(@Valid @RequestBody Sm2Request request) {
        return cryptoGatewayService.sm2Encrypt(request)
                .map(ResponseEntity::ok);
    }
    
    @PostMapping("/sm2/decrypt")
    public Mono<ResponseEntity<Result<String>>> sm2Decrypt(@Valid @RequestBody Sm2Request request) {
        return cryptoGatewayService.sm2Decrypt(request)
                .map(ResponseEntity::ok);
    }
    
    @PostMapping("/pqc/genKeyPair")
    public Mono<ResponseEntity<Result<Map>>> genPqcKeyPair(@Valid @RequestBody KeyPairRequest request) {
        return cryptoGatewayService.genPqcKeyPair(request)
                .map(ResponseEntity::ok);
    }
    
    @PostMapping("/pqc/wrapKey")
    public Mono<ResponseEntity<Result<PqcKeyWrapperResponse>>> pqcKeyWrapper(@Valid @RequestBody PqcKeyWrapperRequest request) {
        return cryptoGatewayService.pqcKeyWrapper(request)
                .map(ResponseEntity::ok);
    }
    
    @PostMapping("/pqc/unwrapKey")
    public Mono<ResponseEntity<Result<String>>> pqcKeyUnwrapper(@Valid @RequestBody PqcKeyUnwrapperRequest request) {
        return cryptoGatewayService.pqcKeyUnwrapper(request)
                .map(ResponseEntity::ok);
    }
    
    @PostMapping("/encrypt")
    public Mono<ResponseEntity<Result<HybridEncryptResponse>>> hybridEncrypt(@Valid @RequestBody HybridEncryptRequest request) {
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
                                return ResponseEntity.ok(Result.success(response));
                            });
                });
    }
    
    @PostMapping("/decrypt")
    public Mono<ResponseEntity<Result<HybridDecryptResponse>>> hybridDecrypt(@Valid @RequestBody HybridDecryptRequest request) {
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
                                return ResponseEntity.ok(Result.success(response));
                            });
                });
    }
}

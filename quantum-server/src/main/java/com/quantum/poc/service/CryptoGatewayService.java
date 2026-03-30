package com.quantum.poc.service;

import com.quantum.poc.config.EncryptorConfig;
import com.quantum.poc.dto.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class CryptoGatewayService {
    
    private final WebClient encryptorWebClient;
    
    public CryptoGatewayService(WebClient encryptorWebClient, EncryptorConfig encryptorConfig) {
        this.encryptorWebClient = encryptorWebClient;
    }
    
    public Mono<Result<String>> genRandom(Integer length) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/genRandom?length=" + length)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Result<String>>() {});
    }
    
    public Mono<Result<String>> sm4Encrypt(EncryptRequest request) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/symAlgEnc")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Result<String>>() {});
    }
    
    public Mono<Result<String>> sm4Decrypt(EncryptRequest request) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/symAlgDec")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Result<String>>() {});
    }
    
    public Mono<Result<String>> hash(HashRequest request) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/hash")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Result<String>>() {});
    }
    
    public Mono<Result<String>> hmac(HMacRequest request) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/hmac")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Result<String>>() {});
    }
    
    public Mono<Result<Map>> genEccKeyPair() {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/genEccKeyPair")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Result<Map>>() {});
    }
    
    public Mono<Result<String>> sm2Encrypt(Sm2Request request) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/sm2Enc")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Result<String>>() {});
    }
    
    public Mono<Result<String>> sm2Decrypt(Sm2Request request) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/sm2Dec")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Result<String>>() {});
    }
    
    public Mono<Result<Map>> genPqcKeyPair(KeyPairRequest request) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/genPqcKeyPair")
                .bodyValue(Map.of("algorithm", request.getAlgorithm()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Result<Map>>() {});
    }
    
    public Mono<Result<PqcKeyWrapperResponse>> pqcKeyWrapper(PqcKeyWrapperRequest request) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/pqcKeyWrapper")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Result<PqcKeyWrapperResponse>>() {});
    }
    
    public Mono<Result<String>> pqcKeyUnwrapper(PqcKeyUnwrapperRequest request) {
        return encryptorWebClient.post()
                .uri("/scyh-server/v101/pqcKeyUnWrapper")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Result<String>>() {});
    }
}

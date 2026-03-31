package com.quantum.poc.service;

import com.quantum.poc.config.EncryptorConfig;
import com.quantum.poc.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Consumer;

@Service
public class CryptoGatewayService {

    private static final Logger log = LoggerFactory.getLogger(CryptoGatewayService.class);

    private final WebClient encryptorWebClient;
    private final EncryptorConfig encryptorConfig;

    public CryptoGatewayService(WebClient encryptorWebClient, EncryptorConfig encryptorConfig) {
        this.encryptorWebClient = encryptorWebClient;
        this.encryptorConfig = encryptorConfig;
        log.info("CryptoGatewayService initialized, encryptor URL: {}", encryptorConfig.getBaseUrl());
    }

    public Mono<Result<String>> genRandom(Integer length) {
        log.info("[REQUEST] genRandom, length={}", length);
        return doRequest(
                "/scyh-server/v101/genRandom?length=" + length,
                new ParameterizedTypeReference<Result<String>>() {},
                "genRandom",
                r -> log.info("[RESPONSE] genRandom success, data={}", truncate(r.getData(), 100))
        );
    }

    public Mono<Result<String>> sm4Encrypt(EncryptRequest request) {
        log.info("[REQUEST] sm4Encrypt, algorithm={}", request.getAlgorithm());
        return doRequest(
                "/scyh-server/v101/symAlgEnc",
                request,
                new ParameterizedTypeReference<Result<String>>() {},
                "sm4Encrypt",
                r -> log.info("[RESPONSE] sm4Encrypt success, data={}", truncate(r.getData(), 100))
        );
    }

    public Mono<Result<String>> sm4Decrypt(EncryptRequest request) {
        log.info("[REQUEST] sm4Decrypt, algorithm={}", request.getAlgorithm());
        return doRequest(
                "/scyh-server/v101/symAlgDec",
                request,
                new ParameterizedTypeReference<Result<String>>() {},
                "sm4Decrypt",
                r -> log.info("[RESPONSE] sm4Decrypt success, data={}", truncate(r.getData(), 100))
        );
    }

    public Mono<Result<String>> hash(HashRequest request) {
        log.info("[REQUEST] hash, algorithm={}", request.getAlgorithm());
        return doRequest(
                "/scyh-server/v101/hash",
                request,
                new ParameterizedTypeReference<Result<String>>() {},
                "hash",
                r -> log.info("[RESPONSE] hash success, data={}", r.getData())
        );
    }

    public Mono<Result<String>> hmac(HMacRequest request) {
        log.info("[REQUEST] hmac");
        return doRequest(
                "/scyh-server/v101/hmac",
                request,
                new ParameterizedTypeReference<Result<String>>() {},
                "hmac",
                r -> log.info("[RESPONSE] hmac success, data={}", r.getData())
        );
    }

    public Mono<Result<Map>> genEccKeyPair() {
        log.info("[REQUEST] genEccKeyPair");
        return doRequest(
                "/scyh-server/v101/genEccKeyPair",
                new ParameterizedTypeReference<Result<Map>>() {},
                "genEccKeyPair",
                r -> log.info("[RESPONSE] genEccKeyPair success, keys={}", r.getData())
        );
    }

    public Mono<Result<String>> sm2Encrypt(Sm2Request request) {
        log.info("[REQUEST] sm2Encrypt");
        return doRequest(
                "/scyh-server/v101/sm2Enc",
                request,
                new ParameterizedTypeReference<Result<String>>() {},
                "sm2Encrypt",
                r -> log.info("[RESPONSE] sm2Encrypt success, data={}", truncate(r.getData(), 100))
        );
    }

    public Mono<Result<String>> sm2Decrypt(Sm2Request request) {
        log.info("[REQUEST] sm2Decrypt");
        return doRequest(
                "/scyh-server/v101/sm2Dec",
                request,
                new ParameterizedTypeReference<Result<String>>() {},
                "sm2Decrypt",
                r -> log.info("[RESPONSE] sm2Decrypt success, data={}", truncate(r.getData(), 100))
        );
    }

    public Mono<Result<Map>> genPqcKeyPair(KeyPairRequest request) {
        log.info("[REQUEST] genPqcKeyPair, algorithm={}", request.getAlgorithm());
        return doRequest(
                "/scyh-server/v101/genPqcKeyPair",
                java.util.Map.of("algorithm", request.getAlgorithm()),
                new ParameterizedTypeReference<Result<Map>>() {},
                "genPqcKeyPair",
                r -> log.info("[RESPONSE] genPqcKeyPair success, keys={}", r.getData())
        );
    }

    public Mono<Result<PqcKeyWrapperResponse>> pqcKeyWrapper(PqcKeyWrapperRequest request) {
        log.info("[REQUEST] pqcKeyWrapper, algorithm={}", request.getAlgorithm());
        return doRequest(
                "/scyh-server/v101/pqcKeyWrapper",
                request,
                new ParameterizedTypeReference<Result<PqcKeyWrapperResponse>>() {},
                "pqcKeyWrapper",
                r -> log.info("[RESPONSE] pqcKeyWrapper success, keyCipher={}, keyId={}",
                        r.getData() != null ? truncate(r.getData().getKeyCipher(), 50) : null,
                        r.getData() != null ? r.getData().getKeyId() : null)
        );
    }

    public Mono<Result<String>> pqcKeyUnwrapper(PqcKeyUnwrapperRequest request) {
        log.info("[REQUEST] pqcKeyUnwrapper, algorithm={}", request.getAlgorithm());
        return doRequest(
                "/scyh-server/v101/pqcKeyUnWrapper",
                request,
                new ParameterizedTypeReference<Result<String>>() {},
                "pqcKeyUnwrapper",
                r -> log.info("[RESPONSE] pqcKeyUnwrapper success, data={}", r.getData())
        );
    }

    private <T, R> Mono<R> doRequest(String uri, Object body, ParameterizedTypeReference<R> typeRef,
                                       String operationName, Consumer<R> successHandler) {
        return encryptorWebClient.post()
                .uri(uri)
                .bodyValue(body != null ? body : "")
                .retrieve()
                .bodyToMono(typeRef)
                .doOnError(error -> log.error("[ERROR] {} failed: {}, type={}",
                        operationName, error.getMessage(), error.getClass().getSimpleName()));
    }

    private <R> Mono<R> doRequest(String uri, ParameterizedTypeReference<R> typeRef,
                                   String operationName, Consumer<R> successHandler) {
        return doRequest(uri, null, typeRef, operationName, successHandler);
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}

package com.quantum.gateway.service;

import com.quantum.gateway.dto.DecryptRequest;
import com.quantum.gateway.dto.DecryptResponse;
import com.quantum.gateway.dto.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EncryptorClient {

    private final RestTemplate encryptorRestTemplate;

    @Value("${encryptor.base-url}")
    private String encryptorBaseUrl;

    public Map<String, String> generatePqcKeyPair(String algorithm) {
        String url = encryptorBaseUrl + "/genPqcKeyPair";
        Map<String, String> request = Map.of("algorithm", algorithm);

        ResponseEntity<Result<Map>> response = encryptorRestTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {});

        if (response.getBody() != null && response.getBody().getCode() == 0) {
            return response.getBody().getData();
        }
        throw new RuntimeException("Failed to generate PQC key pair: " +
                (response.getBody() != null ? response.getBody().getMsg() : "null response"));
    }

    public String generateRandom(int length) {
        String url = encryptorBaseUrl + "/genRandom?length=" + length;

        ResponseEntity<Result<String>> response = encryptorRestTemplate.exchange(
                url, HttpMethod.POST, null,
                new ParameterizedTypeReference<>() {});

        if (response.getBody() != null && response.getBody().getCode() == 0) {
            return response.getBody().getData();
        }
        throw new RuntimeException("Failed to generate random bytes: " +
                (response.getBody() != null ? response.getBody().getMsg() : "null response"));
    }

    public DecryptResponse decryptAndVerify(DecryptRequest request) {
        String url = encryptorBaseUrl + "/session/decrypt";

        ResponseEntity<Result<DecryptResponse>> response = encryptorRestTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {});

        if (response.getBody() != null && response.getBody().getCode() == 0) {
            return response.getBody().getData();
        }
        throw new RuntimeException("Failed to decrypt: " +
                (response.getBody() != null ? response.getBody().getMsg() : "null response"));
    }
}

package com.quantum.gateway.service;

import com.quantum.gateway.dto.BusinessDataRequest;
import com.quantum.gateway.dto.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessServerClient {

    private final RestTemplate businessServerRestTemplate;

    @Value("${business-server.base-url}")
    private String businessServerBaseUrl;

    @SuppressWarnings("unchecked")
    public Map<String, Object> receiveData(BusinessDataRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Session-Id", request.getSessionId());
            HttpEntity<BusinessDataRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Result<Map>> response = businessServerRestTemplate.exchange(
                    businessServerBaseUrl + "/api/data/receive",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {});

            if (response.getBody() != null && response.getBody().getCode() == 0) {
                return response.getBody().getData();
            }
            throw new RuntimeException("Business server returned error");
        } catch (Exception e) {
            log.error("Failed to forward data to business server", e);
            throw new RuntimeException("Failed to forward data: " + e.getMessage(), e);
        }
    }
}

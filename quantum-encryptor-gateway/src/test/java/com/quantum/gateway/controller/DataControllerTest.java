package com.quantum.gateway.controller;

import com.quantum.gateway.dto.*;
import com.quantum.gateway.service.BusinessServerClient;
import com.quantum.gateway.service.EncryptorClient;
import com.quantum.gateway.service.ReplayProtectionService;
import com.quantum.gateway.service.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DataController.class)
class DataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SessionManager sessionManager;

    @MockBean
    private ReplayProtectionService replayProtectionService;

    @MockBean
    private EncryptorClient encryptorClient;

    @MockBean
    private BusinessServerClient businessServerClient;

    @Test
    void uploadData_validRequest_returnsSuccess() throws Exception {
        SessionContext session = SessionContext.builder()
                .sessionId("test-session")
                .psk("abcdef1234567890abcdef1234567890")
                .build();
        when(sessionManager.getSession("test-session")).thenReturn(session);
        when(replayProtectionService.validateRequest(anyString(), anyString(),
                anyLong(), anyString(), anyString())).thenReturn(true);
        when(encryptorClient.decryptAndVerify(any(DecryptRequest.class)))
                .thenReturn(DecryptResponse.builder()
                        .plainText("decrypted-hex")
                        .dilithiumVerifyResult(true)
                        .sm2VerifyResult(true)
                        .build());
        when(businessServerClient.receiveData(any(BusinessDataRequest.class)))
                .thenReturn(Map.of("dataId", "data-uuid-123"));

        UploadRequest request = new UploadRequest();
        request.setCipherText("cipher-hex");
        request.setIv("iv-hex");
        request.setDilithiumSignature("dilithium-sig-hex");
        request.setSm2Signature("sm2-sig-hex");

        mockMvc.perform(post("/alsp/v1/data/upload")
                        .header("X-Session-Id", "test-session")
                        .header("X-Nonce", "nonce-hex")
                        .header("X-Timestamp", System.currentTimeMillis())
                        .header("X-HMAC", "hmac-hex")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.dataId").value("data-uuid-123"));
    }

    @Test
    void uploadData_expiredSession_returnsError() throws Exception {
        when(sessionManager.getSession("test-session"))
                .thenThrow(new IllegalStateException("Session expired"));

        UploadRequest request = new UploadRequest();
        request.setCipherText("cipher-hex");
        request.setIv("iv-hex");
        request.setDilithiumSignature("dilithium-sig-hex");
        request.setSm2Signature("sm2-sig-hex");

        mockMvc.perform(post("/alsp/v1/data/upload")
                        .header("X-Session-Id", "test-session")
                        .header("X-Nonce", "nonce-hex")
                        .header("X-Timestamp", System.currentTimeMillis())
                        .header("X-HMAC", "hmac-hex")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(60));
    }

    @Test
    void uploadData_replayDetected_returnsError() throws Exception {
        SessionContext session = SessionContext.builder()
                .sessionId("test-session")
                .psk("abcdef1234567890abcdef1234567890")
                .build();
        when(sessionManager.getSession("test-session")).thenReturn(session);
        when(replayProtectionService.validateRequest(anyString(), anyString(),
                anyLong(), anyString(), anyString())).thenReturn(false);

        UploadRequest request = new UploadRequest();
        request.setCipherText("cipher-hex");
        request.setIv("iv-hex");
        request.setDilithiumSignature("dilithium-sig-hex");
        request.setSm2Signature("sm2-sig-hex");

        mockMvc.perform(post("/alsp/v1/data/upload")
                        .header("X-Session-Id", "test-session")
                        .header("X-Nonce", "nonce-hex")
                        .header("X-Timestamp", System.currentTimeMillis())
                        .header("X-HMAC", "hmac-hex")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(70));
    }

    @Test
    void uploadData_signatureVerificationFails_returnsError() throws Exception {
        SessionContext session = SessionContext.builder()
                .sessionId("test-session")
                .psk("abcdef1234567890abcdef1234567890")
                .build();
        when(sessionManager.getSession("test-session")).thenReturn(session);
        when(replayProtectionService.validateRequest(anyString(), anyString(),
                anyLong(), anyString(), anyString())).thenReturn(true);
        when(encryptorClient.decryptAndVerify(any(DecryptRequest.class)))
                .thenReturn(DecryptResponse.builder()
                        .plainText("decrypted-hex")
                        .dilithiumVerifyResult(false)
                        .sm2VerifyResult(true)
                        .build());

        UploadRequest request = new UploadRequest();
        request.setCipherText("cipher-hex");
        request.setIv("iv-hex");
        request.setDilithiumSignature("dilithium-sig-hex");
        request.setSm2Signature("sm2-sig-hex");

        mockMvc.perform(post("/alsp/v1/data/upload")
                        .header("X-Session-Id", "test-session")
                        .header("X-Nonce", "nonce-hex")
                        .header("X-Timestamp", System.currentTimeMillis())
                        .header("X-HMAC", "hmac-hex")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(80));
    }
}

package com.quantum.gateway.controller;

import com.quantum.gateway.dto.*;
import com.quantum.gateway.service.EncryptorClient;
import com.quantum.gateway.service.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionController.class)
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SessionManager sessionManager;

    @MockBean
    private EncryptorClient encryptorClient;

    @Test
    void initSession_returnsSessionId() throws Exception {
        when(encryptorClient.generatePqcKeyPair("Kyber768"))
                .thenReturn(Map.of("publicKey", "kyber-pub-hex", "privateKey", "kyber-priv-hex"));

        SessionContext session = SessionContext.builder()
                .sessionId("test-session-id")
                .kyberPublicKey("kyber-pub-hex")
                .serverNonce("server-nonce-hex")
                .expiresAt(1712188800L)
                .build();
        when(sessionManager.createSession(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(session);

        SessionInitRequest request = new SessionInitRequest();
        request.setClientNonce("client-nonce-hex");
        request.setKyberAlgorithm("Kyber768");
        request.setDilithiumAlgorithm("Dilithium2");

        mockMvc.perform(post("/alsp/v1/session/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sessionId").value("test-session-id"))
                .andExpect(jsonPath("$.data.kyberPublicKey").value("kyber-pub-hex"));
    }

    @Test
    void resumeSession_returnsNewSessionId() throws Exception {
        SessionContext resumed = SessionContext.builder()
                .sessionId("new-session-id")
                .resumed(true)
                .expiresAt(1712188800L)
                .build();
        when(sessionManager.resumeSession("old-session", "new-nonce")).thenReturn(resumed);

        ResumeRequest request = new ResumeRequest();
        request.setSessionId("old-session");
        request.setClientNonce("new-nonce");
        request.setPskHint("psk-hint");

        mockMvc.perform(post("/alsp/v1/session/resume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sessionId").value("new-session-id"))
                .andExpect(jsonPath("$.data.resumed").value(true));
    }

    @Test
    void initSession_failed_returnsError() throws Exception {
        when(encryptorClient.generatePqcKeyPair("Kyber768"))
                .thenThrow(new RuntimeException("Encryptor unavailable"));

        SessionInitRequest request = new SessionInitRequest();
        request.setClientNonce("client-nonce-hex");
        request.setKyberAlgorithm("Kyber768");
        request.setDilithiumAlgorithm("Dilithium2");

        mockMvc.perform(post("/alsp/v1/session/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }
}

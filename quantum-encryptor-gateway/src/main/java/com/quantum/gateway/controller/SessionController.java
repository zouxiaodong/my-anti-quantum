package com.quantum.gateway.controller;

import com.quantum.gateway.dto.*;
import com.quantum.gateway.service.EncryptorClient;
import com.quantum.gateway.service.SessionManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/alsp/v1/session")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SessionController {

    private final SessionManager sessionManager;
    private final EncryptorClient encryptorClient;

    @PostMapping("/init")
    public ResponseEntity<Result<SessionInitResponse>> initSession(
            @Valid @RequestBody SessionInitRequest request) {
        try {
            Map<String, String> kyberKeys = encryptorClient.generatePqcKeyPair(
                    request.getKyberAlgorithm());

            SessionContext session = sessionManager.createSession(
                    request.getClientNonce(),
                    request.getKyberAlgorithm(),
                    request.getDilithiumAlgorithm(),
                    kyberKeys.get("publicKey"),
                    kyberKeys.get("privateKey")
            );

            SessionInitResponse response = SessionInitResponse.builder()
                    .sessionId(session.getSessionId())
                    .kyberPublicKey(session.getKyberPublicKey())
                    .serverNonce(session.getServerNonce())
                    .expiresAt(session.getExpiresAt())
                    .build();

            return ResponseEntity.ok(Result.success(response));
        } catch (Exception e) {
            log.error("Session init failed", e);
            return ResponseEntity.ok(Result.error(1, "会话初始化失败: " + e.getMessage()));
        }
    }

    @PostMapping("/genKeys")
    public ResponseEntity<Result<SessionKeyResponse>> generateKeys(
            @RequestHeader("X-Session-Id") String sessionId) {
        try {
            Map<String, String> sm2Keys = encryptorClient.generatePqcKeyPair("SM2");

            SessionContext session = sessionManager.getSession(sessionId);
            Map<String, String> dilithiumKeys = encryptorClient.generatePqcKeyPair(
                    session.getDilithiumAlgorithm());

            sessionManager.updateSessionKeys(
                    sessionId,
                    sm2Keys.get("publicKey"),
                    sm2Keys.get("privateKey"),
                    dilithiumKeys.get("publicKey"),
                    dilithiumKeys.get("privateKey")
            );

            SessionKeyResponse response = SessionKeyResponse.builder()
                    .sm2PublicKey(sm2Keys.get("publicKey"))
                    .sm2PrivateKey(sm2Keys.get("privateKey"))
                    .dilithiumPublicKey(dilithiumKeys.get("publicKey"))
                    .dilithiumPrivateKey(dilithiumKeys.get("privateKey"))
                    .build();

            return ResponseEntity.ok(Result.success(response));
        } catch (Exception e) {
            log.error("Key generation failed for session {}", sessionId, e);
            return ResponseEntity.ok(Result.error(1, "密钥生成失败: " + e.getMessage()));
        }
    }

    @PostMapping("/resume")
    public ResponseEntity<Result<ResumeResponse>> resumeSession(
            @Valid @RequestBody ResumeRequest request) {
        try {
            SessionContext resumedSession = sessionManager.resumeSession(
                    request.getSessionId(),
                    request.getClientNonce()
            );

            ResumeResponse response = ResumeResponse.builder()
                    .sessionId(resumedSession.getSessionId())
                    .resumed(true)
                    .expiresAt(resumedSession.getExpiresAt())
                    .build();

            return ResponseEntity.ok(Result.success(response));
        } catch (Exception e) {
            log.error("Session resume failed for {}", request.getSessionId(), e);
            return ResponseEntity.ok(Result.error(1, "会话恢复失败: " + e.getMessage()));
        }
    }
}

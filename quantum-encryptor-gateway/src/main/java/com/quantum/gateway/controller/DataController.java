package com.quantum.gateway.controller;

import com.quantum.gateway.dto.*;
import com.quantum.gateway.service.BusinessServerClient;
import com.quantum.gateway.service.EncryptorClient;
import com.quantum.gateway.service.ReplayProtectionService;
import com.quantum.gateway.service.SessionManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/alsp/v1/data")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DataController {

    private final SessionManager sessionManager;
    private final ReplayProtectionService replayProtectionService;
    private final EncryptorClient encryptorClient;
    private final BusinessServerClient businessServerClient;

    @PostMapping("/upload")
    public ResponseEntity<Result<UploadResponse>> uploadData(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestHeader("X-Nonce") String nonce,
            @RequestHeader("X-Timestamp") long timestamp,
            @RequestHeader("X-HMAC") String hmac,
            @Valid @RequestBody UploadRequest request) {
        try {
            SessionContext session = sessionManager.getSession(sessionId);

            if (!replayProtectionService.validateRequest(
                    sessionId, nonce, timestamp, hmac, session.getPsk())) {
                return ResponseEntity.ok(Result.error(70, "Replay detected or validation failed"));
            }

            DecryptRequest decryptRequest = DecryptRequest.builder()
                    .sessionId(sessionId)
                    .cipherText(request.getCipherText())
                    .dilithiumSignature(request.getDilithiumSignature())
                    .sm2Signature(request.getSm2Signature())
                    .iv(request.getIv())
                    .build();

            DecryptResponse decryptResponse = encryptorClient.decryptAndVerify(decryptRequest);

            if (!decryptResponse.isDilithiumVerifyResult() || !decryptResponse.isSm2VerifyResult()) {
                return ResponseEntity.ok(Result.error(80,
                        "Signature verification failed: Dilithium=" +
                                decryptResponse.isDilithiumVerifyResult() +
                                ", SM2=" + decryptResponse.isSm2VerifyResult()));
            }

            BusinessDataRequest businessRequest = BusinessDataRequest.builder()
                    .plainText(decryptResponse.getPlainText())
                    .sessionId(sessionId)
                    .dilithiumVerifyResult(decryptResponse.isDilithiumVerifyResult())
                    .sm2VerifyResult(decryptResponse.isSm2VerifyResult())
                    .timestamp(System.currentTimeMillis())
                    .build();

            Map<String, Object> businessResponse = businessServerClient.receiveData(businessRequest);

            UploadResponse response = UploadResponse.builder()
                    .success(true)
                    .dataId((String) businessResponse.get("dataId"))
                    .build();

            return ResponseEntity.ok(Result.success(response));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(Result.error(60, "Session expired or not found"));
        } catch (Exception e) {
            log.error("Data upload failed for session {}", sessionId, e);
            return ResponseEntity.ok(Result.error(1, "数据上传失败: " + e.getMessage()));
        }
    }
}

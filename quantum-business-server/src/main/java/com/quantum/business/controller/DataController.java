package com.quantum.business.controller;

import com.quantum.business.dto.*;
import com.quantum.business.service.DataStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DataController {

    private final DataStorageService storageService;

    @PostMapping("/receive")
    public ResponseEntity<Result<DataReceiveResponse>> receiveData(
            @RequestHeader("X-Session-Id") String sessionId,
            @Valid @RequestBody DataReceiveRequest request) {
        try {
            String dataId = storageService.storeData(
                    request.getPlainText(),
                    request.getSessionId(),
                    request.isDilithiumVerifyResult(),
                    request.isSm2VerifyResult()
            );

            DataReceiveResponse response = DataReceiveResponse.builder()
                    .success(true)
                    .dataId(dataId)
                    .build();

            return ResponseEntity.ok(Result.success(response));
        } catch (Exception e) {
            log.error("Failed to receive data", e);
            return ResponseEntity.ok(Result.error(1, "数据接收失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{dataId}")
    public ResponseEntity<Result<DataQueryResponse>> queryData(
            @PathVariable String dataId) {
        StoredData data = storageService.getData(dataId);
        if (data == null) {
            return ResponseEntity.ok(Result.error(1, "数据不存在"));
        }

        String status = (data.isDilithiumVerified() && data.isSm2Verified())
                ? "verified" : "partial";

        DataQueryResponse response = DataQueryResponse.builder()
                .dataId(data.getDataId())
                .plainText(data.getPlainText())
                .sessionId(data.getSessionId())
                .timestamp(data.getReceivedAt())
                .status(status)
                .build();

        return ResponseEntity.ok(Result.success(response));
    }
}

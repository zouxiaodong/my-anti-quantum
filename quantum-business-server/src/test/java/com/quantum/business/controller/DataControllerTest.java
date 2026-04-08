package com.quantum.business.controller;

import com.quantum.business.dto.DataReceiveRequest;
import com.quantum.business.dto.StoredData;
import com.quantum.business.service.DataStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DataController.class)
class DataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DataStorageService storageService;

    @Test
    void receiveData_validRequest_returnsSuccess() throws Exception {
        when(storageService.storeData(anyString(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn("data-uuid-123");

        DataReceiveRequest request = new DataReceiveRequest();
        request.setPlainText("plaintext-hex");
        request.setSessionId("session-123");
        request.setDilithiumVerifyResult(true);
        request.setSm2VerifyResult(true);
        request.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/api/data/receive")
                        .header("X-Session-Id", "session-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.dataId").value("data-uuid-123"));
    }

    @Test
    void queryData_existingId_returnsData() throws Exception {
        when(storageService.getData("data-uuid-123"))
                .thenReturn(StoredData.builder()
                        .dataId("data-uuid-123")
                        .plainText("plaintext-hex")
                        .sessionId("session-123")
                        .dilithiumVerified(true)
                        .sm2Verified(true)
                        .receivedAt(1712102400L)
                        .build());

        mockMvc.perform(get("/api/data/data-uuid-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.dataId").value("data-uuid-123"))
                .andExpect(jsonPath("$.data.plainText").value("plaintext-hex"))
                .andExpect(jsonPath("$.data.status").value("verified"));
    }

    @Test
    void queryData_nonExistentId_returnsError() throws Exception {
        when(storageService.getData("non-existent")).thenReturn(null);

        mockMvc.perform(get("/api/data/non-existent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }
}

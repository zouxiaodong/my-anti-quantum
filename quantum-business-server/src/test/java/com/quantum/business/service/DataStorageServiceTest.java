package com.quantum.business.service;

import com.quantum.business.dto.StoredData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataStorageServiceTest {

    private DataStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new DataStorageService();
    }

    @Test
    void storeData_returnsDataId() {
        String dataId = storageService.storeData(
                "plaintext-hex", "session-123", true, true);

        assertNotNull(dataId);
    }

    @Test
    void getData_returnsStoredData() {
        String dataId = storageService.storeData(
                "plaintext-hex", "session-123", true, true);

        StoredData data = storageService.getData(dataId);

        assertNotNull(data);
        assertEquals("plaintext-hex", data.getPlainText());
        assertEquals("session-123", data.getSessionId());
        assertTrue(data.isDilithiumVerified());
        assertTrue(data.isSm2Verified());
    }

    @Test
    void getData_returnsNullForNonExistentId() {
        StoredData data = storageService.getData("non-existent");
        assertNull(data);
    }

    @Test
    void getAllData_returnsAllStoredData() {
        storageService.storeData("data1", "session-1", true, true);
        storageService.storeData("data2", "session-2", false, true);

        assertEquals(2, storageService.getAllData().size());
    }
}

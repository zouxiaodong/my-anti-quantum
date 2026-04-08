package com.quantum.business.service;

import com.quantum.business.dto.StoredData;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DataStorageService {

    private final Map<String, StoredData> dataStore = new ConcurrentHashMap<>();

    public String storeData(String plainText, String sessionId,
                            boolean dilithiumVerified, boolean sm2Verified) {
        String dataId = UUID.randomUUID().toString();

        StoredData storedData = StoredData.builder()
                .dataId(dataId)
                .plainText(plainText)
                .sessionId(sessionId)
                .dilithiumVerified(dilithiumVerified)
                .sm2Verified(sm2Verified)
                .receivedAt(System.currentTimeMillis())
                .build();

        dataStore.put(dataId, storedData);
        return dataId;
    }

    public StoredData getData(String dataId) {
        return dataStore.get(dataId);
    }

    public List<StoredData> getAllData() {
        return new ArrayList<>(dataStore.values());
    }
}

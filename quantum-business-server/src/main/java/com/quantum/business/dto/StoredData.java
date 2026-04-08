package com.quantum.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredData {
    private String dataId;
    private String plainText;
    private String sessionId;
    private boolean dilithiumVerified;
    private boolean sm2Verified;
    private long receivedAt;
}

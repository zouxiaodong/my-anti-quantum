package com.quantum.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQueryResponse {
    private String dataId;
    private String plainText;
    private String sessionId;
    private long timestamp;
    private String status;
}

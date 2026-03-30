package com.quantum.mock.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PqcService {

    public Map<String, String> generateKeyPair(String algorithm) throws Exception {
        throw new UnsupportedOperationException(
            "PQC algorithms not yet implemented in mock. " +
            "Use SM2/SM3/SM4 for testing, or configure real encryption machine for PQC."
        );
    }

    public Map<String, String> keyWrapper(String algorithm, String pqcPubkey) throws Exception {
        throw new UnsupportedOperationException(
            "PQC key wrapping not yet implemented in mock. " +
            "Use SM2/SM3/SM4 for testing, or configure real encryption machine for PQC."
        );
    }

    public String keyUnwrapper(String algorithm, String cipherText, String pqcPrikey) throws Exception {
        throw new UnsupportedOperationException(
            "PQC key unwrapping not yet implemented in mock. " +
            "Use SM2/SM3/SM4 for testing, or configure real encryption machine for PQC."
        );
    }
}

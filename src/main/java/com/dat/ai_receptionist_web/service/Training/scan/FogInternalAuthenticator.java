package com.dat.ai_receptionist_web.service.Training.scan;

import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class FogInternalAuthenticator {
    private final byte[] expectedSecret;

    public FogInternalAuthenticator(@Value("${app.fog.internal-secret:}") String secret) {
        this.expectedSecret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public void requireValid(String suppliedSecret) {
        byte[] supplied = suppliedSecret == null
                ? new byte[0]
                : suppliedSecret.getBytes(StandardCharsets.UTF_8);
        if (expectedSecret.length == 0 || !MessageDigest.isEqual(expectedSecret, supplied)) {
            throw new ApiException(SecurityErrorCode.UNAUTHORIZED);
        }
    }
}

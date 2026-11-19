package com.dat.ai_receptionist_web.error.code;

import com.dat.ai_receptionist_web.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum CrmErrorCode implements ErrorCode {
    LEAD_NOT_FOUND("LEAD_NOT_FOUND", HttpStatus.NOT_FOUND,
            "Lead not found", "Lead not found"),
    LEAD_IN_USE("LEAD_IN_USE", HttpStatus.CONFLICT,
            "Lead is in use", "Lead has trial registrations and cannot be deleted"),
    TRIAL_REGISTRATION_NOT_FOUND("TRIAL_REGISTRATION_NOT_FOUND", HttpStatus.NOT_FOUND,
            "Trial registration not found", "Trial registration not found");

    private final String code;
    private final HttpStatus status;
    private final String title;
    private final String defaultDetail;

    @Override
    public String code() {
        return code;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public String defaultDetail() {
        return defaultDetail;
    }
}

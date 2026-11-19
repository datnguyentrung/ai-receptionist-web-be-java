package com.dat.ai_receptionist_web.dto.Crm;

import com.dat.ai_receptionist_web.enums.Crm.TrialRegistrationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public final class TrialRegistrationDTO {
    private TrialRegistrationDTO() {
    }

    public record CreateRequest(
            @NotNull UUID leadId,
            UUID classSessionId,
            UUID coachPersonId,
            @NotNull TrialRegistrationStatus status,
            LocalDateTime checkedInAt,
            @Size(max = 1000) String coachFeedback,
            @Size(max = 1000) String outcomeNote
    ) {
    }

    public record UpdateRequest(
            UUID classSessionId,
            UUID coachPersonId,
            @NotNull TrialRegistrationStatus status,
            LocalDateTime checkedInAt,
            @Size(max = 1000) String coachFeedback,
            @Size(max = 1000) String outcomeNote
    ) {
    }

    public record SimpleResponse(
            UUID trialRegistrationId,
            UUID leadId,
            String leadFullName,
            UUID classSessionId,
            TrialRegistrationStatus status
    ) {
    }

    public record Response(
            UUID trialRegistrationId,
            UUID leadId,
            String leadFullName,
            UUID classSessionId,
            UUID coachPersonId,
            String coachPersonName,
            TrialRegistrationStatus status,
            LocalDateTime checkedInAt,
            String coachFeedback,
            String outcomeNote,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}

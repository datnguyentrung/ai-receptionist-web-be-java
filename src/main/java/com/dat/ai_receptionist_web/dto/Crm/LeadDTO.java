package com.dat.ai_receptionist_web.dto.Crm;

import com.dat.ai_receptionist_web.enums.Crm.LeadStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public final class LeadDTO {
    private LeadDTO() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Size(max = 20) String phoneNumber,
            @Email @Size(max = 100) String email,
            @NotNull @PastOrPresent LocalDate birthDate,
            @NotNull LeadStatus status,
            UUID assignedUserId,
            LocalDateTime nextFollowUpAt,
            LocalDateTime lastContactedAt,
            @Size(max = 1000) String note,
            UUID convertedPersonId,
            LocalDateTime convertedAt
    ) {
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Size(max = 20) String phoneNumber,
            @Email @Size(max = 100) String email,
            @NotNull @PastOrPresent LocalDate birthDate,
            @NotNull LeadStatus status,
            UUID assignedUserId,
            LocalDateTime nextFollowUpAt,
            LocalDateTime lastContactedAt,
            @Size(max = 1000) String note,
            UUID convertedPersonId,
            LocalDateTime convertedAt
    ) {
    }

    public record SimpleResponse(
            UUID leadId,
            String fullName,
            String phoneNumber,
            LeadStatus status,
            UUID assignedUserId,
            LocalDateTime nextFollowUpAt,
            LocalDateTime createdAt
    ) {
    }

    public record Response(
            UUID leadId,
            String fullName,
            String phoneNumber,
            String email,
            LocalDate birthDate,
            LeadStatus status,
            UUID assignedUserId,
            LocalDateTime nextFollowUpAt,
            LocalDateTime lastContactedAt,
            String note,
            UUID convertedPersonId,
            LocalDateTime convertedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}

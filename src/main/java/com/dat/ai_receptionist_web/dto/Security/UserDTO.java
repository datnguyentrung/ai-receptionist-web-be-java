package com.dat.ai_receptionist_web.dto.Security;

import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.dat.ai_receptionist_web.enums.Security.UserStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public final class UserDTO {
    private UserDTO() {
    }

    public record CreateRequest(
            @NotBlank String phoneNumber,
            @NotBlank String passwordHash,

            UUID personId,

            @Valid
            PersonDTO.CreateRequest person,

            @NotNull RelationshipType relationshipType
    ) {

        @AssertTrue(message = "Phải cung cấp chính xác một trong hai: personId hoặc person")
        public boolean isPersonReferenceValid() {
            return (personId != null) ^ (person != null);
        }
    }

    public record UpdateRequest(
            @NotNull String phoneNumber,
            @NotNull String passwordHash,
            @NotNull UserStatus status,
            @NotNull LocalDateTime lastLoginAt,
            @NotNull long authorizationVersion
    ) {
    }

    public record Response(
            UUID userId,
            String phoneNumber,
            String passwordHash,
            UserStatus status,
            long authorizationVersion,
            LocalDateTime lastLoginAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record SimpleResponse(
            UUID userId,
            String phoneNumber,
            UserStatus status,
            LocalDateTime lastLoginAt
    ) {
    }
}

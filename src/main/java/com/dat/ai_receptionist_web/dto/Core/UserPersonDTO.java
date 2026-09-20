package com.dat.ai_receptionist_web.dto.Core;

import com.dat.ai_receptionist_web.dto.Security.UserDTO;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import java.util.UUID;

public final class UserPersonDTO {
    private UserPersonDTO() {
    }

    public record CreateRequest(
            @NotNull UUID userId,
            @NotNull UUID personId,
            @NotNull RelationshipType relationshipType,
            boolean active
    ) {
    }

    public record UpdateRequest(
            @NotNull UUID userId,
            @NotNull UUID personId,
            @NotNull RelationshipType relationshipType,
            boolean active
    ) {
    }

    public record Response(
            UUID userPersonId,
            UserDTO.SimpleResponse user,
            PersonDTO.Response person,
            RelationshipType relationshipType,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record SimpleResponse(
            UUID userPersonId,
            UserDTO.SimpleResponse user,
            PersonDTO.SimpleResponse person,
            RelationshipType relationshipType,
            boolean active
    ) {
    }
}

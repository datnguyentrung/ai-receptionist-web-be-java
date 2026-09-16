package com.dat.ai_receptionist_web.dto.Training;

import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.dto.Security.UserDTO;
import com.dat.ai_receptionist_web.enums.Training.LeaveRequestStatus;
import com.dat.ai_receptionist_web.enums.Training.RequesterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public final class LeaveRequestDTO {
    private LeaveRequestDTO() {
    }

    public record CreateRequest(
            @NotNull UUID personId,
            @NotNull RequesterType requesterType,
            LocalDate leaveDate,
            UUID leaveClassSessionId,
            UUID makeupClassSessionId,
            @NotBlank @Size(max = 1000) String leaveContext
    ) {
    }

    public record ReviewCommand(
            @Size(max = 1000) String reviewNote
    ) {
    }

    public record Response(
            UUID leaveRequestId,
            PersonDTO.Response person,
            RequesterType requesterType,
            LocalDate leaveDate,
            ClassSessionDTO.Response leaveClassSession,
            ClassSessionDTO.Response makeupClassSession,
            String leaveContext,
            LeaveRequestStatus status,
            UserDTO.SimpleResponse createdByUser,
            UserDTO.SimpleResponse reviewedByUser,
            LocalDateTime reviewedAt,
            String reviewNote,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record SimpleResponse(
            UUID leaveRequestId,
            PersonDTO.SimpleResponse person,
            RequesterType requesterType,
            LocalDate leaveDate,
            ClassSessionDTO.SimpleResponse leaveClassSession,
            ClassSessionDTO.SimpleResponse makeupClassSession,
            LeaveRequestStatus status,
            LocalDateTime reviewedAt
    ) {
    }
}

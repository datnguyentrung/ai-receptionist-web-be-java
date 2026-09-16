package com.dat.ai_receptionist_web.dto.Skill;

import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public final class FitnessRecordDTO {
    private FitnessRecordDTO() {
    }

    public record CreateRequest(@NotNull UUID studentId, @NotNull Long fitnessId, @NotNull UUID recordedByCoachId, @NotNull LocalDate recordDate, int duration) {
    }

    public record UpdateRequest(@NotNull UUID studentId, @NotNull Long fitnessId, @NotNull UUID recordedByCoachId, @NotNull LocalDate recordDate, int duration) {
    }

    public record Response(Long fitnessRecordId, PersonDTO.Response student, FitnessDTO.Response fitness, PersonDTO.Response recordedByCoach, LocalDate recordDate, int duration, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record SimpleResponse(Long fitnessRecordId, PersonDTO.SimpleResponse student, FitnessDTO.SimpleResponse fitness, PersonDTO.SimpleResponse recordedByCoach, LocalDate recordDate, int duration) {
    }
}

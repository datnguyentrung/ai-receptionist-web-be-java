package com.dat.ai_receptionist_web.dto.Training;

import com.dat.ai_receptionist_web.dto.Catalog.CourseDTO;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.enums.Training.AssignmentType;
import com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public final class CourseStaffAssignmentDTO {
    private CourseStaffAssignmentDTO() {
    }

    public record CreateRequest(
            @NotNull UUID staffPersonId,
            @NotNull UUID courseId,
            @NotNull AssignmentType assignmentType,
            @NotNull LocalDate startDate,
            LocalDate endDate,
            @NotNull CourseStaffAssignmentStatus assignmentStatus,
            @NotNull String note
    ) {
    }

    public record UpdateRequest(
            @NotNull UUID staffPersonId,
            @NotNull UUID courseId,
            @NotNull AssignmentType assignmentType,
            @NotNull LocalDate startDate,
            LocalDate endDate,
            @NotNull CourseStaffAssignmentStatus assignmentStatus,
            @NotNull String note
    ) {
    }

    public record Response(
            UUID courseStaffAssignmentId,
            PersonDTO.Response staffPerson,
            CourseDTO.Response course,
            AssignmentType assignmentType,
            LocalDate startDate,
            LocalDate endDate,
            CourseStaffAssignmentStatus assignmentStatus,
            String note,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record SimpleResponse(
            UUID courseStaffAssignmentId,
            PersonDTO.SimpleResponse staffPerson,
            CourseDTO.SimpleResponse course,
            AssignmentType assignmentType,
            LocalDate startDate,
            LocalDate endDate,
            CourseStaffAssignmentStatus assignmentStatus
    ) {
    }
}

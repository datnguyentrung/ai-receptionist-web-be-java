package com.dat.ai_receptionist_web.dto.Core;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

import com.dat.ai_receptionist_web.enums.Core.BranchStatus;

import java.time.LocalDate;

public final class BranchDTO {
    private BranchDTO() {
    }

    public record CreateRequest(
            @NotNull
            String name,
            @NotNull
            String address,
            @NotNull
            String hotline,
            @NotNull
            LocalDate openedDate,
            @NotNull
            BranchStatus status
    ) {
    }

    public record UpdateRequest(
            @NotNull
            String name,
            @NotNull
            String address,
            @NotNull
            String hotline,
            @NotNull
            LocalDate openedDate,
            @NotNull
            BranchStatus status
    ) {
    }

    public record Response(
            Long branchId,
            String name,
            String address,
            String hotline,
            LocalDate openedDate,
            BranchStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record SimpleResponse(
            Long branchId,
            String name,
            String address,
            String hotline,
            BranchStatus status
    ) {
    }
}

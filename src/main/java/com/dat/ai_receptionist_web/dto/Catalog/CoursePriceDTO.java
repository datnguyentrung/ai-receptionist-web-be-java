package com.dat.ai_receptionist_web.dto.Catalog;

import jakarta.validation.constraints.*;
import com.dat.ai_receptionist_web.enums.Catalog.CoursePriceStatus;
import java.math.BigDecimal;
import java.util.UUID;

public final class CoursePriceDTO {
    private CoursePriceDTO() {
    }

    public record CreateRequest(
            @NotNull(message = "Course ID is required")
            UUID courseId,

            int durationMonths,

            int sessionCount,

            @NotNull(message = "Base price is required")
            BigDecimal basePrice,

            @NotNull(message = "Final price is required")
            BigDecimal finalPrice,

            @NotNull(message = "Status is required")
            CoursePriceStatus status
    ) {
    }

    public record UpdateRequest(
            @NotNull(message = "Course ID is required")
            UUID courseId,

            int durationMonths,

            int sessionCount,

            @NotNull(message = "Base price is required")
            BigDecimal basePrice,

            @NotNull(message = "Final price is required")
            BigDecimal finalPrice,

            @NotNull(message = "Status is required")
            CoursePriceStatus status
    ) {
    }

    public record Response(
            UUID coursePriceId,
            CourseDTO.Response course,
            int durationMonths,
            int sessionCount,
            BigDecimal basePrice,
            BigDecimal finalPrice,
            CoursePriceStatus status
    ) {
    }

    public record SimpleResponse(
            UUID coursePriceId,
            CourseDTO.SimpleResponse course,
            int durationMonths,
            int sessionCount,
            BigDecimal finalPrice,
            CoursePriceStatus status
    ) {
    }
}

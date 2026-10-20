package com.dat.ai_receptionist_web.dto.Training;

import com.dat.ai_receptionist_web.enums.Training.BookingSource;
import com.dat.ai_receptionist_web.enums.Training.BookingStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public final class StudentSessionBookingDTO {
    private StudentSessionBookingDTO() {}

    public record CreateRequest(@NotNull UUID classSessionId) {}

    public record Response(
            UUID bookingId,
            UUID enrollmentId,
            UUID classSessionId,
            BookingSource source,
            BookingStatus status,
            LocalDateTime bookedAt,
            LocalDateTime cancelledAt,
            LocalDateTime settledAt
    ) {}
}

package com.dat.ai_receptionist_web.dto.Training;

import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLocation;
import com.dat.ai_receptionist_web.enums.Core.Weekday;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record SessionAttendanceFilter(
        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,
        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to,
        UUID courseId,
        UUID studentPersonId,
        UUID staffPersonId,
        Long branchId,
        Weekday weekday,
        ScheduleLevel scheduleLevel,
        ScheduleLocation location
) {
    private static final long MAX_RANGE_DAYS = 93;

    @AssertTrue(message = "from must be before or equal to to")
    public boolean isRangeOrdered() {
        return from == null || to == null || !from.isAfter(to);
    }

    @AssertTrue(message = "Date range must not exceed 93 days")
    public boolean isRangeWithinLimit() {
        return from == null || to == null || from.isAfter(to)
                || ChronoUnit.DAYS.between(from, to) <= MAX_RANGE_DAYS;
    }
}

package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.dto.Training.SessionAttendanceFilter;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SessionAttendanceFilterTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsRangeAtMaximumLength() {
        LocalDate from = LocalDate.of(2026, 1, 1);

        assertThat(validator.validate(filter(from, from.plusDays(93)))).isEmpty();
    }

    @Test
    void rejectsMissingDates() {
        assertThat(validator.validate(filter(null, null)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("from", "to");
    }

    @Test
    void rejectsInvertedRange() {
        assertThat(validator.validate(filter(
                LocalDate.of(2026, 2, 2), LocalDate.of(2026, 2, 1))))
                .extracting(violation -> violation.getMessage())
                .contains("from must be before or equal to to");
    }

    @Test
    void rejectsRangeOverMaximumLength() {
        LocalDate from = LocalDate.of(2026, 1, 1);

        assertThat(validator.validate(filter(from, from.plusDays(94))))
                .extracting(violation -> violation.getMessage())
                .contains("Date range must not exceed 93 days");
    }

    private static SessionAttendanceFilter filter(LocalDate from, LocalDate to) {
        return new SessionAttendanceFilter(
                from, to, null, null, null, null, null, null, null);
    }
}

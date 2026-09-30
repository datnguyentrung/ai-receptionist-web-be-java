package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Catalog.ClassSchedule;
import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.service.Training.scheduling.CourseScheduleResolver;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CourseScheduleResolverTest {
    private final CourseScheduleResolver resolver = new CourseScheduleResolver();

    @Test
    void resolvesPendingScheduleOnAndAfterEffectiveDate() {
        ClassSchedule current = new ClassSchedule();
        ClassSchedule pending = new ClassSchedule();
        LocalDate effectiveDate = LocalDate.of(2026, 10, 15);
        Course course = Course.builder()
                .classSchedule(current)
                .nextClassSchedule(pending)
                .nextScheduleEffectiveFrom(effectiveDate)
                .build();

        assertThat(resolver.resolve(course, effectiveDate.minusDays(1))).isSameAs(current);
        assertThat(resolver.resolve(course, effectiveDate)).isSameAs(pending);
        assertThat(resolver.resolve(course, effectiveDate.plusDays(1))).isSameAs(pending);
    }

    @Test
    void resolvesCurrentScheduleWithoutPendingChange() {
        ClassSchedule current = new ClassSchedule();
        Course course = Course.builder().classSchedule(current).build();

        assertThat(resolver.resolve(course, LocalDate.of(2026, 10, 15))).isSameAs(current);
    }
}

package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Catalog.ClassSchedule;
import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.service.Training.scheduling.CourseScheduleResolver;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourseScheduleResolverTest {
    private final CourseScheduleResolver resolver = new CourseScheduleResolver();

    @Test
    void resolvesCurrentScheduleFromCourseSchedules() {
        ClassSchedule current = new ClassSchedule();
        CourseSchedule courseSchedule = CourseSchedule.builder()
                .classSchedule(current)
                .startDate(LocalDate.of(2026, 1, 1))
                .build();
        Course course = Course.builder()
                .courseSchedules(List.of(courseSchedule))
                .build();

        assertThat(resolver.resolve(course, LocalDate.of(2026, 10, 15))).isSameAs(current);
    }
}

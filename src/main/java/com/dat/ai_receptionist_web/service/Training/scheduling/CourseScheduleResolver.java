package com.dat.ai_receptionist_web.service.Training.scheduling;

import com.dat.ai_receptionist_web.domain.Catalog.ClassSchedule;
import com.dat.ai_receptionist_web.domain.Catalog.Course;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class CourseScheduleResolver {
    public ClassSchedule resolve(Course course, LocalDate sessionDate) {
        if (course.getNextClassSchedule() != null
                && course.getNextScheduleEffectiveFrom() != null
                && !sessionDate.isBefore(course.getNextScheduleEffectiveFrom())) {
            return course.getNextClassSchedule();
        }
        return course.getClassSchedule();
    }
}

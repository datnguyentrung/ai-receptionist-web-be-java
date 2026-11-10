package com.dat.ai_receptionist_web.repository.Training;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SessionAttendanceQueryContractTest {
    @Test
    void attendanceCreateQueriesOnlyFetchRequiredGraph() throws Exception {
        Query classSessionQuery = ClassSessionRepository.class
                .getMethod("findDetailedById", UUID.class)
                .getAnnotation(Query.class);
        Query enrollmentQuery = StudentEnrollmentRepository.class
                .getMethod("findDetailedForAttendanceById", UUID.class)
                .getAnnotation(Query.class);
        Query assignmentQuery = CourseStaffAssignmentRepository.class
                .getMethod("findDetailedForAttendanceById", UUID.class)
                .getAnnotation(Query.class);

        assertThat(classSessionQuery).isNotNull();
        assertThat(classSessionQuery.value())
                .contains("join fetch c.course")
                .contains("join fetch c.courseSchedule");

        assertThat(enrollmentQuery).isNotNull();
        assertThat(enrollmentQuery.value())
                .contains("select distinct e")
                .contains("join fetch e.studentPerson studentPerson")
                .contains("left join fetch studentPerson.position")
                .contains("left join fetch e.schedules link")
                .contains("left join fetch link.courseSchedule courseSchedule");

        assertThat(assignmentQuery).isNotNull();
        assertThat(assignmentQuery.value())
                .contains("join fetch a.staffPerson staffPerson")
                .contains("left join fetch staffPerson.position")
                .contains("join fetch a.courseSchedule courseSchedule");
    }

    @Test
    void evaluationUpdateQueryFetchesOnlyAttendanceValidationGraph() throws Exception {
        Query query = SessionAttendanceRepository.class
                .getMethod("findEvaluationUpdateById", UUID.class)
                .getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.value())
                .contains("select distinct a")
                .contains("join fetch a.classSession session")
                .contains("join fetch session.courseSchedule sessionSchedule")
                .contains("left join fetch a.studentEnrollment enrollment")
                .contains("left join fetch enrollment.schedules enrollmentScheduleLink")
                .contains("left join fetch a.courseStaffAssignment assignment")
                .doesNotContain("studentPerson")
                .doesNotContain("classSchedule")
                .doesNotContain("branch")
                .doesNotContain("coursePurchase");
    }
}

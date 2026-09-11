package com.dat.ai_receptionist_web.repository.Training;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingPolicyRepositoryContractTest {
    @Test
    void studentAttendanceAccessibleQueriesUseDbScopedPolicyPaths() throws Exception {
        Query listQuery = StudentAttendanceRepository.class
                .getMethod(
                        "findAccessible",
                        UUID.class,
                        UUID.class,
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        LocalDate.class,
                        LocalDate.class,
                        UUID.class,
                        UUID.class,
                        Pageable.class
                )
                .getAnnotation(Query.class);
        Query detailQuery = StudentAttendanceRepository.class
                .getMethod(
                        "findAccessibleById",
                        UUID.class,
                        UUID.class,
                        UUID.class,
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        boolean.class
                )
                .getAnnotation(Query.class);

        assertScopedAttendanceQuery(listQuery.value());
        assertScopedAttendanceQuery(detailQuery.value());
    }

    @Test
    void studentEnrollmentAccessibleQueriesUseGuardianAndHistoricalAssignmentScope() throws Exception {
        Query listQuery = StudentEnrollmentRepository.class
                .getMethod(
                        "findAccessible",
                        UUID.class,
                        UUID.class,
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        LocalDate.class,
                        LocalDate.class,
                        UUID.class,
                        UUID.class,
                        Pageable.class
                )
                .getAnnotation(Query.class);
        Query detailQuery = StudentEnrollmentRepository.class
                .getMethod(
                        "findAccessibleById",
                        UUID.class,
                        UUID.class,
                        UUID.class,
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        boolean.class
                )
                .getAnnotation(Query.class);

        assertThat(listQuery.value())
                .contains("UserPerson up", "RelationshipType.GUARDIAN", "up.active = true")
                .contains("csa.startDate <= :toDate", "csa.endDate is null or csa.endDate >= :fromDate")
                .contains("CourseStaffAssignmentStatus.ACTIVE", "CourseStaffAssignmentStatus.ENDED")
                .doesNotContain("status != CANCELLED", "stream().filter");
        assertThat(detailQuery.value())
                .contains("UserPerson up", "RelationshipType.GUARDIAN")
                .contains("csa.startDate <= e.endDate", "csa.endDate is null or csa.endDate >= e.startDate")
                .contains("CourseStaffAssignmentStatus.ACTIVE", "CourseStaffAssignmentStatus.ENDED")
                .doesNotContain("status != CANCELLED", "stream().filter");
    }

    @Test
    void coachTimesheetAccessibleQueriesSupportSelfAndManagedCourseScope() throws Exception {
        Query listQuery = CoachTimesheetRepository.class
                .getMethod(
                        "findAccessible",
                        UUID.class,
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        LocalDate.class,
                        LocalDate.class,
                        UUID.class,
                        Pageable.class
                )
                .getAnnotation(Query.class);
        Query detailQuery = CoachTimesheetRepository.class
                .getMethod(
                        "findAccessibleById",
                        UUID.class,
                        UUID.class,
                        boolean.class,
                        boolean.class,
                        boolean.class
                )
                .getAnnotation(Query.class);

        assertScopedTimesheetQuery(listQuery.value());
        assertScopedTimesheetQuery(detailQuery.value());
    }

    @Test
    void classSessionAccessibleQueriesUseHistoricalAssignmentScope() throws Exception {
        Query listQuery = ClassSessionRepository.class
                .getMethod("findAccessible", UUID.class, boolean.class, Pageable.class)
                .getAnnotation(Query.class);
        Query detailQuery = ClassSessionRepository.class
                .getMethod("findAccessibleById", UUID.class, UUID.class, boolean.class)
                .getAnnotation(Query.class);

        assertThat(listQuery.value())
                .contains("CourseStaffAssignment a")
                .contains("a.startDate <= c.sessionDate", "a.endDate is null or a.endDate >= c.sessionDate")
                .contains("CourseStaffAssignmentStatus.ACTIVE", "CourseStaffAssignmentStatus.ENDED")
                .doesNotContain("status != CANCELLED", "status <> CANCELLED", "stream().filter");
        assertThat(detailQuery.value())
                .contains("c.classSessionId = :id")
                .contains("a.startDate <= c.sessionDate", "a.endDate is null or a.endDate >= c.sessionDate")
                .contains("CourseStaffAssignmentStatus.ACTIVE", "CourseStaffAssignmentStatus.ENDED")
                .doesNotContain("status != CANCELLED", "status <> CANCELLED", "stream().filter");
    }

    @Test
    void courseStaffAssignmentAccessibleQueriesSupportOwnAndManagedCourseScope() throws Exception {
        Query listQuery = CourseStaffAssignmentRepository.class
                .getMethod(
                        "findAccessible",
                        UUID.class,
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        Pageable.class
                )
                .getAnnotation(Query.class);
        Query detailQuery = CourseStaffAssignmentRepository.class
                .getMethod(
                        "findAccessibleById",
                        UUID.class,
                        UUID.class,
                        boolean.class,
                        boolean.class,
                        boolean.class
                )
                .getAnnotation(Query.class);

        assertScopedCourseStaffAssignmentQuery(listQuery.value());
        assertScopedCourseStaffAssignmentQuery(detailQuery.value());
    }

    private void assertScopedAttendanceQuery(String query) {
        assertThat(query)
                .contains("UserPerson up", "RelationshipType.GUARDIAN", "up.active = true")
                .contains("CourseStaffAssignment csa")
                .contains("csa.startDate <= cs.sessionDate", "csa.endDate is null or csa.endDate >= cs.sessionDate")
                .contains("CourseStaffAssignmentStatus.ACTIVE", "CourseStaffAssignmentStatus.ENDED")
                .doesNotContain("status != CANCELLED", "status <> CANCELLED", "stream().filter");
    }

    private void assertScopedTimesheetQuery(String query) {
        assertThat(query)
                .contains("csa.staffPerson.personId = :activePersonId")
                .contains("actorAssignment.assignmentType = com.dat.ai_receptionist_web.enums.Training.AssignmentType.MANAGER")
                .contains("actorAssignment.startDate <= cs.sessionDate")
                .contains("actorAssignment.endDate is null or actorAssignment.endDate >= cs.sessionDate")
                .contains("CourseStaffAssignmentStatus.ACTIVE", "CourseStaffAssignmentStatus.ENDED")
                .doesNotContain("status != CANCELLED", "status <> CANCELLED", "stream().filter");
    }

    private void assertScopedCourseStaffAssignmentQuery(String query) {
        assertThat(query)
                .contains("a.staffPerson.personId = :activePersonId")
                .contains("managerAssignment.assignmentType = com.dat.ai_receptionist_web.enums.Training.AssignmentType.MANAGER")
                .contains("managerAssignment.course.courseId = a.course.courseId")
                .contains("managerAssignment.startDate <= a.endDate")
                .contains("managerAssignment.endDate is null or managerAssignment.endDate >= a.startDate")
                .contains("CourseStaffAssignmentStatus.ACTIVE", "CourseStaffAssignmentStatus.ENDED")
                .doesNotContain("status != CANCELLED", "status <> CANCELLED", "stream().filter");
    }
}

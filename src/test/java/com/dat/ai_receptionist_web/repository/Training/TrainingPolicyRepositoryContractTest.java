package com.dat.ai_receptionist_web.repository.Training;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingPolicyRepositoryContractTest {
    @Test
    void sessionAttendanceAccessibleQueriesUseDbScopedPolicyPaths() throws Exception {
        Query listQuery = SessionAttendanceRepository.class
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
                        UUID.class,
                        Pageable.class
                )
                .getAnnotation(Query.class);
        Query detailQuery = SessionAttendanceRepository.class
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
        assertThat(listQuery.value())
                .contains(
                        "join fetch a.classSession",
                        "left join fetch sessionCourse.classSchedule",
                        "left join fetch sessionCourseSchedule.branch",
                        "left join fetch sessionCourse.nextClassSchedule",
                        "left join fetch sessionCourseNextSchedule.branch",
                        "left join fetch a.studentEnrollment",
                        "left join fetch e.studentPerson",
                        "left join fetch e.classSchedule",
                        "left join fetch enrollmentSchedule.branch",
                        "left join fetch e.coursePurchase",
                        "left join fetch purchase.coursePrice",
                        "left join fetch a.courseStaffAssignment",
                        "left join fetch participantAssignment.staffPerson",
                        "left join fetch participantCourse.classSchedule",
                        "left join fetch participantCourseSchedule.branch",
                        "left join fetch participantCourse.nextClassSchedule",
                        "left join fetch participantCourseNextSchedule.branch"
                );
        assertThat(listQuery.countQuery()).isNotBlank();
        assertThat(listQuery.countQuery()).doesNotContain("fetch");
        assertScopedAttendanceQuery(detailQuery.value());
        assertThat(detailQuery.value())
                .contains(
                        "join fetch a.classSession",
                        "left join fetch sessionCourse.classSchedule",
                        "left join fetch sessionCourseNextSchedule.branch",
                        "left join fetch a.studentEnrollment",
                        "left join fetch e.studentPerson",
                        "left join fetch enrollmentStudent.position",
                        "left join fetch e.classSchedule",
                        "left join fetch enrollmentSchedule.branch",
                        "left join fetch e.coursePurchase",
                        "left join fetch a.courseStaffAssignment",
                        "left join fetch participantAssignment.staffPerson",
                        "left join fetch participantStaff.position",
                        "left join fetch participantCourse.classSchedule",
                        "left join fetch participantCourse.nextClassSchedule"
                );
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
                .contains(
                        "left join fetch e.coursePurchase",
                        "left join fetch e.studentPerson",
                        "left join fetch e.classSchedule",
                        "left join fetch enrollmentSchedule.branch"
                )
                .doesNotContain("status != CANCELLED", "stream().filter");
        assertThat(detailQuery.value())
                .contains("UserPerson up", "RelationshipType.GUARDIAN")
                .contains("csa.startDate <= e.endDate", "csa.endDate is null or csa.endDate >= e.startDate")
                .contains("CourseStaffAssignmentStatus.ACTIVE", "CourseStaffAssignmentStatus.ENDED")
                .contains(
                        "left join fetch e.coursePurchase",
                        "left join fetch e.studentPerson",
                        "left join fetch enrollmentStudent.position",
                        "left join fetch e.classSchedule",
                        "left join fetch enrollmentSchedule.branch"
                )
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
        assertThat(listQuery.value())
                .contains(
                        "join fetch t.classSession",
                        "join fetch t.courseStaffAssignment",
                        "left join fetch sessionCourse.classSchedule",
                        "left join fetch sessionCourse.nextClassSchedule",
                        "left join fetch csa.staffPerson",
                        "left join fetch assignmentCourse.classSchedule",
                        "left join fetch assignmentCourse.nextClassSchedule"
                )
                .doesNotContain("left join fetch assignmentStaff.position");
        assertThat(detailQuery.value())
                .contains(
                        "join fetch t.classSession",
                        "join fetch t.courseStaffAssignment",
                        "left join fetch sessionCourse.classSchedule",
                        "left join fetch sessionCourse.nextClassSchedule",
                        "left join fetch csa.staffPerson",
                        "left join fetch assignmentStaff.position",
                        "left join fetch assignmentCourse.classSchedule",
                        "left join fetch assignmentCourse.nextClassSchedule"
                );
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
                .contains(
                        "left join fetch c.course",
                        "left join fetch course.classSchedule",
                        "left join fetch courseSchedule.branch",
                        "left join fetch course.nextClassSchedule",
                        "left join fetch courseNextSchedule.branch"
                )
                .doesNotContain("status != CANCELLED", "status <> CANCELLED", "stream().filter");
        assertThat(detailQuery.value())
                .contains("c.classSessionId = :id")
                .contains("a.startDate <= c.sessionDate", "a.endDate is null or a.endDate >= c.sessionDate")
                .contains("CourseStaffAssignmentStatus.ACTIVE", "CourseStaffAssignmentStatus.ENDED")
                .contains(
                        "left join fetch c.course",
                        "left join fetch course.classSchedule",
                        "left join fetch course.nextClassSchedule"
                )
                .doesNotContain("status != CANCELLED", "status <> CANCELLED", "stream().filter");
    }

    @Test
    void classSessionCalendarQueryUsesDateRangeSortAndFetchesCourse() throws Exception {
        Query calendarQuery = ClassSessionRepository.class
                .getMethod("findCalendarSessions", LocalDate.class, LocalDate.class, UUID.class, boolean.class)
                .getAnnotation(Query.class);

        assertThat(calendarQuery.value())
                .contains(
                        "join fetch c.course",
                        "c.sessionDate >= :fromDate",
                        "c.sessionDate <= :toDate",
                        "order by c.sessionDate asc, c.startTime asc"
                )
                .contains("CourseStaffAssignment a")
                .contains("a.startDate <= c.sessionDate", "a.endDate is null or a.endDate >= c.sessionDate")
                .doesNotContain("Pageable", "PageResponse");
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
        assertThat(listQuery.value())
                .contains(
                        "left join fetch a.staffPerson",
                        "left join fetch a.course",
                        "left join fetch course.classSchedule",
                        "left join fetch courseSchedule.branch",
                        "left join fetch course.nextClassSchedule",
                        "left join fetch courseNextSchedule.branch"
                )
                .doesNotContain("left join fetch staffPerson.position");
        assertThat(detailQuery.value())
                .contains(
                        "left join fetch a.staffPerson",
                        "left join fetch staffPerson.position",
                        "left join fetch a.course",
                        "left join fetch course.classSchedule",
                        "left join fetch course.nextClassSchedule"
                );
    }

    @Test
    void courseStaffAssignmentBatchStaffQueriesFetchPersonAndUseEffectivePeriod() throws Exception {
        Query courseStaffQuery = CourseStaffAssignmentRepository.class
                .getMethod("findEffectiveStaffByCourseIdsAndDate",
                        java.util.Collection.class, java.util.Collection.class, LocalDate.class)
                .getAnnotation(Query.class);
        Query primaryCoachRangeQuery = CourseStaffAssignmentRepository.class
                .getMethod("findEffectivePrimaryCoachAssignmentsForCourseIdsBetween",
                        java.util.Collection.class, LocalDate.class, LocalDate.class)
                .getAnnotation(Query.class);

        assertThat(courseStaffQuery.value())
                .contains(
                        "join fetch a.staffPerson",
                        "left join fetch staffPerson.position",
                        "a.course.courseId in :courseIds",
                        "a.assignmentType in :assignmentTypes",
                        "a.startDate <= :effectiveDate",
                        "a.endDate is null or a.endDate >= :effectiveDate",
                        "CourseStaffAssignmentStatus.ACTIVE",
                        "CourseStaffAssignmentStatus.ENDED"
                );
        assertThat(primaryCoachRangeQuery.value())
                .contains(
                        "join fetch a.staffPerson",
                        "left join fetch staffPerson.position",
                        "AssignmentType.PRIMARY_COACH",
                        "a.startDate <= :toDate",
                        "a.endDate is null or a.endDate >= :fromDate"
                );
    }

    @Test
    void beltExamReadQueriesFetchTheirMappingGraph() throws Exception {
        Query listQuery = BeltExamRepository.class
                .getMethod("findAllDetailed", Pageable.class)
                .getAnnotation(Query.class);
        Query detailQuery = BeltExamRepository.class
                .getMethod("findDetailedById", UUID.class)
                .getAnnotation(Query.class);

        assertThat(listQuery.value())
                .contains("left join fetch b.person")
                .doesNotContain("left join fetch person.position", "left join fetch b.createdByUser");
        assertThat(listQuery.countQuery()).isNotBlank();
        assertThat(listQuery.countQuery()).doesNotContain("fetch");

        assertThat(detailQuery.value())
                .contains(
                        "left join fetch b.person",
                        "left join fetch person.position",
                        "left join fetch b.createdByUser"
                );
    }

    @Test
    void leaveRequestReadQueriesFetchTheirMappingGraph() throws Exception {
        Query listQuery = LeaveRequestRepository.class
                .getMethod("findAllDetailed", Pageable.class)
                .getAnnotation(Query.class);
        Query detailQuery = LeaveRequestRepository.class
                .getMethod("findDetailedById", UUID.class)
                .getAnnotation(Query.class);

        assertThat(listQuery.value())
                .contains(
                        "left join fetch lr.person",
                        "left join fetch lr.leaveClassSession",
                        "left join fetch leaveSessionCourse.classSchedule",
                        "left join fetch leaveSessionSchedule.branch",
                        "left join fetch leaveSessionCourse.nextClassSchedule",
                        "left join fetch leaveSessionNextSchedule.branch",
                        "left join fetch lr.makeupClassSession",
                        "left join fetch makeupSessionCourse.classSchedule",
                        "left join fetch makeupSessionCourse.nextClassSchedule"
                )
                .doesNotContain("left join fetch lr.createdByUser", "left join fetch lr.reviewedByUser");
        assertThat(listQuery.countQuery()).isNotBlank();
        assertThat(listQuery.countQuery()).doesNotContain("fetch");

        assertThat(detailQuery.value())
                .contains(
                        "left join fetch lr.person",
                        "left join fetch person.position",
                        "left join fetch lr.leaveClassSession",
                        "left join fetch lr.makeupClassSession",
                        "left join fetch lr.createdByUser",
                        "left join fetch lr.reviewedByUser"
                );
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

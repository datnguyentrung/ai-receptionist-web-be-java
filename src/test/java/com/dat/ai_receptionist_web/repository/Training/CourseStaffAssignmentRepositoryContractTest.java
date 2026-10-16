package com.dat.ai_receptionist_web.repository.Training;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class CourseStaffAssignmentRepositoryContractTest {
    @Test
    void effectiveScheduleStaffQueryFiltersByScheduleDateAndActiveLikeStatuses() throws Exception {
        Query query = CourseStaffAssignmentRepository.class
                .getMethod("findEffectiveByCourseScheduleIdsAndDate", Collection.class, LocalDate.class)
                .getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.value())
                .contains("join fetch a.courseSchedule courseSchedule")
                .contains("courseSchedule.courseScheduleId in :courseScheduleIds")
                .contains("a.startDate <= :effectiveDate")
                .contains("a.endDate is null or a.endDate >= :effectiveDate")
                .contains("CourseStaffAssignmentStatus.ACTIVE")
                .contains("CourseStaffAssignmentStatus.ENDED")
                .contains("join fetch a.staffPerson");
    }

    @Test
    void primaryCoachConflictQueryChecksScheduleRolePeriodStatusAndExcludedAssignment() throws Exception {
        Query query = CourseStaffAssignmentRepository.class
                .getMethod("existsOverlappingPrimaryAssignment", java.util.UUID.class,
                        LocalDate.class, LocalDate.class, java.util.UUID.class)
                .getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.value())
                .contains("a.courseSchedule.courseScheduleId = :courseScheduleId")
                .contains("AssignmentType.PRIMARY_COACH")
                .contains(":excludeId is null or a.courseStaffAssignmentId <> :excludeId")
                .contains("a.startDate <= coalesce(:endDate, a.startDate)")
                .contains("a.endDate is null or a.endDate >= :startDate")
                .contains("CourseStaffAssignmentStatus.ACTIVE")
                .contains("CourseStaffAssignmentStatus.ENDED");
    }
}

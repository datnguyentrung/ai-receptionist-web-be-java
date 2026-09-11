package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.enums.Training.AssignmentType;
import com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Training.access.CoachTimesheetAccessPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoachTimesheetAccessPolicyTest {
    private final CoachTimesheetAccessPolicy policy = new CoachTimesheetAccessPolicy();

    @Test
    void endedAssignmentStillAllowsHistoricalSessionInsidePeriod() {
        UUID courseId = UUID.randomUUID();
        UUID staffPersonId = UUID.randomUUID();

        assertThatCode(() -> policy.requireCanCreate(
                context(staffPersonId),
                session(courseId, LocalDate.of(2026, 3, 15)),
                assignment(staffPersonId, courseId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))
        )).doesNotThrowAnyException();
    }

    @Test
    void assignmentOutsideSessionDateDeniesTimesheet() {
        UUID courseId = UUID.randomUUID();
        UUID staffPersonId = UUID.randomUUID();

        assertThatThrownBy(() -> policy.requireCanCreate(
                context(staffPersonId),
                session(courseId, LocalDate.of(2026, 4, 15)),
                assignment(staffPersonId, courseId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))
        ))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE);
    }

    @Test
    void nonAdminReadScopeIsSelfPlusManagedCourses() {
        var scope = policy.resolveReadScope(context(UUID.randomUUID()));
        assertThat(scope.unrestricted()).isFalse();
        assertThat(scope.self()).isTrue();
        assertThat(scope.assignedCourses()).isFalse();
        assertThat(scope.managedCourses()).isTrue();
    }

    @Test
    void pendingSuspendedAndCancelledAssignmentsDoNotGrantAccess() {
        UUID courseId = UUID.randomUUID();
        UUID staffPersonId = UUID.randomUUID();
        ClassSession session = session(courseId, LocalDate.of(2026, 3, 15));

        for (CourseStaffAssignmentStatus status : new CourseStaffAssignmentStatus[]{
                CourseStaffAssignmentStatus.PENDING,
                CourseStaffAssignmentStatus.SUSPENDED,
                CourseStaffAssignmentStatus.CANCELLED
        }) {
            CourseStaffAssignment assignment = assignment(staffPersonId, courseId,
                    LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
            assignment.setAssignmentStatus(status);

            assertThatThrownBy(() -> policy.requireCanCreate(context(staffPersonId), session, assignment))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode")
                    .isEqualTo(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE);
        }
    }

    private AccessContext context(UUID activePersonId) {
        return new AccessContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                activePersonId,
                null,
                Set.of(),
                Set.of()
        );
    }

    private ClassSession session(UUID courseId, LocalDate sessionDate) {
        return ClassSession.builder()
                .course(Course.builder().courseId(courseId).build())
                .sessionDate(sessionDate)
                .status(SessionStatus.ACTIVE)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .build();
    }

    private CourseStaffAssignment assignment(UUID staffPersonId, UUID courseId, LocalDate startDate, LocalDate endDate) {
        return CourseStaffAssignment.builder()
                .staffPerson(Person.builder().personId(staffPersonId).personCode("VQT_001").build())
                .course(Course.builder().courseId(courseId).build())
                .assignmentType(AssignmentType.PRIMARY_COACH)
                .startDate(startDate)
                .endDate(endDate)
                .assignmentStatus(CourseStaffAssignmentStatus.ENDED)
                .build();
    }
}

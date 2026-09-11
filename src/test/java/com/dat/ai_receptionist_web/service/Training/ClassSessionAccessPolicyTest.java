package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.enums.Security.SystemRoleDefinition;
import com.dat.ai_receptionist_web.enums.Training.AssignmentType;
import com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Training.access.ClassSessionAccessPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClassSessionAccessPolicyTest {
    private final CourseStaffAssignmentRepository repository = mock(CourseStaffAssignmentRepository.class);
    private final ClassSessionAccessPolicy policy = new ClassSessionAccessPolicy(repository);

    @Test
    void adminHasUnrestrictedReadAndWriteScope() {
        AccessContext context = adminContext();

        assertThat(policy.resolveReadScope(context).unrestricted()).isTrue();
        assertThat(policy.resolveWriteScope(context).unrestricted()).isTrue();
        assertThatCode(() -> policy.requireCanManage(context, UUID.randomUUID(), LocalDate.of(2026, 3, 15)))
                .doesNotThrowAnyException();
        verify(repository, never()).findEffectiveAssignmentsForStaffCourseOnDate(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void activeAssignmentAllowsManage() {
        UUID staffPersonId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        LocalDate sessionDate = LocalDate.of(2026, 3, 15);
        when(repository.findEffectiveAssignmentsForStaffCourseOnDate(staffPersonId, courseId, sessionDate))
                .thenReturn(List.of(assignment(staffPersonId, courseId, CourseStaffAssignmentStatus.ACTIVE)));

        assertThatCode(() -> policy.requireCanManage(context(staffPersonId), courseId, sessionDate))
                .doesNotThrowAnyException();
    }

    @Test
    void historicalEndedAssignmentAllowsManage() {
        UUID staffPersonId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        LocalDate sessionDate = LocalDate.of(2026, 3, 15);
        when(repository.findEffectiveAssignmentsForStaffCourseOnDate(staffPersonId, courseId, sessionDate))
                .thenReturn(List.of(assignment(staffPersonId, courseId, CourseStaffAssignmentStatus.ENDED)));

        assertThatCode(() -> policy.requireCanManage(context(staffPersonId), courseId, sessionDate))
                .doesNotThrowAnyException();
    }

    @Test
    void outsideEffectiveDateDeniesManage() {
        UUID staffPersonId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        LocalDate sessionDate = LocalDate.of(2026, 4, 15);
        when(repository.findEffectiveAssignmentsForStaffCourseOnDate(staffPersonId, courseId, sessionDate))
                .thenReturn(List.of());

        assertThatThrownBy(() -> policy.requireCanManage(context(staffPersonId), courseId, sessionDate))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE);
    }

    @Test
    void pendingSuspendedAndCancelledAssignmentsDenyManage() {
        UUID staffPersonId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        LocalDate sessionDate = LocalDate.of(2026, 3, 15);

        for (CourseStaffAssignmentStatus status : List.of(
                CourseStaffAssignmentStatus.PENDING,
                CourseStaffAssignmentStatus.SUSPENDED,
                CourseStaffAssignmentStatus.CANCELLED
        )) {
            when(repository.findEffectiveAssignmentsForStaffCourseOnDate(staffPersonId, courseId, sessionDate))
                    .thenReturn(List.of(assignment(staffPersonId, courseId, status)));

            assertThatThrownBy(() -> policy.requireCanManage(context(staffPersonId), courseId, sessionDate))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode")
                    .isEqualTo(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE);
        }
    }

    private AccessContext context(UUID activePersonId) {
        return new AccessContext(UUID.randomUUID(), UUID.randomUUID(), activePersonId, null, Set.of(), Set.of());
    }

    private AccessContext adminContext() {
        return new AccessContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                Set.of(SystemRoleDefinition.SYSTEM_ADMIN.getCode()),
                Set.of()
        );
    }

    private CourseStaffAssignment assignment(
            UUID staffPersonId,
            UUID courseId,
            CourseStaffAssignmentStatus status
    ) {
        return CourseStaffAssignment.builder()
                .staffPerson(Person.builder().personId(staffPersonId).build())
                .course(Course.builder().courseId(courseId).build())
                .assignmentType(AssignmentType.PRIMARY_COACH)
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .assignmentStatus(status)
                .build();
    }
}

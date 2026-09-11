package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.enums.Security.SystemRoleDefinition;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Training.access.CourseStaffAssignmentAccessPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseStaffAssignmentAccessPolicyTest {
    private final CourseStaffAssignmentRepository repository = mock(CourseStaffAssignmentRepository.class);
    private final CourseStaffAssignmentAccessPolicy policy = new CourseStaffAssignmentAccessPolicy(repository);

    @Test
    void ownAssignmentReadAndManagedCourseReadScopesAreSeparate() {
        var scope = policy.resolveReadScope(context(UUID.randomUUID()));

        assertThat(scope.self()).isTrue();
        assertThat(scope.managedCourses()).isTrue();
        assertThat(scope.assignedCourses()).isFalse();
    }

    @Test
    void adminHasUnrestrictedReadAndWriteScope() {
        AccessContext context = adminContext();

        assertThat(policy.resolveReadScope(context).unrestricted()).isTrue();
        assertThat(policy.resolveWriteScope(context).unrestricted()).isTrue();
        assertThatCode(() -> policy.requireCanManagePeriod(
                context, UUID.randomUUID(), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .doesNotThrowAnyException();
    }

    @Test
    void managerCanWriteOnlyWithinManagedCoursePeriod() {
        UUID managerPersonId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);
        when(repository.existsManagerAssignmentCoveringPeriod(managerPersonId, courseId, startDate, endDate))
                .thenReturn(true);

        assertThatCode(() -> policy.requireCanManagePeriod(
                context(managerPersonId), courseId, startDate, endDate))
                .doesNotThrowAnyException();
    }

    @Test
    void unrelatedCourseWriteIsDenied() {
        UUID managerPersonId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);
        when(repository.existsManagerAssignmentCoveringPeriod(managerPersonId, courseId, startDate, endDate))
                .thenReturn(false);

        assertThatThrownBy(() -> policy.requireCanManagePeriod(
                context(managerPersonId), courseId, startDate, endDate))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE);
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
}

package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Training.access.StudentEnrollmentAccessPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class StudentEnrollmentAccessPolicyTest {
    private final CourseStaffAssignmentRepository assignmentRepository = mock(CourseStaffAssignmentRepository.class);
    private final StudentEnrollmentAccessPolicy policy = new StudentEnrollmentAccessPolicy(assignmentRepository);

    @Test
    void assignedStaffCanManageEnrollmentInsideAssignedCoursePeriod() {
        UUID personId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        when(assignmentRepository.existsEffectiveAssignmentForStaffCoursePeriod(personId, courseId, from, to))
                .thenReturn(true);

        assertThatCode(() -> policy.requireCanManageCoursePeriod(context(personId, Set.of()), courseId, from, to))
                .doesNotThrowAnyException();
    }

    @Test
    void unassignedStaffCannotManageEnrollment() {
        UUID personId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 4, 30);

        assertThatThrownBy(() -> policy.requireCanManageCoursePeriod(context(personId, Set.of()), courseId, from, to))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE);
    }

    @Test
    void systemAdminBypassesAssignmentScope() {
        assertThatCode(() -> policy.requireCanManageCoursePeriod(
                context(null, Set.of("SYSTEM_ADMIN")),
                UUID.randomUUID(),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        )).doesNotThrowAnyException();
        verifyNoInteractions(assignmentRepository);
    }

    private AccessContext context(UUID activePersonId, Set<String> roles) {
        return new AccessContext(
                UUID.randomUUID(),
                activePersonId == null ? null : UUID.randomUUID(),
                activePersonId,
                null,
                roles,
                Set.of()
        );
    }
}
package com.dat.ai_receptionist_web.security;

import com.dat.ai_receptionist_web.controller.Training.ClassSessionController;
import com.dat.ai_receptionist_web.controller.Training.CoachTimesheetController;
import com.dat.ai_receptionist_web.controller.Training.CourseStaffAssignmentController;
import com.dat.ai_receptionist_web.controller.Training.StudentAttendanceController;
import com.dat.ai_receptionist_web.controller.Training.StudentEnrollmentController;
import com.dat.ai_receptionist_web.dto.Training.ClassSessionDTO;
import com.dat.ai_receptionist_web.dto.Training.CoachTimesheetDTO;
import com.dat.ai_receptionist_web.dto.Training.CourseStaffAssignmentDTO;
import com.dat.ai_receptionist_web.dto.Training.StudentAttendanceDTO;
import com.dat.ai_receptionist_web.dto.Training.StudentEnrollmentDTO;
import com.dat.ai_receptionist_web.enums.Security.PermissionDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingAuthorizationContractTest {
    @Test
    void protectedTrainingControllersUseCoarseRbacPreAuthorizeAnnotations() throws Exception {
        assertPermission(ClassSessionController.class, "list", PermissionDefinition.CLASS_SESSION_READ, Pageable.class);
        assertPermission(ClassSessionController.class, "get", PermissionDefinition.CLASS_SESSION_READ, UUID.class);
        assertPermission(ClassSessionController.class, "create", PermissionDefinition.CLASS_SESSION_CREATE,
                ClassSessionDTO.CreateRequest.class);
        assertPermission(ClassSessionController.class, "update", PermissionDefinition.CLASS_SESSION_UPDATE,
                UUID.class, ClassSessionDTO.UpdateRequest.class);
        assertPermission(ClassSessionController.class, "delete", PermissionDefinition.CLASS_SESSION_DELETE, UUID.class);
        assertPermission(ClassSessionController.class, "reopenAttendance", PermissionDefinition.CLASS_SESSION_UPDATE,
                UUID.class, ClassSessionDTO.ReopenAttendanceRequest.class);

        assertPermission(StudentAttendanceController.class, "get", PermissionDefinition.STUDENT_ATTENDANCE_READ,
                UUID.class);
        assertPermission(StudentAttendanceController.class, "create", PermissionDefinition.STUDENT_ATTENDANCE_CREATE,
                StudentAttendanceDTO.CreateRequest.class);
        assertPermission(StudentAttendanceController.class, "update", PermissionDefinition.STUDENT_ATTENDANCE_UPDATE,
                UUID.class, StudentAttendanceDTO.UpdateRequest.class);
        assertPermission(StudentAttendanceController.class, "delete", PermissionDefinition.STUDENT_ATTENDANCE_DELETE,
                UUID.class);

        assertPermission(StudentEnrollmentController.class, "get", PermissionDefinition.STUDENT_ENROLLMENT_READ,
                UUID.class);
        assertPermission(StudentEnrollmentController.class, "create", PermissionDefinition.STUDENT_ENROLLMENT_CREATE,
                StudentEnrollmentDTO.CreateRequest.class);
        assertPermission(StudentEnrollmentController.class, "update", PermissionDefinition.STUDENT_ENROLLMENT_UPDATE,
                UUID.class, StudentEnrollmentDTO.UpdateRequest.class);
        assertPermission(StudentEnrollmentController.class, "delete", PermissionDefinition.STUDENT_ENROLLMENT_DELETE,
                UUID.class);

        assertPermission(CoachTimesheetController.class, "get", PermissionDefinition.COACH_TIMESHEET_READ, UUID.class);
        assertPermission(CoachTimesheetController.class, "create", PermissionDefinition.COACH_TIMESHEET_CREATE,
                CoachTimesheetDTO.CreateRequest.class);
        assertPermission(CoachTimesheetController.class, "update", PermissionDefinition.COACH_TIMESHEET_UPDATE,
                UUID.class, CoachTimesheetDTO.UpdateRequest.class);
        assertPermission(CoachTimesheetController.class, "delete", PermissionDefinition.COACH_TIMESHEET_DELETE,
                UUID.class);

        assertPermission(CourseStaffAssignmentController.class, "get",
                PermissionDefinition.COURSE_STAFF_ASSIGNMENT_READ, UUID.class);
        assertPermission(CourseStaffAssignmentController.class, "create",
                PermissionDefinition.COURSE_STAFF_ASSIGNMENT_CREATE, CourseStaffAssignmentDTO.CreateRequest.class);
        assertPermission(CourseStaffAssignmentController.class, "update",
                PermissionDefinition.COURSE_STAFF_ASSIGNMENT_UPDATE,
                UUID.class, CourseStaffAssignmentDTO.UpdateRequest.class);
        assertPermission(CourseStaffAssignmentController.class, "delete",
                PermissionDefinition.COURSE_STAFF_ASSIGNMENT_DELETE, UUID.class);
    }

    @Test
    void outOfScopeDetailReadsUseAccessibleByIdAndNotFoundErrors() throws Exception {
        assertOutOfScopeReturnsNotFound(
                "src/main/java/com/dat/ai_receptionist_web/service/Training/StudentAttendanceService.java",
                "repository.findAccessibleById",
                "TrainingErrorCode.STUDENT_ATTENDANCE_NOT_FOUND"
        );
        assertOutOfScopeReturnsNotFound(
                "src/main/java/com/dat/ai_receptionist_web/service/Training/StudentEnrollmentService.java",
                "repository.findAccessibleById",
                "TrainingErrorCode.STUDENT_ENROLLMENT_NOT_FOUND"
        );
        assertOutOfScopeReturnsNotFound(
                "src/main/java/com/dat/ai_receptionist_web/service/Training/CoachTimesheetService.java",
                "repository.findAccessibleById",
                "TrainingErrorCode.COACH_TIMESHEET_NOT_FOUND"
        );
        assertOutOfScopeReturnsNotFound(
                "src/main/java/com/dat/ai_receptionist_web/service/Training/session/ClassSessionService.java",
                "repository.findAccessibleById",
                "TrainingErrorCode.CLASS_SESSION_NOT_FOUND"
        );
        assertOutOfScopeReturnsNotFound(
                "src/main/java/com/dat/ai_receptionist_web/service/Training/CourseStaffAssignmentService.java",
                "repository.findAccessibleById",
                "TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_FOUND"
        );
    }

    @Test
    void listEndpointsUseScopedServiceFiltersWherePolicyRequiresDateScope() throws Exception {
        assertPermission(
                StudentAttendanceController.class,
                "list",
                PermissionDefinition.STUDENT_ATTENDANCE_READ,
                LocalDate.class,
                LocalDate.class,
                UUID.class,
                UUID.class,
                Pageable.class
        );
        assertPermission(
                StudentEnrollmentController.class,
                "list",
                PermissionDefinition.STUDENT_ENROLLMENT_READ,
                LocalDate.class,
                LocalDate.class,
                UUID.class,
                UUID.class,
                Pageable.class
        );
        assertPermission(
                CoachTimesheetController.class,
                "list",
                PermissionDefinition.COACH_TIMESHEET_READ,
                LocalDate.class,
                LocalDate.class,
                UUID.class,
                Pageable.class
        );
    }

    private void assertPermission(
            Class<?> controller,
            String methodName,
            PermissionDefinition permission,
            Class<?>... parameterTypes
    ) throws Exception {
        PreAuthorize annotation = controller.getMethod(methodName, parameterTypes).getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).contains(permission.name(), ".getCode()");
    }

    private void assertOutOfScopeReturnsNotFound(
            String sourcePath,
            String accessibleByIdCall,
            String notFoundError
    ) throws Exception {
        String source = Files.readString(Path.of(sourcePath));
        assertThat(source)
                .contains(accessibleByIdCall)
                .contains(".orElseThrow(() -> new ApiException(" + notFoundError + "))");
    }
}

package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Finance.CoursePurchase;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.enums.Training.AssignmentType;
import com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.service.Training.access.SessionAttendanceAccessPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionAttendanceAccessPolicyTest {
    private final SessionAttendanceAccessPolicy policy = new SessionAttendanceAccessPolicy();

    @Test
    void effectiveStudentEnrollmentAllowsCreate() {
        UUID courseId = UUID.randomUUID();
        ClassSession session = session(courseId, LocalDate.of(2026, 3, 15), false, null);
        StudentEnrollment enrollment = enrollment(courseId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertThatCode(() -> policy.requireCanCreate(session, enrollment, null))
                .doesNotThrowAnyException();
    }

    @Test
    void enrollmentOutsideSessionDateDeniesCreate() {
        UUID courseId = UUID.randomUUID();
        ClassSession session = session(courseId, LocalDate.of(2026, 4, 15), false, null);
        StudentEnrollment enrollment = enrollment(courseId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertThatThrownBy(() -> policy.requireCanCreate(session, enrollment, null))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(TrainingErrorCode.STUDENT_ENROLLMENT_NOT_EFFECTIVE);
    }

    @Test
    void closedAttendanceDeniesUnlessReopened() {
        UUID courseId = UUID.randomUUID();
        StudentEnrollment enrollment = enrollment(courseId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertThatThrownBy(() -> policy.requireCanCreate(
                session(courseId, LocalDate.of(2026, 3, 15), true, LocalDateTime.now().minusMinutes(1)),
                enrollment,
                null
        ))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(TrainingErrorCode.ATTENDANCE_CLOSED);

        assertThatCode(() -> policy.requireCanCreate(
                session(courseId, LocalDate.of(2026, 3, 15), true, LocalDateTime.now().plusMinutes(30)),
                enrollment,
                null
        )).doesNotThrowAnyException();
    }

    @Test
    void pendingSuspendedAndCancelledAssistantParticipantsCannotHaveSessionAttendance() {
        UUID courseId = UUID.randomUUID();
        ClassSession session = session(courseId, LocalDate.of(2026, 3, 15), false, null);

        for (CourseStaffAssignmentStatus status : new CourseStaffAssignmentStatus[]{
                CourseStaffAssignmentStatus.PENDING,
                CourseStaffAssignmentStatus.SUSPENDED,
                CourseStaffAssignmentStatus.CANCELLED
        }) {
            CourseStaffAssignment assignment = assignment(UUID.randomUUID(), courseId,
                    LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
            assignment.setAssignmentType(AssignmentType.ASSISTANT_COACH);
            assignment.setAssignmentStatus(status);

            assertThatThrownBy(() -> policy.requireCanCreate(session, null, assignment))
                    .isInstanceOf(ApiException.class)
                    .extracting("errorCode")
                    .isEqualTo(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE);
        }
    }

    @Test
    void onlyAssistantCoachParticipantCanHaveSessionAttendance() {
        UUID courseId = UUID.randomUUID();
        ClassSession session = session(courseId, LocalDate.of(2026, 3, 15), false, null);
        CourseStaffAssignment assistantParticipant = assignment(UUID.randomUUID(), courseId,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        assistantParticipant.setAssignmentType(AssignmentType.ASSISTANT_COACH);
        CourseStaffAssignment teachingAssistantParticipant = assignment(UUID.randomUUID(), courseId,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        teachingAssistantParticipant.setAssignmentType(AssignmentType.TEACHING_ASSISTANT);

        assertThatCode(() -> policy.requireCanCreate(
                session, null, assistantParticipant))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> policy.requireCanCreate(
                session, null, teachingAssistantParticipant))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE);
    }

    private ClassSession session(
            UUID courseId,
            LocalDate sessionDate,
            boolean attendanceClosed,
            LocalDateTime attendanceReopenedUntil
    ) {
        return ClassSession.builder()
                .course(Course.builder().courseId(courseId).build())
                .sessionDate(sessionDate)
                .status(SessionStatus.ACTIVE)
                .attendanceClosed(attendanceClosed)
                .attendanceReopenedUntil(attendanceReopenedUntil)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .build();
    }

    private StudentEnrollment enrollment(UUID courseId, LocalDate startDate, LocalDate endDate) {
        return StudentEnrollment.builder()
                .studentPerson(Person.builder().personId(UUID.randomUUID()).personCode("VQ_001").build())
                .coursePurchase(CoursePurchase.builder()
                        .coursePrice(com.dat.ai_receptionist_web.domain.Catalog.CoursePrice.builder()
                                .course(Course.builder().courseId(courseId).build())
                                .build())
                        .build())
                .startDate(startDate)
                .endDate(endDate)
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

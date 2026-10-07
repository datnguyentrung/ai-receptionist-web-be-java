package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Finance.CoursePurchase;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollmentSchedule;
import com.dat.ai_receptionist_web.enums.Training.AssignmentType;
import com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.service.Training.access.SessionAttendanceAccessPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionAttendanceAccessPolicyTest {
    private final SessionAttendanceAccessPolicy policy = new SessionAttendanceAccessPolicy();

    @Test
    void effectiveStudentEnrollmentAllowsCreate() {
        UUID courseId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        ClassSession session = session(courseId, scheduleId, LocalDate.of(2026, 3, 15), false, null);
        StudentEnrollment enrollment = enrollment(courseId, scheduleId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertThatCode(() -> policy.requireCanCreate(session, enrollment, null))
                .doesNotThrowAnyException();
    }

    @Test
    void enrollmentOutsideSessionDateDeniesCreate() {
        UUID courseId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        ClassSession session = session(courseId, scheduleId, LocalDate.of(2026, 4, 15), false, null);
        StudentEnrollment enrollment = enrollment(courseId, scheduleId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertThatThrownBy(() -> policy.requireCanCreate(session, enrollment, null))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(TrainingErrorCode.STUDENT_ENROLLMENT_NOT_EFFECTIVE);
    }

    @Test
    void closedAttendanceDeniesUnlessReopened() {
        UUID courseId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        StudentEnrollment enrollment = enrollment(courseId, scheduleId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertThatThrownBy(() -> policy.requireCanCreate(
                session(courseId, scheduleId, LocalDate.of(2026, 3, 15), true, LocalDateTime.now().minusMinutes(1)),
                enrollment,
                null
        ))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(TrainingErrorCode.ATTENDANCE_CLOSED);

        assertThatCode(() -> policy.requireCanCreate(
                session(courseId, scheduleId, LocalDate.of(2026, 3, 15), true, LocalDateTime.now().plusMinutes(30)),
                enrollment,
                null
        )).doesNotThrowAnyException();
    }

    @Test
    void pendingSuspendedAndCancelledAssistantParticipantsCannotHaveSessionAttendance() {
        UUID courseId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        ClassSession session = session(courseId, scheduleId, LocalDate.of(2026, 3, 15), false, null);

        for (CourseStaffAssignmentStatus status : new CourseStaffAssignmentStatus[]{
                CourseStaffAssignmentStatus.PENDING,
                CourseStaffAssignmentStatus.SUSPENDED,
                CourseStaffAssignmentStatus.CANCELLED
        }) {
            CourseStaffAssignment assignment = assignment(UUID.randomUUID(), courseId, scheduleId,
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
        UUID scheduleId = UUID.randomUUID();
        ClassSession session = session(courseId, scheduleId, LocalDate.of(2026, 3, 15), false, null);
        CourseStaffAssignment assistantParticipant = assignment(UUID.randomUUID(), courseId, scheduleId,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        assistantParticipant.setAssignmentType(AssignmentType.ASSISTANT_COACH);
        CourseStaffAssignment teachingAssistantParticipant = assignment(UUID.randomUUID(), courseId, scheduleId,
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
            UUID scheduleId,
            LocalDate sessionDate,
            boolean attendanceClosed,
            LocalDateTime attendanceReopenedUntil
    ) {
        Course course = Course.builder().courseId(courseId).build();
        CourseSchedule schedule = CourseSchedule.builder()
                .courseScheduleId(scheduleId)
                .course(course)
                .build();
        return ClassSession.builder()
                .course(course)
                .courseSchedule(schedule)
                .sessionDate(sessionDate)
                .status(SessionStatus.ACTIVE)
                .attendanceClosed(attendanceClosed)
                .attendanceReopenedUntil(attendanceReopenedUntil)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .build();
    }

    private StudentEnrollment enrollment(UUID courseId, UUID scheduleId, LocalDate startDate, LocalDate endDate) {
        Course course = Course.builder().courseId(courseId).build();
        CourseSchedule schedule = CourseSchedule.builder()
                .courseScheduleId(scheduleId)
                .course(course)
                .build();
        StudentEnrollment enrollment = StudentEnrollment.builder()
                .studentPerson(Person.builder().personId(UUID.randomUUID()).personCode("VQ_001").build())
                .coursePurchase(CoursePurchase.builder()
                        .coursePrice(com.dat.ai_receptionist_web.domain.Catalog.CoursePrice.builder()
                                .course(course)
                                .build())
                        .build())
                .startDate(startDate)
                .endDate(endDate)
                .status(StudentEnrollmentStatus.ACTIVE)
                .schedules(new LinkedHashSet<>())
                .build();
        enrollment.getSchedules().add(StudentEnrollmentSchedule.builder()
                .studentEnrollment(enrollment)
                .courseSchedule(schedule)
                .build());
        return enrollment;
    }

    private CourseStaffAssignment assignment(UUID staffPersonId, UUID courseId, UUID scheduleId, LocalDate startDate, LocalDate endDate) {
        Course course = Course.builder().courseId(courseId).build();
        CourseSchedule schedule = CourseSchedule.builder()
                .courseScheduleId(scheduleId)
                .course(course)
                .build();
        return CourseStaffAssignment.builder()
                .staffPerson(Person.builder().personId(staffPersonId).personCode("VQT_001").build())
                .courseSchedule(schedule)
                .assignmentType(AssignmentType.PRIMARY_COACH)
                .startDate(startDate)
                .endDate(endDate)
                .assignmentStatus(CourseStaffAssignmentStatus.ENDED)
                .build();
    }
}

package com.dat.ai_receptionist_web.service.Training.access;

import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.domain.Training.StudentAttendance;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Component
public class StudentAttendanceAccessPolicy {
    public TrainingAccessScope resolveReadScope(AccessContext context) {
        if (context.hasUnrestrictedRole()) {
            return new TrainingAccessScope(true, false, false, false, false);
        }
        boolean hasActivePerson = context.activePersonId() != null;
        return new TrainingAccessScope(
                false,
                hasActivePerson,
                context.relationshipType() == RelationshipType.GUARDIAN,
                hasActivePerson,
                false
        );
    }

    public TrainingAccessScope resolveWriteScope(AccessContext context) {
        if (context.hasUnrestrictedRole()) {
            return new TrainingAccessScope(true, false, false, false, false);
        }
        return new TrainingAccessScope(false, false, false, context.activePersonId() != null, false);
    }

    public void requireCanCreate(
            AccessContext context,
            ClassSession session,
            StudentEnrollment enrollment,
            CourseStaffAssignment assignment
    ) {
        requireCourseAndEnrollmentEffective(session, enrollment);
        requireAssignmentEffective(context, assignment, session);
        requireAttendanceEditable(session);
    }

    public void requireCanUpdate(
            AccessContext context,
            StudentAttendance attendance,
            CourseStaffAssignment actorAssignment
    ) {
        requireCourseAndEnrollmentEffective(attendance.getClassSession(), attendance.getStudentEnrollment());
        requireAssignmentEffective(context, actorAssignment, attendance.getClassSession());
        requireAttendanceEditable(attendance.getClassSession());
    }

    public void requireCanDelete(
            AccessContext context,
            StudentAttendance attendance,
            CourseStaffAssignment actorAssignment
    ) {
        requireCanUpdate(context, attendance, actorAssignment);
    }

    private void requireCourseAndEnrollmentEffective(ClassSession session, StudentEnrollment enrollment) {
        UUIDs.requireEqual(
                session.getCourse().getCourseId(),
                enrollment.getCoursePurchase().getCoursePrice().getCourse().getCourseId(),
                TrainingErrorCode.STUDENT_ENROLLMENT_NOT_EFFECTIVE
        );
        LocalDate sessionDate = session.getSessionDate();
        if (enrollment.getStartDate().isAfter(sessionDate) || enrollment.getEndDate().isBefore(sessionDate)) {
            throw new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_NOT_EFFECTIVE);
        }
    }

    private void requireAssignmentEffective(
            AccessContext context,
            CourseStaffAssignment assignment,
            ClassSession session
    ) {
        if (context.hasUnrestrictedRole()) {
            return;
        }
        if (context.activePersonId() == null
                || assignment == null
                || !Objects.equals(assignment.getStaffPerson().getPersonId(), context.activePersonId())
                || !Objects.equals(assignment.getCourse().getCourseId(), session.getCourse().getCourseId())
                || assignment.getStartDate().isAfter(session.getSessionDate())
                || (assignment.getEndDate() != null && assignment.getEndDate().isBefore(session.getSessionDate()))
                || !assignment.getAssignmentStatus().allowsPolicyAccess()) {
            throw new ApiException(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE);
        }
    }

    private void requireAttendanceEditable(ClassSession session) {
        LocalDateTime reopenedUntil = session.getAttendanceReopenedUntil();
        if (session.isAttendanceClosed()
                && (reopenedUntil == null || !reopenedUntil.isAfter(LocalDateTime.now()))) {
            throw new ApiException(TrainingErrorCode.ATTENDANCE_CLOSED);
        }
    }

    private static final class UUIDs {
        private static void requireEqual(Object left, Object right, TrainingErrorCode errorCode) {
            if (!Objects.equals(left, right)) {
                throw new ApiException(errorCode);
            }
        }
    }
}

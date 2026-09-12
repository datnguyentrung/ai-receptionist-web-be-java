package com.dat.ai_receptionist_web.service.Training.access;

import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.enums.Training.AssignmentType;
import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Component
public class SessionAttendanceAccessPolicy {
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

    public void requireCanCreate(
            ClassSession session,
            StudentEnrollment enrollment,
            CourseStaffAssignment participantAssignment
    ) {
        requireParticipantEffective(session, enrollment, participantAssignment);
        requireAttendanceEditable(session);
    }

    public void requireCanUpdate(SessionAttendance attendance) {
        requireParticipantEffective(
                attendance.getClassSession(),
                attendance.getStudentEnrollment(),
                attendance.getCourseStaffAssignment()
        );
        requireAttendanceEditable(attendance.getClassSession());
    }

    public void requireCanDelete(SessionAttendance attendance) {
        requireCanUpdate(attendance);
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

    private void requireParticipantEffective(
            ClassSession session,
            StudentEnrollment enrollment,
            CourseStaffAssignment participantAssignment
    ) {
        if ((enrollment == null) == (participantAssignment == null)) {
            throw new ApiException(TrainingErrorCode.SESSION_ATTENDANCE_PARTICIPANT_INVALID);
        }
        if (enrollment != null) {
            requireCourseAndEnrollmentEffective(session, enrollment);
            return;
        }
        UUIDs.requireEqual(
                session.getCourse().getCourseId(),
                participantAssignment.getCourse().getCourseId(),
                TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE
        );
        if (participantAssignment.getAssignmentType() != AssignmentType.ASSISTANT_COACH
                || participantAssignment.getStartDate().isAfter(session.getSessionDate())
                || (participantAssignment.getEndDate() != null
                && participantAssignment.getEndDate().isBefore(session.getSessionDate()))
                || !participantAssignment.getAssignmentStatus().allowsPolicyAccess()) {
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

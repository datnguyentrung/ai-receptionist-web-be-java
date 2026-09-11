package com.dat.ai_receptionist_web.service.Training.access;

import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.CoachTimesheet;
import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CoachTimesheetAccessPolicy {
    public TrainingAccessScope resolveReadScope(AccessContext context) {
        if (context.hasUnrestrictedRole()) {
            return new TrainingAccessScope(true, false, false, false, false);
        }
        boolean hasActivePerson = context.activePersonId() != null;
        return new TrainingAccessScope(false, hasActivePerson, false, false, hasActivePerson);
    }

    public TrainingAccessScope resolveWriteScope(AccessContext context) {
        if (context.hasUnrestrictedRole()) {
            return new TrainingAccessScope(true, false, false, false, false);
        }
        return new TrainingAccessScope(false, context.activePersonId() != null, false, false, false);
    }

    public void requireCanCreate(
            AccessContext context,
            ClassSession session,
            CourseStaffAssignment assignment
    ) {
        requireAssignmentEffective(context, assignment, session);
    }

    public void requireCanUpdate(AccessContext context, CoachTimesheet timesheet) {
        requireAssignmentEffective(context, timesheet.getCourseStaffAssignment(), timesheet.getClassSession());
    }

    public void requireCanDelete(AccessContext context, CoachTimesheet timesheet) {
        requireCanUpdate(context, timesheet);
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
}

package com.dat.ai_receptionist_web.service.Training.access;

import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClassSessionAccessPolicy {
    private final CourseStaffAssignmentRepository assignmentRepository;

    public TrainingAccessScope resolveReadScope(AccessContext context) {
        if (context.hasUnrestrictedRole()) {
            return new TrainingAccessScope(true, false, false, false, false);
        }
        return new TrainingAccessScope(false, false, false,
                context.activePersonId() != null, false);
    }

    public TrainingAccessScope resolveWriteScope(AccessContext context) {
        return resolveReadScope(context);
    }

    public void requireCanManage(
            AccessContext context,
            UUID courseId,
            LocalDate sessionDate
    ) {
        if (context.hasUnrestrictedRole()) {
            return;
        }
        if (context.activePersonId() == null
                || courseId == null
                || sessionDate == null
                || !hasExactlyOneEffectiveAssignment(context, courseId, sessionDate)) {
            throw new ApiException(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE);
        }
    }

    private boolean hasExactlyOneEffectiveAssignment(
            AccessContext context,
            UUID courseId,
            LocalDate sessionDate
    ) {
        var assignments = assignmentRepository.findEffectiveAssignmentsForStaffCourseOnDate(
                context.activePersonId(), courseId, sessionDate);
        if (assignments.size() > 1) {
            throw new ApiException(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_AMBIGUOUS);
        }
        return assignments.size() == 1 && assignments.getFirst().getAssignmentStatus().allowsPolicyAccess();
    }
}

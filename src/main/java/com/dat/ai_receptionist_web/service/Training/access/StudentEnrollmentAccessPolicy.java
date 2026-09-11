package com.dat.ai_receptionist_web.service.Training.access;

import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
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
public class StudentEnrollmentAccessPolicy {
    private final CourseStaffAssignmentRepository assignmentRepository;

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

    public void requireCanManageCoursePeriod(
            AccessContext context,
            UUID courseId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        if (context.hasUnrestrictedRole()) {
            return;
        }
        if (context.activePersonId() == null
                || fromDate == null
                || toDate == null
                || toDate.isBefore(fromDate)
                || !assignmentRepository.existsEffectiveAssignmentForStaffCoursePeriod(
                        context.activePersonId(), courseId, fromDate, toDate)) {
            throw new ApiException(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE);
        }
    }
}
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
public class CourseStaffAssignmentAccessPolicy {
    private final CourseStaffAssignmentRepository repository;

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
        return new TrainingAccessScope(false, false, false, false,
                context.activePersonId() != null);
    }
    public void requireCanManagePeriod(
            AccessContext context,
            UUID courseId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (context.hasUnrestrictedRole()) {
            return;
        }
        if (context.activePersonId() == null
                || courseId == null
                || startDate == null
                || (endDate != null && endDate.isBefore(startDate))
                || !repository.existsManagerAssignmentCoveringPeriod(
                        context.activePersonId(), courseId, startDate, endDate)) {
            throw new ApiException(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE);
        }
    }
}

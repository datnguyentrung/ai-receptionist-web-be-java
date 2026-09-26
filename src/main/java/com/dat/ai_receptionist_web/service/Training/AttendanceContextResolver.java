package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.dto.Training.CheckInCandidate;
import com.dat.ai_receptionist_web.enums.Training.AssignmentType;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.repository.Training.AttendanceContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceContextResolver {
    private final AttendanceContextRepository repository;

    public CheckInCandidate resolve(UUID personId, LocalDateTime now) {
        var candidates = repository.findCheckInCandidates(personId)
                .stream()
                .map(this::toCandidate)
                .toList();
        if (candidates.isEmpty()) {
            throw new ApiException(TrainingErrorCode.FACE_CHECK_IN_NO_ACTIVE_CONTEXT);
        }
        if (candidates.size() > 1) {
            throw new ApiException(TrainingErrorCode.FACE_CHECK_IN_AMBIGUOUS_CONTEXT);
        }
        return candidates.getFirst();
    }

    private CheckInCandidate toCandidate(AttendanceContextRepository.CheckInCandidateRow row) {
        return new CheckInCandidate(
                CheckInCandidate.ContextType.valueOf(row.getContextType()),
                row.getClassSessionId(),
                row.getParticipationId(),
                row.getAssignmentType() == null ? null : AssignmentType.valueOf(row.getAssignmentType()),
                row.getSessionDate(),
                row.getStartTime(),
                row.getEndTime()
        );
    }
}

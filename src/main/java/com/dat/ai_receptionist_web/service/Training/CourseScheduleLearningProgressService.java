package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.dto.Training.ClassSessionDTO;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Aggregates every session status within each course schedule's effective date range. */
@Service
@RequiredArgsConstructor
public class CourseScheduleLearningProgressService {
    private static final ClassSessionDTO.LearningProgress EMPTY =
            new ClassSessionDTO.LearningProgress(0, 0, 0, 0, 0, 0, 0);

    private final ClassSessionRepository classSessionRepository;

    @Transactional(readOnly = true)
    public ClassSessionDTO.LearningProgress getByCourseScheduleId(UUID courseScheduleId) {
        ClassSessionRepository.LearningProgressRow row =
                classSessionRepository.findLearningProgressByCourseScheduleId(courseScheduleId);
        return row == null ? EMPTY : toLearningProgress(row);
    }

    @Transactional(readOnly = true)
    public Map<UUID, ClassSessionDTO.LearningProgress> getByCourseScheduleIds(
            Collection<UUID> courseScheduleIds
    ) {
        if (courseScheduleIds == null || courseScheduleIds.isEmpty()) {
            return Map.of();
        }
        List<UUID> distinctIds = courseScheduleIds.stream().distinct().toList();
        Map<UUID, ClassSessionDTO.LearningProgress> progressByScheduleId =
                classSessionRepository.findLearningProgressByCourseScheduleIds(distinctIds).stream()
                        .collect(Collectors.toMap(
                                ClassSessionRepository.CourseScheduleLearningProgressRow::getCourseScheduleId,
                                CourseScheduleLearningProgressService::toLearningProgress,
                                (left, right) -> left,
                                LinkedHashMap::new));
        distinctIds.forEach(id -> progressByScheduleId.putIfAbsent(id, EMPTY));
        return progressByScheduleId;
    }

    private static ClassSessionDTO.LearningProgress toLearningProgress(
            ClassSessionRepository.LearningProgressRow row
    ) {
        return new ClassSessionDTO.LearningProgress(
                row.getScheduled(), row.getActive(), row.getCompleted(), row.getTerminated(),
                row.getCancelled(), row.getPostponed(), row.getTotal());
    }
}

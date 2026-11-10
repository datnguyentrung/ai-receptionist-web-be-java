package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseScheduleLearningProgressServiceTest {
    @Test
    void loadsAllScheduleProgressInOneBatchAndDefaultsSchedulesWithoutSessionsToZero() {
        ClassSessionRepository repository = mock(ClassSessionRepository.class);
        CourseScheduleLearningProgressService service = new CourseScheduleLearningProgressService(repository);
        UUID withSessions = UUID.randomUUID();
        UUID withoutSessions = UUID.randomUUID();
        ClassSessionRepository.CourseScheduleLearningProgressRow row =
                mock(ClassSessionRepository.CourseScheduleLearningProgressRow.class);
        when(row.getCourseScheduleId()).thenReturn(withSessions);
        when(row.getScheduled()).thenReturn(3L);
        when(row.getActive()).thenReturn(1L);
        when(row.getCompleted()).thenReturn(8L);
        when(row.getTerminated()).thenReturn(2L);
        when(row.getCancelled()).thenReturn(1L);
        when(row.getPostponed()).thenReturn(4L);
        when(row.getTotal()).thenReturn(19L);
        when(repository.findLearningProgressByCourseScheduleIds(List.of(withSessions, withoutSessions)))
                .thenReturn(List.of(row));

        Map<UUID, com.dat.ai_receptionist_web.dto.Training.ClassSessionDTO.LearningProgress> result =
                service.getByCourseScheduleIds(List.of(withSessions, withoutSessions));

        assertThat(result.get(withSessions)).hasFieldOrPropertyWithValue("scheduled", 3L)
                .hasFieldOrPropertyWithValue("completed", 8L)
                .hasFieldOrPropertyWithValue("total", 19L);
        assertThat(result.get(withoutSessions)).hasFieldOrPropertyWithValue("total", 0L);
    }
}

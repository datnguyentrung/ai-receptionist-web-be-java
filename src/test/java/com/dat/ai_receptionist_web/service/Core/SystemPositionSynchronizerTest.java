package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.Position;
import com.dat.ai_receptionist_web.repository.Core.PositionRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemPositionSynchronizerTest {
    private final PositionRepository positionRepository = mock(PositionRepository.class);
    private final SystemPositionSynchronizer synchronizer = new SystemPositionSynchronizer(positionRepository);

    @Test
    void createsStudentPositionWhenItIsMissing() {
        when(positionRepository.findByCodeIgnoreCase("STUDENT")).thenReturn(Optional.empty());

        boolean created = synchronizer.syncStudentPosition();

        assertThat(created).isTrue();
        verify(positionRepository).upsertSystemPosition(
                SystemPositionSynchronizer.STUDENT_POSITION_ID,
                "STUDENT",
                "Học viên",
                "Student business position");
    }

    @Test
    void upsertsExistingStudentPosition() {
        when(positionRepository.findByCodeIgnoreCase("STUDENT"))
                .thenReturn(Optional.of(mock(Position.class)));

        boolean created = synchronizer.syncStudentPosition();

        assertThat(created).isFalse();
        verify(positionRepository).upsertSystemPosition(
                SystemPositionSynchronizer.STUDENT_POSITION_ID,
                "STUDENT",
                "Học viên",
                "Student business position");
    }
}

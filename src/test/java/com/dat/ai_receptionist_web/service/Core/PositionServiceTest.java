package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.domain.Core.Position;
import com.dat.ai_receptionist_web.dto.Core.PositionDTO;
import com.dat.ai_receptionist_web.mapper.Core.PositionMapper;
import com.dat.ai_receptionist_web.repository.Core.PositionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PositionServiceTest {
    @Test
    void listsPositionsWithBatchPersonCounts() {
        PositionRepository repository = mock(PositionRepository.class);
        PositionMapper mapper = mock(PositionMapper.class);
        Pageable pageable = Pageable.unpaged();
        Position coach = position("COACH");
        Position guardian = position("GUARDIAN");
        PositionDTO.SimpleResponse coachResponse = simpleResponse(coach, 3L);
        PositionDTO.SimpleResponse guardianResponse = simpleResponse(guardian, 0L);
        PositionRepository.PersonCountByPosition countRow = mock(PositionRepository.PersonCountByPosition.class);

        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(coach, guardian)));
        when(countRow.getPositionId()).thenReturn(coach.getPositionId());
        when(countRow.getPersonCount()).thenReturn(3L);
        when(repository.countPersonsByPositionIds(anyCollection())).thenReturn(List.of(countRow));
        when(mapper.toSimpleResponse(coach, 3L)).thenReturn(coachResponse);
        when(mapper.toSimpleResponse(guardian, 0L)).thenReturn(guardianResponse);

        PositionService service = new PositionService(repository, mapper);

        var result = service.list(pageable);

        assertThat(result.getContent()).containsExactly(coachResponse, guardianResponse);
        verify(repository).countPersonsByPositionIds(argThat(ids ->
                ids.contains(coach.getPositionId()) && ids.contains(guardian.getPositionId()) && ids.size() == 2));
        verify(mapper).toSimpleResponse(eq(coach), eq(3L));
        verify(mapper).toSimpleResponse(eq(guardian), eq(0L));
    }

    @Test
    void getsPositionWithPersonCount() {
        PositionRepository repository = mock(PositionRepository.class);
        PositionMapper mapper = mock(PositionMapper.class);
        Position position = position("COACH");
        PositionDTO.Response response = new PositionDTO.Response(position.getPositionId(), "COACH", "COACH",
                null, true, 5L, null, null);

        when(repository.findById(position.getPositionId())).thenReturn(java.util.Optional.of(position));
        when(repository.countPersonsByPositionId(position.getPositionId())).thenReturn(5L);
        when(mapper.toResponse(position, 5L)).thenReturn(response);

        PositionService service = new PositionService(repository, mapper);

        assertThat(service.get(position.getPositionId())).isEqualTo(response);
    }

    private static Position position(String code) {
        return Position.builder()
                .positionId(UUID.randomUUID())
                .code(code)
                .name(code)
                .active(true)
                .build();
    }

    private static PositionDTO.SimpleResponse simpleResponse(Position position, long personCount) {
        return new PositionDTO.SimpleResponse(position.getPositionId(), position.getCode(), position.getName(),
                position.isActive(), personCount);
    }
}

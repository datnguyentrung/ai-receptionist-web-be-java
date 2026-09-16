package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Training.SessionAttendanceDTO;
import com.dat.ai_receptionist_web.mapper.Training.SessionAttendanceMapper;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
import com.dat.ai_receptionist_web.repository.Training.SessionAttendanceRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.service.Core.PersonCodePolicy;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import com.dat.ai_receptionist_web.service.Training.access.SessionAttendanceAccessPolicy;
import com.dat.ai_receptionist_web.service.Training.access.TrainingAccessScope;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SessionAttendanceServiceTest {
    private final SessionAttendanceRepository repository = mock(SessionAttendanceRepository.class);
    private final CurrentAccessContextResolver currentAccessContextResolver = mock(CurrentAccessContextResolver.class);
    private final SessionAttendanceAccessPolicy accessPolicy = mock(SessionAttendanceAccessPolicy.class);

    private final SessionAttendanceService service = new SessionAttendanceService(
            repository,
            mock(SessionAttendanceMapper.class),
            mock(ClassSessionRepository.class),
            mock(StudentEnrollmentRepository.class),
            mock(CourseStaffAssignmentRepository.class),
            mock(PersonCodePolicy.class),
            currentAccessContextResolver,
            accessPolicy
    );

    @Test
    void listAppliesStableCreatedAtSortWhenClientDoesNotSort() {
        UUID userId = UUID.randomUUID();
        UUID activePersonId = UUID.randomUUID();
        when(currentAccessContextResolver.current()).thenReturn(new AccessContext(
                userId, UUID.randomUUID(), activePersonId, null, Set.of(), Set.of()));
        when(accessPolicy.resolveReadScope(any())).thenReturn(new TrainingAccessScope(false, true, false, true, false));
        when(repository.findAccessible(
                any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                any(), any(), any(), any(), any(), any()
        )).thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(11), 0));

        PageResponse<SessionAttendanceDTO.SimpleResponse> ignored = service.list(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 9, 30),
                null,
                null,
                null,
                PageRequest.of(0, 50)
        );

        verify(repository).findAccessible(
                eq(userId),
                eq(activePersonId),
                eq(false),
                eq(true),
                eq(false),
                eq(true),
                eq(LocalDate.of(2026, 7, 1)),
                eq(LocalDate.of(2026, 9, 30)),
                isNull(),
                isNull(),
                isNull(),
                argThat(pageable -> pageable.getSort().equals(Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("sessionAttendanceId")
                )))
        );
        assertThat(ignored.getContent()).isEmpty();
    }

    @Test
    void listKeepsClientSortWhenProvided() {
        UUID userId = UUID.randomUUID();
        UUID activePersonId = UUID.randomUUID();
        Pageable clientPageable = PageRequest.of(1, 25, Sort.by(Sort.Order.asc("checkInTime")));
        when(currentAccessContextResolver.current()).thenReturn(new AccessContext(
                userId, UUID.randomUUID(), activePersonId, null, Set.of(), Set.of()));
        when(accessPolicy.resolveReadScope(any())).thenReturn(new TrainingAccessScope(false, true, false, true, false));
        when(repository.findAccessible(
                any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                any(), any(), any(), any(), any(), any()
        )).thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(11), 0));

        service.list(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 9, 30),
                null,
                null,
                null,
                clientPageable
        );

        verify(repository).findAccessible(
                any(),
                any(),
                anyBoolean(),
                anyBoolean(),
                anyBoolean(),
                anyBoolean(),
                any(),
                any(),
                any(),
                any(),
                any(),
                same(clientPageable)
        );
    }
}

package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Training.SessionAttendanceDTO;
import com.dat.ai_receptionist_web.dto.Training.SessionAttendanceFilter;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.GeneralErrorCode;
import com.dat.ai_receptionist_web.mapper.Training.SessionAttendanceMapper;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
import com.dat.ai_receptionist_web.repository.Training.SessionAttendanceQueryRepository.AttendanceStatsRow;
import com.dat.ai_receptionist_web.repository.Training.SessionAttendanceRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.service.Core.PersonCodePolicy;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import com.dat.ai_receptionist_web.service.Training.access.SessionAttendanceAccessPolicy;
import com.dat.ai_receptionist_web.service.Training.access.TrainingAccessScope;
import com.dat.ai_receptionist_web.service.Training.command.AttendanceCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
            accessPolicy,
            mock(AttendanceCommandService.class),
            new ObjectMapper()
    );

    @BeforeEach
    void setUpAccess() {
        when(currentAccessContextResolver.current()).thenReturn(new AccessContext(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, Set.of(), Set.of()));
        when(accessPolicy.resolveReadScope(any()))
                .thenReturn(new TrainingAccessScope(false, true, false, true, false));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listAppliesStableCreatedAtSortWhenClientDoesNotSort() {
        when(repository.findAll(
                any(Specification.class), any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(2), 0));

        PageResponse<SessionAttendanceDTO.SimpleResponse> result = service.list(
                filter(), PageRequest.of(0, 50));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(
                any(Specification.class), any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("sessionAttendanceId")
        ));
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void listMapsAllowedSessionDateSortToEntityPath() {
        when(repository.findAll(
                any(Specification.class), any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(2), 0));

        service.list(filter(), PageRequest.of(1, 25, Sort.by("sessionDate").ascending()));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(
                any(Specification.class), any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getSort())
                .isEqualTo(Sort.by("classSession.sessionDate").ascending());
    }

    @Test
    void listRejectsUnsupportedSortProperty() {
        assertThatThrownBy(() -> service.list(
                filter(), PageRequest.of(0, 25, Sort.by("studentEnrollment.studentPerson.fullName"))))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(GeneralErrorCode.INVALID_REQUEST_PARAMETER));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listWithStatsUsesAggregateForEntireFilteredSet() {
        when(repository.findAll(
                any(Specification.class), any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(2), 0));
        when(repository.summarize(any())).thenReturn(new AttendanceStatsRow(
                20, 10, 4, 1, 2, 3, 5, 4, 3, 8));

        SessionAttendanceDTO.AttendanceListResponse result =
                service.listWithStats(filter(), PageRequest.of(0, 5));

        assertThat(result.stats().totalRecords()).isEqualTo(20);
        assertThat(result.stats().attendanceRate()).isEqualTo(75.0);
        assertThat(result.attendances().getContent()).isEmpty();
    }

    private static SessionAttendanceFilter filter() {
        return new SessionAttendanceFilter(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 9, 30),
                null, null, null, null, null, null, null);
    }
}

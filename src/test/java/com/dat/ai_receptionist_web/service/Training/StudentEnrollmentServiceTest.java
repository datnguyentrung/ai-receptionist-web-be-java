package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollmentSchedule;
import com.dat.ai_receptionist_web.dto.Training.StudentEnrollmentDTO;
import com.dat.ai_receptionist_web.mapper.Catalog.CourseScheduleMapper;
import com.dat.ai_receptionist_web.mapper.Training.StudentEnrollmentMapper;
import com.dat.ai_receptionist_web.repository.Catalog.CourseScheduleRepository;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Finance.CoursePurchaseRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentScheduleRepository;
import com.dat.ai_receptionist_web.service.Core.PersonCodePolicy;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import com.dat.ai_receptionist_web.service.Training.access.StudentEnrollmentAccessPolicy;
import com.dat.ai_receptionist_web.service.Training.access.TrainingAccessScope;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StudentEnrollmentServiceTest {
    @Test
    void listLoadsEnrollmentSchedulesInOneBatchInsteadOfPerEnrollment() {
        StudentEnrollmentRepository enrollmentRepository = mock(StudentEnrollmentRepository.class);
        StudentEnrollmentScheduleRepository enrollmentScheduleRepository = mock(StudentEnrollmentScheduleRepository.class);
        StudentEnrollmentMapper enrollmentMapper = mock(StudentEnrollmentMapper.class);
        CourseScheduleMapper courseScheduleMapper = mock(CourseScheduleMapper.class);
        CurrentAccessContextResolver contextResolver = mock(CurrentAccessContextResolver.class);
        StudentEnrollmentAccessPolicy accessPolicy = mock(StudentEnrollmentAccessPolicy.class);
        UUID enrollmentId = UUID.randomUUID();
        AccessContext context = new AccessContext(UUID.randomUUID(), null, UUID.randomUUID(), null, Set.of(), Set.of());
        StudentEnrollment enrollment = StudentEnrollment.builder().studentEnrollmentId(enrollmentId).build();
        StudentEnrollmentSchedule link = StudentEnrollmentSchedule.builder()
                .studentEnrollment(enrollment)
                .courseSchedule(CourseSchedule.builder().courseScheduleId(UUID.randomUUID()).build())
                .build();
        Pageable pageable = Pageable.unpaged();
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now();
        StudentEnrollmentDTO.SimpleResponse response = new StudentEnrollmentDTO.SimpleResponse(
                enrollmentId, null, null, List.of(), from, to, null);

        when(contextResolver.current()).thenReturn(context);
        when(accessPolicy.resolveReadScope(context)).thenReturn(
                new TrainingAccessScope(true, false, false, false, false));
        when(enrollmentRepository.findAccessible(
                eq(context.userId()), eq(context.activePersonId()), eq(true), eq(false), eq(false), eq(false),
                eq(from), eq(to), isNull(), isNull(), isNull(), eq(""), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(enrollment)));
        when(enrollmentScheduleRepository.findDetailedByStudentEnrollmentIds(List.of(enrollmentId)))
                .thenReturn(List.of(link));
        when(enrollmentMapper.toSimpleResponse(eq(enrollment), anyList())).thenReturn(response);

        StudentEnrollmentService service = new StudentEnrollmentService(
                enrollmentRepository,
                enrollmentMapper,
                mock(PersonRepository.class),
                mock(CoursePurchaseRepository.class),
                mock(CourseScheduleRepository.class),
                enrollmentScheduleRepository,
                courseScheduleMapper,
                mock(PersonCodePolicy.class),
                contextResolver,
                accessPolicy);

        var result = service.list(from, to, null, null, null, null, pageable);

        assertThat(result.getContent()).containsExactly(response);
        verify(enrollmentScheduleRepository).findDetailedByStudentEnrollmentIds(List.of(enrollmentId));
        verify(enrollmentMapper).toSimpleResponse(eq(enrollment), anyList());
        verify(enrollmentMapper, never()).toSimpleResponse(enrollment);
    }
}

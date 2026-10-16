package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.dto.Training.CourseStaffAssignmentDTO;
import com.dat.ai_receptionist_web.enums.Training.AssignmentType;
import com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.mapper.Training.CourseStaffAssignmentMapper;
import com.dat.ai_receptionist_web.repository.Catalog.CourseScheduleRepository;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
import com.dat.ai_receptionist_web.service.Core.PersonCodePolicy;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Training.access.CourseStaffAssignmentAccessPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CourseStaffAssignmentServiceTest {
    @Test
    void createRejectsOverlappingPrimaryCoachAssignment() {
        CourseStaffAssignmentRepository repository = mock(CourseStaffAssignmentRepository.class);
        CourseScheduleRepository scheduleRepository = mock(CourseScheduleRepository.class);
        CurrentAccessContextResolver contextResolver = mock(CurrentAccessContextResolver.class);
        CourseStaffAssignmentAccessPolicy accessPolicy = mock(CourseStaffAssignmentAccessPolicy.class);
        UUID scheduleId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        LocalDate start = LocalDate.of(2026, 10, 16);
        LocalDate end = LocalDate.of(2026, 11, 16);
        CourseSchedule schedule = CourseSchedule.builder()
                .courseScheduleId(scheduleId)
                .course(Course.builder().courseId(courseId).build())
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .build();
        AccessContext context = new AccessContext(UUID.randomUUID(), null, UUID.randomUUID(), null, Set.of(), Set.of());
        CourseStaffAssignmentService service = new CourseStaffAssignmentService(
                repository,
                mock(CourseStaffAssignmentMapper.class),
                mock(PersonRepository.class),
                scheduleRepository,
                mock(PersonCodePolicy.class),
                contextResolver,
                accessPolicy
        );
        when(contextResolver.current()).thenReturn(context);
        when(scheduleRepository.findDetailedById(scheduleId)).thenReturn(Optional.of(schedule));
        when(repository.existsOverlappingPrimaryAssignment(scheduleId, start, end, null)).thenReturn(true);

        var request = new CourseStaffAssignmentDTO.CreateRequest(
                personId, scheduleId, AssignmentType.PRIMARY_COACH, start, end,
                CourseStaffAssignmentStatus.ACTIVE, "");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(TrainingErrorCode.PRIMARY_COACH_ASSIGNMENT_CONFLICT));
        verify(repository, never()).save(any());
    }

    @Test
    void groupsEffectiveStaffByScheduleAndOrdersRolesWithPrimaryCoachFirst() {
        CourseStaffAssignmentRepository repository = mock(CourseStaffAssignmentRepository.class);
        CourseStaffAssignmentMapper mapper = mock(CourseStaffAssignmentMapper.class);
        CourseStaffAssignmentService service = new CourseStaffAssignmentService(
                repository,
                mapper,
                mock(PersonRepository.class),
                mock(CourseScheduleRepository.class),
                mock(PersonCodePolicy.class),
                mock(CurrentAccessContextResolver.class),
                mock(CourseStaffAssignmentAccessPolicy.class)
        );
        LocalDate today = LocalDate.of(2026, 10, 16);
        UUID firstScheduleId = UUID.randomUUID();
        UUID secondScheduleId = UUID.randomUUID();
        CourseStaffAssignment manager = assignment(firstScheduleId, "Manager", AssignmentType.MANAGER);
        CourseStaffAssignment assistant = assignment(firstScheduleId, "Assistant", AssignmentType.ASSISTANT_COACH);
        CourseStaffAssignment primary = assignment(firstScheduleId, "Primary", AssignmentType.PRIMARY_COACH);
        CourseStaffAssignment teachingAssistant = assignment(secondScheduleId, "Teaching", AssignmentType.TEACHING_ASSISTANT);

        when(repository.findEffectiveByCourseScheduleIdsAndDate(List.of(firstScheduleId, secondScheduleId), today))
                .thenReturn(List.of(manager, teachingAssistant, assistant, primary));
        for (CourseStaffAssignment assignment : List.of(manager, assistant, primary, teachingAssistant)) {
            when(mapper.toBriefResponse(assignment)).thenReturn(new CourseStaffAssignmentDTO.BriefResponse(
                    assignment.getCourseStaffAssignmentId(),
                    null,
                    assignment.getAssignmentType()
            ));
        }

        var result = service.getEffectiveBriefByCourseScheduleIds(
                List.of(firstScheduleId, secondScheduleId), today);

        assertThat(result.get(firstScheduleId))
                .extracting(CourseStaffAssignmentDTO.BriefResponse::assignmentType)
                .containsExactly(AssignmentType.PRIMARY_COACH, AssignmentType.ASSISTANT_COACH, AssignmentType.MANAGER);
        assertThat(result.get(secondScheduleId))
                .extracting(CourseStaffAssignmentDTO.BriefResponse::assignmentType)
                .containsExactly(AssignmentType.TEACHING_ASSISTANT);
    }

    private CourseStaffAssignment assignment(UUID scheduleId, String name, AssignmentType type) {
        return CourseStaffAssignment.builder()
                .courseStaffAssignmentId(UUID.randomUUID())
                .courseSchedule(CourseSchedule.builder().courseScheduleId(scheduleId).build())
                .staffPerson(Person.builder().personId(UUID.randomUUID()).fullName(name).build())
                .assignmentType(type)
                .build();
    }
}

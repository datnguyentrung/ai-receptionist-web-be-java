package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.Training.SessionAttendanceDTO;
import com.dat.ai_receptionist_web.enums.Training.AssignmentType;
import com.dat.ai_receptionist_web.enums.Training.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus;
import com.dat.ai_receptionist_web.enums.Training.EvaluationStatus;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus;
import com.dat.ai_receptionist_web.mapper.Training.SessionAttendanceMapper;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
import com.dat.ai_receptionist_web.repository.Training.SessionAttendanceRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.service.Core.PersonCodePolicy;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import com.dat.ai_receptionist_web.service.Training.access.SessionAttendanceAccessPolicy;
import com.dat.ai_receptionist_web.service.Training.command.AttendanceCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SessionAttendanceCreateServiceTest {
    private final SessionAttendanceRepository repository = mock(SessionAttendanceRepository.class);
    private final SessionAttendanceMapper mapper = mock(SessionAttendanceMapper.class);
    private final ClassSessionRepository classSessionRepository = mock(ClassSessionRepository.class);
    private final StudentEnrollmentRepository studentEnrollmentRepository = mock(StudentEnrollmentRepository.class);
    private final CourseStaffAssignmentRepository courseStaffAssignmentRepository =
            mock(CourseStaffAssignmentRepository.class);
    private final PersonCodePolicy personCodePolicy = mock(PersonCodePolicy.class);
    private final CurrentAccessContextResolver currentAccessContextResolver =
            mock(CurrentAccessContextResolver.class);
    private final SessionAttendanceAccessPolicy accessPolicy = mock(SessionAttendanceAccessPolicy.class);
    private final SessionAttendanceService service = new SessionAttendanceService(
            repository,
            mapper,
            classSessionRepository,
            studentEnrollmentRepository,
            courseStaffAssignmentRepository,
            personCodePolicy,
            currentAccessContextResolver,
            accessPolicy,
            mock(AttendanceCommandService.class),
            new ObjectMapper()
    );

    @BeforeEach
    void setUpAccessContext() {
        when(currentAccessContextResolver.current()).thenReturn(new AccessContext(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, Set.of(), Set.of()));
    }

    @Test
    void createReturnsBriefResponseUsingPreloadedGraph() {
        UUID attendanceId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        UUID classSessionId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        UUID studentPersonId = UUID.randomUUID();
        LocalDate sessionDate = LocalDate.of(2026, 11, 10);
        LocalTime startTime = LocalTime.of(18, 0);
        LocalTime endTime = LocalTime.of(19, 30);
        LocalDateTime checkInTime = LocalDateTime.of(sessionDate, startTime.plusMinutes(10));
        LocalDateTime createdAt = LocalDateTime.now();

        Course course = Course.builder().courseId(courseId).name("Taekwondo Kids").build();
        CourseSchedule schedule = CourseSchedule.builder()
                .courseScheduleId(scheduleId)
                .course(course)
                .build();
        ClassSession session = ClassSession.builder()
                .classSessionId(classSessionId)
                .course(course)
                .courseSchedule(schedule)
                .sessionDate(sessionDate)
                .startTime(startTime)
                .endTime(endTime)
                .status(SessionStatus.ACTIVE)
                .build();
        Person student = Person.builder()
                .personId(studentPersonId)
                .fullName("Nguyen Van A")
                .personCode("VQ_001")
                .build();
        StudentEnrollment enrollment = StudentEnrollment.builder()
                .studentEnrollmentId(enrollmentId)
                .studentPerson(student)
                .schedules(new LinkedHashSet<>())
                .startDate(sessionDate.minusMonths(1))
                .endDate(sessionDate.plusMonths(1))
                .status(StudentEnrollmentStatus.ACTIVE)
                .build();

        when(classSessionRepository.findDetailedById(classSessionId)).thenReturn(Optional.of(session));
        when(studentEnrollmentRepository.findDetailedForAttendanceById(enrollmentId))
                .thenReturn(Optional.of(enrollment));
        when(repository.save(any(SessionAttendance.class))).thenAnswer(invocation -> {
            SessionAttendance saved = invocation.getArgument(0);
            saved.setSessionAttendanceId(attendanceId);
            saved.setCreatedAt(createdAt);
            return saved;
        });

        SessionAttendanceDTO.BriefResponse response = service.create(
                new SessionAttendanceDTO.CreateRequest(
                        classSessionId,
                        enrollmentId,
                        null,
                        checkInTime,
                        AttendanceStatus.PRESENT,
                        EvaluationStatus.PENDING,
                        "Checked in at reception"
                ));

        assertThat(response.sessionAttendanceId()).isEqualTo(attendanceId);
        assertThat(response.classSession().classSessionId()).isEqualTo(classSessionId);
        assertThat(response.classSession().course().courseId()).isEqualTo(courseId);
        assertThat(response.classSession().course().name()).isEqualTo("Taekwondo Kids");
        assertThat(response.classSession().sessionDate()).isEqualTo(sessionDate);
        assertThat(response.classSession().startTime()).isEqualTo(startTime);
        assertThat(response.classSession().endTime()).isEqualTo(endTime);
        assertThat(response.studentEnrollmentId()).isEqualTo(enrollmentId);
        assertThat(response.courseStaffAssignmentId()).isNull();
        assertThat(response.participantPersonId()).isEqualTo(studentPersonId);
        assertThat(response.participantName()).isEqualTo("Nguyen Van A");
        assertThat(response.checkInTime()).isEqualTo(checkInTime);
        assertThat(response.createdAt()).isEqualTo(createdAt);

        verify(classSessionRepository).findDetailedById(classSessionId);
        verify(studentEnrollmentRepository).findDetailedForAttendanceById(enrollmentId);
        verify(courseStaffAssignmentRepository, never()).findDetailedForAttendanceById(any());
        verify(repository).save(any(SessionAttendance.class));
        verifyNoInteractions(mapper);
    }

    @Test
    void createReturnsAssistantParticipantBrief() {
        UUID attendanceId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        UUID classSessionId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID staffPersonId = UUID.randomUUID();
        LocalDate sessionDate = LocalDate.of(2026, 11, 10);

        Course course = Course.builder().courseId(courseId).name("Taekwondo Kids").build();
        CourseSchedule schedule = CourseSchedule.builder()
                .courseScheduleId(scheduleId)
                .course(course)
                .build();
        ClassSession session = ClassSession.builder()
                .classSessionId(classSessionId)
                .course(course)
                .courseSchedule(schedule)
                .sessionDate(sessionDate)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 30))
                .status(SessionStatus.SCHEDULED)
                .build();
        Person staff = Person.builder()
                .personId(staffPersonId)
                .fullName("Tran Coach")
                .personCode("VQ_002")
                .build();
        CourseStaffAssignment assignment = CourseStaffAssignment.builder()
                .courseStaffAssignmentId(assignmentId)
                .staffPerson(staff)
                .courseSchedule(schedule)
                .assignmentType(AssignmentType.ASSISTANT_COACH)
                .assignmentStatus(CourseStaffAssignmentStatus.ACTIVE)
                .startDate(sessionDate.minusMonths(1))
                .endDate(sessionDate.plusMonths(1))
                .build();

        when(classSessionRepository.findDetailedById(classSessionId)).thenReturn(Optional.of(session));
        when(courseStaffAssignmentRepository.findDetailedForAttendanceById(assignmentId))
                .thenReturn(Optional.of(assignment));
        when(repository.save(any(SessionAttendance.class))).thenAnswer(invocation -> {
            SessionAttendance saved = invocation.getArgument(0);
            saved.setSessionAttendanceId(attendanceId);
            return saved;
        });

        SessionAttendanceDTO.BriefResponse response = service.create(
                new SessionAttendanceDTO.CreateRequest(
                        classSessionId,
                        null,
                        assignmentId,
                        LocalDateTime.of(sessionDate, LocalTime.of(18, 5)),
                        AttendanceStatus.PRESENT,
                        EvaluationStatus.PENDING,
                        null
                ));

        assertThat(response.studentEnrollmentId()).isNull();
        assertThat(response.courseStaffAssignmentId()).isEqualTo(assignmentId);
        assertThat(response.participantPersonId()).isEqualTo(staffPersonId);
        assertThat(response.participantName()).isEqualTo("Tran Coach");
        assertThat(response.classSession().course().name()).isEqualTo("Taekwondo Kids");

        verify(studentEnrollmentRepository, never()).findDetailedForAttendanceById(any());
        verify(courseStaffAssignmentRepository).findDetailedForAttendanceById(assignmentId);
    }
}

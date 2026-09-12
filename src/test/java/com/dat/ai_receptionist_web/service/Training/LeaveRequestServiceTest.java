package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.LeaveRequest;
import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.Training.LeaveRequestDTO;
import com.dat.ai_receptionist_web.enums.Training.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Training.LeaveRequestStatus;
import com.dat.ai_receptionist_web.enums.Training.RequesterType;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.GeneralErrorCode;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.mapper.Training.LeaveRequestMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.LeaveRequestRepository;
import com.dat.ai_receptionist_web.repository.Training.SessionAttendanceRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LeaveRequestServiceTest {
    private LeaveRequestRepository repository;
    private PersonRepository personRepository;
    private UserRepository userRepository;
    private ClassSessionRepository classSessionRepository;
    private StudentEnrollmentRepository enrollmentRepository;
    private SessionAttendanceRepository attendanceRepository;
    private LeaveRequestService service;

    private UUID userId;

    @BeforeEach
    void setUp() {
        repository = mock(LeaveRequestRepository.class);
        personRepository = mock(PersonRepository.class);
        userRepository = mock(UserRepository.class);
        classSessionRepository = mock(ClassSessionRepository.class);
        enrollmentRepository = mock(StudentEnrollmentRepository.class);
        attendanceRepository = mock(SessionAttendanceRepository.class);
        service = new LeaveRequestService(
                repository, mock(LeaveRequestMapper.class), personRepository, userRepository,
                classSessionRepository, enrollmentRepository, attendanceRepository);

        userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS512")
                .subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, null, List.of()));
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(User.builder().userId(userId).build()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void approveCreatesExcusedAndMakeupAttendanceAtomically() {
        Course course = Course.builder().courseId(UUID.randomUUID()).build();
        ClassSession leaveSession = ClassSession.builder().classSessionId(UUID.randomUUID())
                .course(course).sessionDate(LocalDate.of(2026, 8, 31)).build();
        ClassSession makeupSession = ClassSession.builder().classSessionId(UUID.randomUUID())
                .course(course).sessionDate(LocalDate.of(2026, 9, 7)).build();
        LeaveRequest request = LeaveRequest.builder().leaveRequestId(UUID.randomUUID())
                .person(Person.builder().personId(UUID.randomUUID()).build())
                .requesterType(RequesterType.STUDENT)
                .leaveClassSession(leaveSession)
                .makeupClassSession(makeupSession)
                .status(LeaveRequestStatus.PENDING).build();
        StudentEnrollment enrollment = StudentEnrollment.builder()
                .studentEnrollmentId(UUID.randomUUID()).build();

        when(repository.findByIdForUpdate(request.getLeaveRequestId()))
                .thenReturn(Optional.of(request));
        when(enrollmentRepository.findActiveEnrollmentForCourseOnDate(
                any(), any(), any())).thenReturn(Optional.of(enrollment));
        when(attendanceRepository.existsByClassSession_ClassSessionIdAndStudentEnrollment_StudentEnrollmentId(
                any(), any())).thenReturn(false);

        service.approve(request.getLeaveRequestId(), "ok");

        assertThat(request.getStatus()).isEqualTo(LeaveRequestStatus.APPROVED);
        assertThat(request.getReviewedByUser()).isNotNull();
        assertThat(request.getReviewedAt()).isNotNull();
        ArgumentCaptor<SessionAttendance> captor = ArgumentCaptor.forClass(SessionAttendance.class);
        verify(attendanceRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(SessionAttendance::getAttendanceStatus)
                .containsExactlyInAnyOrder(AttendanceStatus.EXCUSED, AttendanceStatus.MAKEUP);
    }

    @Test
    void approveSystemEmployeeLeaveDoesNotTouchAttendance() {
        LeaveRequest request = LeaveRequest.builder().leaveRequestId(UUID.randomUUID())
                .person(Person.builder().personId(UUID.randomUUID()).build())
                .requesterType(RequesterType.SYSTEM_EMPLOYEE)
                .status(LeaveRequestStatus.PENDING).build();
        when(repository.findByIdForUpdate(request.getLeaveRequestId()))
                .thenReturn(Optional.of(request));

        service.approve(request.getLeaveRequestId(), null);

        assertThat(request.getStatus()).isEqualTo(LeaveRequestStatus.APPROVED);
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void processingAnApprovedRequestIsRejected() {
        LeaveRequest request = LeaveRequest.builder().leaveRequestId(UUID.randomUUID())
                .person(Person.builder().personId(UUID.randomUUID()).build())
                .requesterType(RequesterType.STUDENT)
                .status(LeaveRequestStatus.APPROVED).build();
        when(repository.findByIdForUpdate(request.getLeaveRequestId()))
                .thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(request.getLeaveRequestId(), null))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(TrainingErrorCode.LEAVE_REQUEST_ALREADY_PROCESSED));
    }

    @Test
    void studentCreateRequiresLeaveAndMakeupSessions() {
        LeaveRequestDTO.CreateRequest request = new LeaveRequestDTO.CreateRequest(
                UUID.randomUUID(), RequesterType.STUDENT, null, null, null, "context");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(GeneralErrorCode.INVALID_REQUEST_BODY));
    }
}

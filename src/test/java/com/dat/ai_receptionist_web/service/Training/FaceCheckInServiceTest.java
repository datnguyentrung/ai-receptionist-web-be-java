package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.CoachTimesheet;
import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.dto.Training.CheckInCandidate;
import com.dat.ai_receptionist_web.dto.Training.FaceCheckInResponse;
import com.dat.ai_receptionist_web.enums.Training.AssignmentType;
import com.dat.ai_receptionist_web.enums.Training.AttendanceStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Training.CoachTimesheetRepository;
import com.dat.ai_receptionist_web.repository.Training.SessionAttendanceRepository;
import com.dat.ai_receptionist_web.service.Core.PersonFaceImageUrlResolver;
import com.dat.ai_receptionist_web.service.Training.command.AttendanceCommandService;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FaceCheckInServiceTest {
    private final FaceIdentificationService faceIdentificationService = mock(FaceIdentificationService.class);
    private final AttendanceContextResolver attendanceContextResolver = mock(AttendanceContextResolver.class);
    private final SessionAttendanceService sessionAttendanceService = mock(SessionAttendanceService.class);
    private final CoachTimesheetService coachTimesheetService = mock(CoachTimesheetService.class);
    private final SessionAttendanceRepository sessionAttendanceRepository = mock(SessionAttendanceRepository.class);
    private final CoachTimesheetRepository coachTimesheetRepository = mock(CoachTimesheetRepository.class);
    private final PersonRepository personRepository = mock(PersonRepository.class);
    private final PersonFaceImageUrlResolver faceImageUrlResolver = mock(PersonFaceImageUrlResolver.class);
    private final AttendanceCommandService attendanceCommandService = mock(AttendanceCommandService.class);

    private final FaceCheckInService service = new FaceCheckInService(
            faceIdentificationService,
            attendanceContextResolver,
            sessionAttendanceService,
            coachTimesheetService,
            sessionAttendanceRepository,
            coachTimesheetRepository,
            personRepository,
            faceImageUrlResolver,
            attendanceCommandService
    );

    @Test
    void checkInIdentifiedPersonReturnsPendingWithConfidenceAndRequestId() {
        UUID personId = UUID.randomUUID();
        float confidence = 0.86f;
        MultipartFile file = mock(MultipartFile.class);
        Person person = person(personId);

        when(faceIdentificationService.identify(file))
                .thenReturn(new FaceIdentificationService.IdentifiedPerson(personId, confidence));
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(faceImageUrlResolver.resolve(personId, person.getFaceImagePath()))
                .thenReturn("https://signed.example/face.jpg");

        FaceCheckInResponse response = service.checkIn(file);

        assertThat(response.status()).isEqualTo(FaceCheckInResponse.Status.PENDING);
        assertThat(response.requestId()).isNotNull();
        assertThat(response.person().personId()).isEqualTo(personId);
        assertThat(response.confidence()).isEqualTo(confidence);
        assertThat(response.error()).isNull();
    }

    @Test
    void queuedStudentCheckInResponseIncludesIdentificationConfidence() {
        UUID personId = UUID.randomUUID();
        UUID classSessionId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        float confidence = 0.86f;
        Person person = person(personId);
        ClassSession session = session(classSessionId);
        SessionAttendance attendance = SessionAttendance.builder()
                .sessionAttendanceId(UUID.randomUUID())
                .classSession(session)
                .checkInTime(LocalDateTime.of(2026, 9, 27, 18, 30))
                .attendanceStatus(AttendanceStatus.PRESENT)
                .build();

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(attendanceContextResolver.resolve(any(), any())).thenReturn(new CheckInCandidate(
                CheckInCandidate.ContextType.STUDENT,
                classSessionId,
                enrollmentId,
                null,
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime()
        ));
        when(sessionAttendanceRepository.findByClassSession_ClassSessionIdAndStudentEnrollment_StudentEnrollmentId(
                classSessionId, enrollmentId)).thenReturn(Optional.empty());
        when(sessionAttendanceService.checkInResolvedStudent(any(), any(), any())).thenReturn(attendance);
        when(faceImageUrlResolver.resolve(personId, person.getFaceImagePath()))
                .thenReturn("https://signed.example/face.jpg");

        FaceCheckInResponse response = service.processQueuedCheckIn(
                personId,
                confidence,
                LocalDateTime.of(2026, 9, 27, 18, 20)
        );

        assertThat(response.status()).isEqualTo(FaceCheckInResponse.Status.SUCCESS);
        assertThat(response.confidence()).isEqualTo(confidence);
        assertThat(response.action()).isEqualTo(FaceCheckInResponse.Action.STUDENT_CHECK_IN);
        assertThat(response.error()).isNull();
    }

    @Test
    void queuedCoachTimesheetResponseIncludesIdentificationConfidence() {
        UUID personId = UUID.randomUUID();
        UUID classSessionId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        float confidence = 0.92f;
        Person person = person(personId);
        ClassSession session = session(classSessionId);
        CoachTimesheet timesheet = CoachTimesheet.builder()
                .coachTimesheetId(UUID.randomUUID())
                .classSession(session)
                .checkInTime(LocalTime.of(18, 25))
                .build();

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(attendanceContextResolver.resolve(any(), any())).thenReturn(new CheckInCandidate(
                CheckInCandidate.ContextType.STAFF,
                classSessionId,
                assignmentId,
                AssignmentType.PRIMARY_COACH,
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime()
        ));
        when(coachTimesheetRepository.findByClassSession_ClassSessionIdAndCourseStaffAssignment_CourseStaffAssignmentId(
                classSessionId, assignmentId)).thenReturn(Optional.empty());
        when(coachTimesheetService.checkInResolvedCoach(any(), any(), any())).thenReturn(timesheet);
        when(faceImageUrlResolver.resolve(personId, person.getFaceImagePath()))
                .thenReturn("https://signed.example/face.jpg");

        FaceCheckInResponse response = service.processQueuedCheckIn(
                personId,
                confidence,
                LocalDateTime.of(2026, 9, 27, 18, 20)
        );

        assertThat(response.status()).isEqualTo(FaceCheckInResponse.Status.SUCCESS);
        assertThat(response.confidence()).isEqualTo(confidence);
        assertThat(response.action()).isEqualTo(FaceCheckInResponse.Action.STAFF_TIMESHEET_CHECKED_IN);
        assertThat(response.error()).isNull();
    }

    @Test
    void queuedNoActiveContextAfterIdentificationReturnsFailedResponseWithPersonAndError() {
        UUID personId = UUID.randomUUID();
        float confidence = 0.79f;
        Person person = person(personId);

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(attendanceContextResolver.resolve(any(), any()))
                .thenThrow(new ApiException(TrainingErrorCode.FACE_CHECK_IN_NO_ACTIVE_CONTEXT));
        when(faceImageUrlResolver.resolve(personId, person.getFaceImagePath()))
                .thenReturn("https://signed.example/face.jpg");

        FaceCheckInResponse response = service.processQueuedCheckIn(
                personId,
                confidence,
                LocalDateTime.of(2026, 9, 27, 18, 20)
        );

        assertThat(response.status()).isEqualTo(FaceCheckInResponse.Status.FAILED);
        assertThat(response.person().personId()).isEqualTo(personId);
        assertThat(response.confidence()).isEqualTo(confidence);
        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo("FACE_CHECK_IN_NO_ACTIVE_CONTEXT");
        assertThat(response.error().detail()).isEqualTo(
                TrainingErrorCode.FACE_CHECK_IN_NO_ACTIVE_CONTEXT.defaultDetail());
    }

    @Test
    void queuedAmbiguousContextAfterIdentificationReturnsFailedResponseWithPersonAndError() {
        UUID personId = UUID.randomUUID();
        float confidence = 0.81f;
        Person person = person(personId);

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(attendanceContextResolver.resolve(any(), any()))
                .thenThrow(new ApiException(TrainingErrorCode.FACE_CHECK_IN_AMBIGUOUS_CONTEXT));
        when(faceImageUrlResolver.resolve(personId, person.getFaceImagePath()))
                .thenReturn("https://signed.example/face.jpg");

        FaceCheckInResponse response = service.processQueuedCheckIn(
                personId,
                confidence,
                LocalDateTime.of(2026, 9, 27, 18, 20)
        );

        assertThat(response.status()).isEqualTo(FaceCheckInResponse.Status.FAILED);
        assertThat(response.person().personId()).isEqualTo(personId);
        assertThat(response.confidence()).isEqualTo(confidence);
        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo("FACE_CHECK_IN_AMBIGUOUS_CONTEXT");
    }

    @Test
    void faceNotMatchedBeforePersonResolutionStillThrowsApiException() {
        MultipartFile file = mock(MultipartFile.class);
        when(faceIdentificationService.identify(file))
                .thenThrow(new ApiException(CoreErrorCode.FACE_NOT_MATCHED));

        assertThatThrownBy(() -> service.checkIn(file))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(CoreErrorCode.FACE_NOT_MATCHED);
    }

    private static Person person(UUID personId) {
        return Person.builder()
                .personId(personId)
                .fullName("Nguyen Van A")
                .personCode("VQ_001")
                .faceImagePath("faces/person.jpg")
                .build();
    }

    private static ClassSession session(UUID classSessionId) {
        Course course = Course.builder()
                .courseId(UUID.randomUUID())
                .name("Taekwondo Basic")
                .build();
        return ClassSession.builder()
                .classSessionId(classSessionId)
                .course(course)
                .sessionDate(LocalDate.of(2026, 9, 27))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 30))
                .build();
    }
}

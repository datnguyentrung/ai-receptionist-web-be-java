package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.CoachTimesheet;
import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.domain.Training.command.AttendanceCommand;
import com.dat.ai_receptionist_web.domain.Training.command.AttendanceCommandStatus;
import com.dat.ai_receptionist_web.domain.Training.command.AttendanceCommandType;
import com.dat.ai_receptionist_web.dto.Training.CheckInCandidate;
import com.dat.ai_receptionist_web.dto.Training.FaceCheckInResponse;
import com.dat.ai_receptionist_web.dto.Training.command.FaceCheckInCommandMessage;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Training.CoachTimesheetRepository;
import com.dat.ai_receptionist_web.repository.Training.SessionAttendanceRepository;
import com.dat.ai_receptionist_web.service.Core.PersonFaceImageUrlResolver;
import com.dat.ai_receptionist_web.service.Training.command.AttendanceCommandService;
import com.dat.ai_receptionist_web.service.Training.command.AttendanceCommandStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FaceCheckInService {
    private final FaceIdentificationService faceIdentificationService;
    private final AttendanceContextResolver attendanceContextResolver;
    private final SessionAttendanceService sessionAttendanceService;
    private final CoachTimesheetService coachTimesheetService;
    private final SessionAttendanceRepository sessionAttendanceRepository;
    private final CoachTimesheetRepository coachTimesheetRepository;
    private final PersonRepository personRepository;
    private final PersonFaceImageUrlResolver faceImageUrlResolver;
    private final AttendanceCommandService attendanceCommandService;
    private final AttendanceCommandStore attendanceCommandStore;
    @Value("${app.face-check-in.expire-after:30s}")
    private Duration expireAfter;

    @Transactional
    public FaceCheckInResponse checkIn(MultipartFile file) {
        LocalDateTime now = LocalDateTime.now();
        var identifiedPerson = faceIdentificationService.identify(file);
        Person person = personRepository.findById(identifiedPerson.personId())
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND));
        UUID requestId = UUID.randomUUID();
        attendanceCommandService.enqueueFaceCheckIn(new FaceCheckInCommandMessage(
                requestId,
                person.getPersonId(),
                identifiedPerson.confidence(),
                now
        ));
        return pendingResponse(person, identifiedPerson.confidence(), requestId);
    }

    @Transactional
    public FaceCheckInResponse get(UUID requestId) {
        AttendanceCommand command = attendanceCommandStore.getEntity(requestId);
        if (command.getCommandType() != AttendanceCommandType.FACE_CHECK_IN) {
            throw new ApiException(TrainingErrorCode.ATTENDANCE_COMMAND_NOT_FOUND);
        }
        expireIfTimedOut(command);
        return switch (command.getStatus()) {
            case QUEUED -> openResponse(command, FaceCheckInResponse.Status.PENDING);
            case PROCESSING -> openResponse(command, FaceCheckInResponse.Status.PROCESSING);
            case SUCCEEDED, REJECTED -> attendanceCommandStore.readResult(command, FaceCheckInResponse.class);
            case FAILED -> failedResponse(command);
            case EXPIRED -> expiredResponse(command);
        };
    }

    @Transactional
    public FaceCheckInResponse processQueuedCheckIn(UUID personId, float confidence, LocalDateTime requestedAt) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND));
        try {
            CheckInCandidate candidate = attendanceContextResolver.resolve(personId, requestedAt);
            return dispatch(person, confidence, candidate, requestedAt);
        } catch (ApiException exception) {
            return rejectedResponse(person, confidence, exception);
        }
    }

    private FaceCheckInResponse dispatch(Person person, float confidence, CheckInCandidate candidate, LocalDateTime now) {
        if (candidate.contextType() == CheckInCandidate.ContextType.STUDENT) {
            boolean existed = sessionAttendanceRepository
                    .findByClassSession_ClassSessionIdAndStudentEnrollment_StudentEnrollmentId(
                            candidate.classSessionId(), candidate.participationId())
                    .isPresent();
            SessionAttendance attendance = sessionAttendanceService.checkInResolvedStudent(
                    candidate.classSessionId(), candidate.participationId(), now);
            if (existed) {
                return attendanceResponse(
                        person,
                        attendance,
                        confidence,
                        FaceCheckInResponse.Status.REJECTED,
                        FaceCheckInResponse.Action.STUDENT_CHECK_IN,
                        error(TrainingErrorCode.FACE_CHECK_IN_ALREADY_CHECKED_IN)
                );
            }
            return attendanceResponse(
                    person,
                    attendance,
                    confidence,
                    FaceCheckInResponse.Status.SUCCESS,
                    FaceCheckInResponse.Action.STUDENT_CHECK_IN,
                    null
            );
        }

        var existing = coachTimesheetRepository
                .findByClassSession_ClassSessionIdAndCourseStaffAssignment_CourseStaffAssignmentId(
                        candidate.classSessionId(), candidate.participationId());
        boolean existed = existing.isPresent();
        boolean existingHadCheckOut = existing
                .map(timesheet -> timesheet.getCheckOutTime() != null)
                .orElse(false);
        CoachTimesheet timesheet = coachTimesheetService.checkInResolvedCoach(
                candidate.classSessionId(), candidate.participationId(), now);
        FaceCheckInResponse.Status status = resolveCoachStatus(existed, existingHadCheckOut, timesheet);
        FaceCheckInResponse.Action action = timesheet.getCheckOutTime() == null
                ? FaceCheckInResponse.Action.STAFF_TIMESHEET_CHECKED_IN
                : FaceCheckInResponse.Action.STAFF_TIMESHEET_CHECKED_OUT;
        return coachResponse(person, timesheet, confidence, status, action, coachError(status, existingHadCheckOut));
    }

    private FaceCheckInResponse.Status resolveCoachStatus(
            boolean existed,
            boolean existingHadCheckOut,
            CoachTimesheet current
    ) {
        if (!existed) {
            return FaceCheckInResponse.Status.SUCCESS;
        }
        if (existingHadCheckOut) {
            return FaceCheckInResponse.Status.REJECTED;
        }
        return current.getCheckOutTime() == null
                ? FaceCheckInResponse.Status.REJECTED
                : FaceCheckInResponse.Status.SUCCESS;
    }

    private FaceCheckInResponse attendanceResponse(
            Person person,
            SessionAttendance attendance,
            float confidence,
            FaceCheckInResponse.Status status,
            FaceCheckInResponse.Action action,
            FaceCheckInResponse.ErrorSummary error
    ) {
        return new FaceCheckInResponse(
                status,
                action,
                personSummary(person),
                sessionSummary(attendance.getClassSession()),
                attendance.getSessionAttendanceId(),
                attendance.getCheckInTime(),
                null,
                null,
                confidence,
                attendance.getAttendanceStatus(),
                error == null ? "Face check-in completed" : error.detail(),
                error
        );
    }

    private FaceCheckInResponse coachResponse(
            Person person,
            CoachTimesheet timesheet,
            float confidence,
            FaceCheckInResponse.Status status,
            FaceCheckInResponse.Action action,
            FaceCheckInResponse.ErrorSummary error
    ) {
        return new FaceCheckInResponse(
                status,
                action,
                personSummary(person),
                sessionSummary(timesheet.getClassSession()),
                timesheet.getCoachTimesheetId(),
                timesheet.getCheckInTime() == null
                        ? null
                        : LocalDateTime.of(timesheet.getClassSession().getSessionDate(), timesheet.getCheckInTime()),
                timesheet.getCheckOutTime(),
                null,
                confidence,
                null,
                error == null ? "Coach timesheet updated" : error.detail(),
                error
        );
    }

    private FaceCheckInResponse rejectedResponse(Person person, float confidence, ApiException exception) {
        FaceCheckInResponse.ErrorSummary error = new FaceCheckInResponse.ErrorSummary(
                exception.getErrorCode().code(),
                exception.getErrorCode().title(),
                exception.responseDetail()
        );
        return new FaceCheckInResponse(
                FaceCheckInResponse.Status.REJECTED,
                null,
                personSummary(person),
                null,
                null,
                null,
                null,
                null,
                confidence,
                null,
                exception.responseDetail(),
                error
        );
    }

    private FaceCheckInResponse openResponse(AttendanceCommand command, FaceCheckInResponse.Status status) {
        FaceCheckInCommandMessage payload = attendanceCommandStore.readPayload(command, FaceCheckInCommandMessage.class);
        Person person = personRepository.findById(payload.personId())
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND));
        return new FaceCheckInResponse(
                status,
                null,
                personSummary(person),
                null,
                null,
                null,
                null,
                command.getRequestId(),
                payload.confidence(),
                null,
                status == FaceCheckInResponse.Status.PENDING
                        ? "Face recognized. Attendance command queued"
                        : "Attendance command is processing",
                null
        );
    }

    private FaceCheckInResponse expiredResponse(AttendanceCommand command) {
        FaceCheckInCommandMessage payload = attendanceCommandStore.readPayload(command, FaceCheckInCommandMessage.class);
        Person person = personRepository.findById(payload.personId())
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND));
        FaceCheckInResponse.ErrorSummary error = error(TrainingErrorCode.FACE_CHECK_IN_EXPIRED);
        return new FaceCheckInResponse(
                FaceCheckInResponse.Status.EXPIRED,
                null,
                personSummary(person),
                null,
                null,
                null,
                null,
                command.getRequestId(),
                payload.confidence(),
                null,
                error.detail(),
                error
        );
    }

    private FaceCheckInResponse failedResponse(AttendanceCommand command) {
        FaceCheckInCommandMessage payload = attendanceCommandStore.readPayload(command, FaceCheckInCommandMessage.class);
        Person person = personRepository.findById(payload.personId())
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND));
        FaceCheckInResponse.ErrorSummary error = new FaceCheckInResponse.ErrorSummary(
                command.getErrorCode(),
                command.getErrorTitle(),
                command.getErrorDetail()
        );
        return new FaceCheckInResponse(
                FaceCheckInResponse.Status.FAILED,
                null,
                personSummary(person),
                null,
                null,
                null,
                null,
                command.getRequestId(),
                payload.confidence(),
                null,
                error.detail(),
                error
        );
    }

    private void expireIfTimedOut(AttendanceCommand command) {
        if ((command.getStatus() != AttendanceCommandStatus.QUEUED
                && command.getStatus() != AttendanceCommandStatus.PROCESSING)
                || command.getCreatedAt().plus(expireAfter).isAfter(LocalDateTime.now())) {
            return;
        }
        attendanceCommandStore.markExpired(command.getRequestId(), error(TrainingErrorCode.FACE_CHECK_IN_EXPIRED));
        command.setStatus(AttendanceCommandStatus.EXPIRED);
    }

    private FaceCheckInResponse.ErrorSummary coachError(
            FaceCheckInResponse.Status status,
            boolean existingHadCheckOut
    ) {
        if (status != FaceCheckInResponse.Status.REJECTED) {
            return null;
        }
        return existingHadCheckOut
                ? error(TrainingErrorCode.FACE_CHECK_IN_ALREADY_CHECKED_OUT)
                : error(TrainingErrorCode.FACE_CHECK_IN_ALREADY_CHECKED_IN);
    }

    private FaceCheckInResponse.ErrorSummary error(TrainingErrorCode errorCode) {
        return new FaceCheckInResponse.ErrorSummary(
                errorCode.code(),
                errorCode.title(),
                errorCode.defaultDetail()
        );
    }

    private FaceCheckInResponse pendingResponse(Person person, float confidence, UUID requestId) {
        return new FaceCheckInResponse(
                FaceCheckInResponse.Status.PENDING,
                null,
                personSummary(person),
                null,
                null,
                null,
                null,
                requestId,
                confidence,
                null,
                "Face recognized. Attendance command queued",
                null
        );
    }

    private FaceCheckInResponse.PersonSummary personSummary(Person person) {
        return new FaceCheckInResponse.PersonSummary(
                person.getPersonId(),
                person.getFullName(),
                person.getPersonCode(),
                faceImageUrlResolver.resolve(person.getPersonId(), person.getFaceImagePath())
        );
    }

    private FaceCheckInResponse.SessionSummary sessionSummary(ClassSession session) {
        return new FaceCheckInResponse.SessionSummary(
                session.getClassSessionId(),
                session.getCourse().getName(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime()
        );
    }
}

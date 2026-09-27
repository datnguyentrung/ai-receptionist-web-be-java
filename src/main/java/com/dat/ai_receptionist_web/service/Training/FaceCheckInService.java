package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.CoachTimesheet;
import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.dto.Training.CheckInCandidate;
import com.dat.ai_receptionist_web.dto.Training.FaceCheckInResponse;
import com.dat.ai_receptionist_web.dto.Training.command.FaceCheckInCommandMessage;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Training.CoachTimesheetRepository;
import com.dat.ai_receptionist_web.repository.Training.SessionAttendanceRepository;
import com.dat.ai_receptionist_web.service.Core.PersonFaceImageUrlResolver;
import com.dat.ai_receptionist_web.service.Training.command.AttendanceCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    public FaceCheckInResponse processQueuedCheckIn(UUID personId, float confidence, LocalDateTime requestedAt) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND));
        try {
            CheckInCandidate candidate = attendanceContextResolver.resolve(personId, requestedAt);
            return dispatch(person, confidence, candidate, requestedAt);
        } catch (ApiException exception) {
            return failedResponse(person, confidence, exception);
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
            return attendanceResponse(
                    person,
                    attendance,
                    confidence,
                    existed ? FaceCheckInResponse.Status.ALREADY_CHECKED_IN : FaceCheckInResponse.Status.SUCCESS,
                    FaceCheckInResponse.Action.STUDENT_CHECK_IN
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
        return coachResponse(person, timesheet, confidence, status, action);
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
            return FaceCheckInResponse.Status.ALREADY_CHECKED_OUT;
        }
        return current.getCheckOutTime() == null
                ? FaceCheckInResponse.Status.ALREADY_CHECKED_IN
                : FaceCheckInResponse.Status.SUCCESS;
    }

    private FaceCheckInResponse attendanceResponse(
            Person person,
            SessionAttendance attendance,
            float confidence,
            FaceCheckInResponse.Status status,
            FaceCheckInResponse.Action action
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
                status == FaceCheckInResponse.Status.SUCCESS ? "Face check-in completed" : "Already checked in",
                null
        );
    }

    private FaceCheckInResponse coachResponse(
            Person person,
            CoachTimesheet timesheet,
            float confidence,
            FaceCheckInResponse.Status status,
            FaceCheckInResponse.Action action
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
                status == FaceCheckInResponse.Status.SUCCESS ? "Coach timesheet updated" : status.name(),
                null
        );
    }

    private FaceCheckInResponse failedResponse(Person person, float confidence, ApiException exception) {
        FaceCheckInResponse.ErrorSummary error = new FaceCheckInResponse.ErrorSummary(
                exception.getErrorCode().code(),
                exception.getErrorCode().title(),
                exception.responseDetail()
        );
        return new FaceCheckInResponse(
                FaceCheckInResponse.Status.FAILED,
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

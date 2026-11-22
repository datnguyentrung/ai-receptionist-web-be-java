package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.dto.Notification.NotificationDTO;
import com.dat.ai_receptionist_web.enums.Training.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Training.NotificationType;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.service.Notification.NotificationService;
import com.dat.ai_receptionist_web.service.Notification.TransactionAfterCommitExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Creates account-level notifications for people affected by attendance actions.
 * Recipients are the active accounts linked to that person (for example, the
 * student's parent account and the student's own account when either exists).
 */
@Service
@RequiredArgsConstructor
public class AttendanceNotificationService {
    private final UserPersonRepository userPersonRepository;
    private final NotificationService notificationService;
    private final TransactionAfterCommitExecutor afterCommitExecutor;
    private final ObjectMapper objectMapper;

    public void notifyAttendanceRecorded(SessionAttendance attendance) {
        Person person = participant(attendance);
        notifyPerson(
                person,
                "Điểm danh đã được cập nhật",
                person.getFullName() + " đã được ghi nhận: " + attendanceLabel(attendance.getAttendanceStatus()) + ".",
                "ATTENDANCE_RECORDED",
                "SESSION_ATTENDANCE",
                attendance.getSessionAttendanceId(),
                attendance.getClassSession().getClassSessionId(),
                attendance.getAttendanceStatus()
        );
    }

    public void notifyEvaluationUpdated(SessionAttendance attendance) {
        Person person = participant(attendance);
        notifyPerson(
                person,
                "Đánh giá buổi học mới",
                "Giáo viên đã cập nhật đánh giá buổi học cho " + person.getFullName() + ".",
                "ATTENDANCE_EVALUATION_UPDATED",
                "SESSION_ATTENDANCE",
                attendance.getSessionAttendanceId(),
                attendance.getClassSession().getClassSessionId(),
                attendance.getAttendanceStatus()
        );
    }

    public void notifyFaceCheckIn(SessionAttendance attendance) {
        Person person = participant(attendance);
        notifyPerson(
                person,
                "Điểm danh khuôn mặt thành công",
                person.getFullName() + " đã điểm danh bằng khuôn mặt.",
                "FACE_CHECK_IN_SUCCEEDED",
                "SESSION_ATTENDANCE",
                attendance.getSessionAttendanceId(),
                attendance.getClassSession().getClassSessionId(),
                attendance.getAttendanceStatus()
        );
    }

    public void notifyFaceCheckIn(Person person, UUID classSessionId, UUID coachTimesheetId) {
        notifyPerson(
                person,
                "Điểm danh khuôn mặt thành công",
                person.getFullName() + " đã điểm danh bằng khuôn mặt.",
                "FACE_CHECK_IN_SUCCEEDED",
                "COACH_TIMESHEET",
                coachTimesheetId,
                classSessionId,
                null
        );
    }

    private void notifyPerson(
            Person person,
            String title,
            String body,
            String event,
            String referenceType,
            UUID referenceId,
            UUID classSessionId,
            AttendanceStatus attendanceStatus
    ) {
        List<UUID> recipientIds = userPersonRepository.findActiveUserIdsByPersonId(person.getPersonId());
        if (recipientIds.isEmpty()) {
            return;
        }

        String payload = payload(event, person.getPersonId(), classSessionId, attendanceStatus);
        Set<UUID> recipients = Set.copyOf(recipientIds);
        afterCommitExecutor.afterCommit(() -> notificationService.create(new NotificationDTO.CreateRequest(
                title,
                body,
                NotificationType.ATTENDANCE,
                referenceType,
                referenceId.toString(),
                payload,
                recipients,
                null,
                null
        )));
    }

    private Person participant(SessionAttendance attendance) {
        return attendance.getPerson();
    }

    private String payload(
            String event,
            UUID personId,
            UUID classSessionId,
            AttendanceStatus attendanceStatus
    ) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("event", event);
        value.put("personId", personId);
        value.put("classSessionId", classSessionId);
        if (attendanceStatus != null) {
            value.put("attendanceStatus", attendanceStatus.name());
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize attendance notification payload", exception);
        }
    }

    private String attendanceLabel(AttendanceStatus status) {
        return switch (status) {
            case PRESENT -> "Có mặt";
            case ABSENT -> "Vắng mặt";
            case EXCUSED -> "Vắng có phép";
            case MAKEUP -> "Học bù";
            case LATE -> "Đi muộn";
        };
    }
}

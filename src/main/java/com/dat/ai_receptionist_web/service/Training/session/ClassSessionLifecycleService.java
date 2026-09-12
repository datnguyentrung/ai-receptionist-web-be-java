package com.dat.ai_receptionist_web.service.Training.session;

import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.enums.Training.AttendanceStatus;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
import com.dat.ai_receptionist_web.repository.Training.SessionAttendanceRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Vòng đời session: SCHEDULED -> ACTIVE -> (đóng điểm danh) -> COMPLETED.
 * Chỉ sở hữu lifecycle orchestration của ClassSession; không hấp thụ nghiệp vụ
 * SessionAttendance độc lập (check-in, CRUD attendance, evaluation).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClassSessionLifecycleService {
    private final ClassSessionRepository classSessionRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final SessionAttendanceRepository attendanceRepository;
    private final CourseStaffAssignmentRepository assignmentRepository;
    private final TransactionTemplate transactionTemplate;

    @Value("${ATTENDANCE_GRACE_PERIOD_MINUTES:30}")
    private int attendanceGracePeriodMinutes;

    @Value("${CLASS_SESSION_COMPLETE_AFTER_END_MINUTES:15}")
    private int classSessionCompleteAfterEndMinutes;

    @Transactional
    public int activateSessions() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime thresholdTime = now
                .plusMinutes(attendanceGracePeriodMinutes)
                .toLocalTime();
        int updated = classSessionRepository.activateScheduledSessions(
                now.toLocalDate(), thresholdTime);
        if (updated > 0) {
            log.info("Activated {} class sessions at {} with prep threshold {} minutes",
                    updated, now, attendanceGracePeriodMinutes);
        }
        return updated;
    }

    public int completeSessions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thresholdDateTime = now.minusMinutes(classSessionCompleteAfterEndMinutes);
        LocalDate thresholdDate = thresholdDateTime.toLocalDate();
        LocalTime thresholdTime = thresholdDateTime.toLocalTime();

        List<ClassSession> candidates = classSessionRepository.findSessionsToComplete(
                thresholdDate, thresholdTime);
        if (candidates.isEmpty()) {
            return 0;
        }

        int completed = 0;
        int skipped = 0;
        int failed = 0;
        for (ClassSession candidate : candidates) {
            try {
                Boolean ok = transactionTemplate.execute(status -> {
                    int updated = classSessionRepository.markSessionCompleted(
                            candidate.getClassSessionId());
                    return updated > 0;
                });
                if (Boolean.TRUE.equals(ok)) {
                    completed++;
                    log.info("Completed class session {} on date {}",
                            candidate.getClassSessionId(), candidate.getSessionDate());
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                failed++;
                log.error("Failed to complete class session {}",
                        candidate.getClassSessionId(), e);
            }
        }

        log.info("Auto complete class sessions finished at {}. Completed: {}, Skipped: {}, Failed: {}, "
                        + "Delay after end: {} minutes",
                now, completed, skipped, failed, classSessionCompleteAfterEndMinutes);
        return completed;
    }

    public int closeDueSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<ClassSession> due = classSessionRepository.findSessionsToClose(now.toLocalDate());
        int closed = 0;
        int failed = 0;

        for (ClassSession session : due) {
            LocalDateTime closeTime = ClassSessionTimingPolicy.attendanceCloseTime(
                    session.getSessionDate(), session.getStartTime(), session.getEndTime());
            if (closeTime == null || now.isBefore(closeTime)) {
                continue;
            }
            try {
                Boolean ok = transactionTemplate.execute(status ->
                        closeOne(session.getClassSessionId()));
                if (Boolean.TRUE.equals(ok)) {
                    closed++;
                }
            } catch (Exception e) {
                failed++;
                log.error("Failed to close attendance for class session {}",
                        session.getClassSessionId(), e);
            }
        }

        log.info("Auto close attendance finished at {}. Success: {}, Failed: {}",
                now, closed, failed);
        return closed;
    }

    private boolean closeOne(UUID sessionId) {
        ClassSession session = classSessionRepository.findById(sessionId).orElse(null);
        if (session == null || session.isAttendanceClosed()) {
            return false;
        }
        List<StudentEnrollment> enrollments = enrollmentRepository
                .findActiveEnrollmentsForCourseOnDate(
                        session.getCourse().getCourseId(), session.getSessionDate());
        List<CourseStaffAssignment> assistantAssignments = assignmentRepository
                .findEffectiveAssistantAssignmentsForCourseOnDate(
                        session.getCourse().getCourseId(), session.getSessionDate());
        List<SessionAttendance> existingAttendances = attendanceRepository
                .findByClassSession_ClassSessionId(sessionId);
        Set<UUID> presentEnrollmentIds = existingAttendances
                .stream()
                .filter(attendance -> attendance.getStudentEnrollment() != null)
                .map(attendance -> attendance.getStudentEnrollment().getStudentEnrollmentId())
                .collect(Collectors.toSet());
        Set<UUID> presentAssignmentIds = existingAttendances
                .stream()
                .filter(attendance -> attendance.getCourseStaffAssignment() != null)
                .map(attendance -> attendance.getCourseStaffAssignment().getCourseStaffAssignmentId())
                .collect(Collectors.toSet());

        List<SessionAttendance> missingEnrollmentAttendances = enrollments.stream()
                .filter(enrollment -> !presentEnrollmentIds.contains(
                        enrollment.getStudentEnrollmentId()))
                .map(enrollment -> SessionAttendance.builder()
                        .classSession(session)
                        .studentEnrollment(enrollment)
                        .attendanceStatus(AttendanceStatus.ABSENT)
                        .build())
                .toList();
        List<SessionAttendance> missingAssistantAttendances = assistantAssignments.stream()
                .filter(assignment -> !presentAssignmentIds.contains(
                        assignment.getCourseStaffAssignmentId()))
                .map(assignment -> SessionAttendance.builder()
                        .classSession(session)
                        .courseStaffAssignment(assignment)
                        .attendanceStatus(AttendanceStatus.ABSENT)
                        .build())
                .toList();
        List<SessionAttendance> missing = new ArrayList<>(
                missingEnrollmentAttendances.size() + missingAssistantAttendances.size());
        missing.addAll(missingEnrollmentAttendances);
        missing.addAll(missingAssistantAttendances);
        attendanceRepository.saveAll(missing);
        session.setAttendanceClosed(true);
        classSessionRepository.save(session);
        return true;
    }
}

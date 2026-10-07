package com.dat.ai_receptionist_web.service.Training.scheduling;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.ClassSessionScheduleSnapshot;
import com.dat.ai_receptionist_web.domain.Training.LeaveRequest;
import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import com.dat.ai_receptionist_web.enums.Core.Weekday;
import com.dat.ai_receptionist_web.enums.Training.ScheduleImpactType;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseScheduleRepository;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Module sâu: lập kế hoạch lịch theo Course, sinh session theo weekday và
 * watermark, xử lý đổi lịch ngay/lịch chờ trong cùng transaction với khóa Course.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseSessionPlanningService {
    public static final int CLASS_SESSION_GENERATION_THRESHOLD_DAYS = 45;
    public static final int CLASS_SESSION_GENERATION_HORIZON_DAYS = 90;

    private final CourseRepository courseRepository;
    private final CourseScheduleRepository courseScheduleRepository;
    private final ClassSessionRepository classSessionRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    @Transactional
    public void maintainGenerationHorizon() {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(CLASS_SESSION_GENERATION_THRESHOLD_DAYS);
        LocalDate horizon = today.plusDays(CLASS_SESSION_GENERATION_HORIZON_DAYS);

        List<Course> courses = courseRepository.findCoursesNeedClassSessionGeneration(
                CourseStatus.ACTIVE, threshold);
        for (Course course : courses) {
            Course locked = lock(course.getCourseId());
            if (locked.getStatus() != CourseStatus.ACTIVE) {
                continue;
            }
            LocalDate from = locked.getClassSessionGeneratedUntil() == null
                    ? today : locked.getClassSessionGeneratedUntil().plusDays(1);
            if (!from.isAfter(horizon)) {
                for (CourseSchedule schedule : courseScheduleRepository.findDetailedByCourseId(locked.getCourseId())) {
                    if (schedule.getStatus() != com.dat.ai_receptionist_web.enums.Core.ScheduleStatus.ACTIVE) {
                        continue;
                    }
                    LocalDate scheduleFrom = from.isAfter(schedule.getStartDate()) ? from : schedule.getStartDate();
                    LocalDate scheduleUntil = schedule.getEndDate() != null && schedule.getEndDate().isBefore(horizon)
                            ? schedule.getEndDate() : horizon;
                    generateSessions(locked, schedule, scheduleFrom, scheduleUntil);
                }
            }
            locked.setClassSessionGeneratedUntil(horizon);
            courseRepository.save(locked);
        }
    }

    private List<ClassSession> cancelUpcomingSessions(
            UUID courseId, LocalDate fromDate, LocalDate today, LocalTime nowTime) {
        List<ClassSession> candidates = classSessionRepository.findUpcomingSessionsToCancel(
                courseId, fromDate, today, nowTime);
        candidates.forEach(session -> session.setStatus(SessionStatus.CANCELLED));
        return candidates;
    }

    private List<UUID> generateSessions(
            Course course, CourseSchedule courseSchedule, LocalDate from, LocalDate until) {
        if (from == null || until == null || from.isAfter(until)) {
            return List.of();
        }
        Set<LocalDate> existing = new HashSet<>(classSessionRepository
                .findSessionDatesByCourseAndRange(course.getCourseId(), from, until, SessionStatus.CANCELLED));
        List<ClassSession> created = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(until); date = date.plusDays(1)) {
            if (existing.contains(date)) {
                continue;
            }
            if (Weekday.fromJavaDayOfWeek(date.getDayOfWeek()) != courseSchedule.getClassSchedule().getWeekday()) {
                continue;
            }
            created.add(ClassSession.builder()
                    .course(course)
                    .courseSchedule(courseSchedule)
                    .scheduleSnapshot(ClassSessionScheduleSnapshot.from(courseSchedule.getClassSchedule()))
                    .sessionDate(date)
                    .status(SessionStatus.SCHEDULED)
                    .attendanceClosed(false)
                    .startTime(courseSchedule.getClassSchedule().getStartTime())
                    .endTime(courseSchedule.getClassSchedule().getEndTime())
                    .build());
        }
        classSessionRepository.saveAll(created);
        return created.stream().map(ClassSession::getClassSessionId).toList();
    }

    private List<CourseScheduleChangeNotifier.AffectedLeaveRequest> detectAffectedLeaveRequests(
            List<ClassSession> cancelled) {
        if (cancelled.isEmpty()) {
            return List.of();
        }
        List<UUID> sessionIds = cancelled.stream().map(ClassSession::getClassSessionId).toList();
        List<LeaveRequest> requests = leaveRequestRepository.findByReferencedSessionIds(sessionIds);
        List<CourseScheduleChangeNotifier.AffectedLeaveRequest> affected = new ArrayList<>();
        for (LeaveRequest request : requests) {
            for (ClassSession session : cancelled) {
                UUID sessionId = session.getClassSessionId();
                if (request.getLeaveClassSession() != null
                        && sessionId.equals(request.getLeaveClassSession().getClassSessionId())) {
                    affected.add(toAffected(request, session, ScheduleImpactType.LEAVE_SESSION));
                }
                if (request.getMakeupClassSession() != null
                        && sessionId.equals(request.getMakeupClassSession().getClassSessionId())) {
                    affected.add(toAffected(request, session, ScheduleImpactType.MAKEUP_SESSION));
                }
            }
        }
        return affected;
    }

    private CourseScheduleChangeNotifier.AffectedLeaveRequest toAffected(
            LeaveRequest request, ClassSession session,
            ScheduleImpactType impactType) {
        return new CourseScheduleChangeNotifier.AffectedLeaveRequest(
                request.getLeaveRequestId(),
                request.getPerson().getPersonId(),
                session.getCourse().getCourseId(),
                session.getClassSessionId(),
                impactType,
                request.getStatus());
    }

    private Course lock(UUID courseId) {
        return courseRepository.findByIdForUpdate(courseId)
                .orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_NOT_FOUND));
    }

}

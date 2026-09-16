package com.dat.ai_receptionist_web.service.Training.scheduling;

import com.dat.ai_receptionist_web.domain.Catalog.ClassSchedule;
import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.LeaveRequest;
import com.dat.ai_receptionist_web.dto.Catalog.CourseDTO;
import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import com.dat.ai_receptionist_web.enums.Core.Weekday;
import com.dat.ai_receptionist_web.enums.Training.ScheduleImpactType;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.mapper.Catalog.CourseMapper;
import com.dat.ai_receptionist_web.repository.Catalog.ClassScheduleRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
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
    private final ClassScheduleRepository classScheduleRepository;
    private final ClassSessionRepository classSessionRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final CourseScheduleChangeNotifier changeNotifier;
    private final CourseMapper courseMapper;

    @Transactional
    public CourseDTO.CourseScheduleChangeResponse changeSchedule(
            UUID courseId, UUID scheduleId, LocalDate effectiveFrom) {
        Course course = lock(courseId);
        requireActive(course);
        ClassSchedule newSchedule = requireActiveSchedule(scheduleId);
        LocalDate today = LocalDate.now();

        if (newSchedule.getScheduleId().equals(course.getClassSchedule().getScheduleId())) {
            return courseMapper.toScheduleChangeResponse(course, List.of(), List.of());
        }
        if (course.getNextClassSchedule() != null
                && newSchedule.getScheduleId().equals(course.getNextClassSchedule().getScheduleId())
                && effectiveFrom.equals(course.getNextScheduleEffectiveFrom())) {
            return courseMapper.toScheduleChangeResponse(course, List.of(), List.of());
        }

        LocalDate effectiveDate = effectiveFrom.isAfter(today) ? effectiveFrom : today;
        List<ClassSession> cancelled;

        if (effectiveFrom.isAfter(today)) {
            LocalDate oldEffective = course.getNextScheduleEffectiveFrom();
            LocalDate cancelStart = oldEffective != null && oldEffective.isBefore(effectiveDate)
                    ? oldEffective : effectiveDate;
            cancelled = cancelUpcomingSessions(course.getCourseId(), cancelStart, today, LocalTime.now());

            if (oldEffective != null && oldEffective.isBefore(effectiveDate)) {
                generateSessions(course, course.getClassSchedule(), oldEffective, effectiveDate.minusDays(1));
            }
            LocalDate currentFrom = course.getClassSessionGeneratedUntil() == null
                    ? today : course.getClassSessionGeneratedUntil().plusDays(1);
            if (currentFrom.isBefore(effectiveDate)) {
                generateSessions(course, course.getClassSchedule(), currentFrom, effectiveDate.minusDays(1));
            }
            course.setNextClassSchedule(newSchedule);
            course.setNextScheduleEffectiveFrom(effectiveFrom);
        } else {
            cancelled = cancelUpcomingSessions(course.getCourseId(), today, today, LocalTime.now());
            course.setClassSchedule(newSchedule);
            course.setNextClassSchedule(null);
            course.setNextScheduleEffectiveFrom(null);
        }

        List<CourseScheduleChangeNotifier.AffectedLeaveRequest> affected =
                detectAffectedLeaveRequests(cancelled);
        LocalDate horizon = today.plusDays(CLASS_SESSION_GENERATION_HORIZON_DAYS);
        List<UUID> generated = generateSessions(course, newSchedule, effectiveDate, horizon);
        course.setClassSessionGeneratedUntil(horizon);
        courseRepository.save(course);

        changeNotifier.notifyAfterCommit(course.getCourseId(), affected);

        return courseMapper.toScheduleChangeResponse(
                course,
                cancelled.stream().map(ClassSession::getClassSessionId).toList(),
                generated
        );
    }

    @Transactional
    public void cancelPendingScheduleChange(UUID courseId) {
        Course course = lock(courseId);
        if (course.getNextClassSchedule() == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate oldEffective = course.getNextScheduleEffectiveFrom();
        ClassSchedule toSchedule = course.getClassSchedule();

        List<ClassSession> cancelled = cancelUpcomingSessions(
                course.getCourseId(), oldEffective, today, LocalTime.now());
        List<CourseScheduleChangeNotifier.AffectedLeaveRequest> affected =
                detectAffectedLeaveRequests(cancelled);
        LocalDate horizon = today.plusDays(CLASS_SESSION_GENERATION_HORIZON_DAYS);
        List<UUID> generated = generateSessions(course, toSchedule, oldEffective, horizon);
        course.setNextClassSchedule(null);
        course.setNextScheduleEffectiveFrom(null);
        course.setClassSessionGeneratedUntil(horizon);
        courseRepository.save(course);

        changeNotifier.notifyAfterCommit(course.getCourseId(), affected);
        log.info("Cancelled pending schedule change for course {}, cancelled={}, generated={}, affectedLeaves={}",
                courseId, cancelled.size(), generated.size(), affected.size());
    }

    @Transactional
    public void maintainGenerationHorizon() {
        applyDueScheduleChanges();
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
            LocalDate effectiveFrom = locked.getNextScheduleEffectiveFrom();
            if (effectiveFrom != null) {
                LocalDate currentUntil = effectiveFrom.minusDays(1);
                if (!from.isAfter(currentUntil)) {
                    generateSessions(locked, locked.getClassSchedule(), from, currentUntil);
                }
                if (!from.isAfter(horizon)) {
                    LocalDate pendingFrom = effectiveFrom.isAfter(from) ? effectiveFrom : from;
                    generateSessions(locked, locked.getNextClassSchedule(), pendingFrom, horizon);
                }
            } else if (!from.isAfter(horizon)) {
                generateSessions(locked, locked.getClassSchedule(), from, horizon);
            }
            locked.setClassSessionGeneratedUntil(horizon);
            courseRepository.save(locked);
        }
    }

    private void applyDueScheduleChanges() {
        List<Course> due = courseRepository.findCoursesWithPendingScheduleDue(LocalDate.now());
        for (Course course : due) {
            Course locked = lock(course.getCourseId());
            if (locked.getNextClassSchedule() == null) {
                continue;
            }
            locked.setClassSchedule(locked.getNextClassSchedule());
            locked.setNextClassSchedule(null);
            locked.setNextScheduleEffectiveFrom(null);
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
            Course course, ClassSchedule schedule, LocalDate from, LocalDate until) {
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
            if (Weekday.fromJavaDayOfWeek(date.getDayOfWeek()) != schedule.getWeekday()) {
                continue;
            }
            created.add(ClassSession.builder()
                    .course(course)
                    .sessionDate(date)
                    .status(SessionStatus.SCHEDULED)
                    .attendanceClosed(false)
                    .startTime(schedule.getStartTime())
                    .endTime(schedule.getEndTime())
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

    private void requireActive(Course course) {
        if (course.getStatus() != CourseStatus.ACTIVE) {
            throw new ApiException(TrainingErrorCode.COURSE_NOT_ACTIVE);
        }
    }

    private ClassSchedule requireActiveSchedule(UUID scheduleId) {
        ClassSchedule schedule = classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ApiException(CatalogErrorCode.CLASS_SCHEDULE_NOT_FOUND));
        if (schedule.getStatus() != ScheduleStatus.ACTIVE) {
            throw new ApiException(CatalogErrorCode.COURSE_SCHEDULE_CHANGE_CONFLICT,
                    "Class schedule must be ACTIVE");
        }
        return schedule;
    }

}

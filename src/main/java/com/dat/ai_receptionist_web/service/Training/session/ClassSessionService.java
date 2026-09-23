package com.dat.ai_receptionist_web.service.Training.session;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Training.ClassSessionDTO;
import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.error.code.GeneralErrorCode;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.mapper.Training.ClassSessionMapper;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import com.dat.ai_receptionist_web.service.Training.access.ClassSessionAccessPolicy;
import com.dat.ai_receptionist_web.service.Training.access.TrainingAccessScope;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassSessionService {
    private static final long MAX_CALENDAR_RANGE_DAYS = 42;

    private final ClassSessionRepository repository;
    private final ClassSessionMapper mapper;
    private final CourseRepository courseRepository;
    private final CurrentAccessContextResolver currentAccessContextResolver;
    private final ClassSessionAccessPolicy accessPolicy;
    private final CourseStaffAssignmentRepository courseStaffAssignmentRepository;

    @Transactional(readOnly = true)
    public PageResponse<ClassSessionDTO.SimpleResponse> list(Pageable pageable) {
        AccessContext context = currentAccessContextResolver.current();
        TrainingAccessScope scope = accessPolicy.resolveReadScope(context);
        Page<ClassSession> sessions = repository.findAccessible(context.activePersonId(), scope.unrestricted(), pageable);
        Map<SessionPrimaryCoachKey, Person> primaryCoachBySessionKey = getPrimaryCoaches(sessions.getContent());
        return PageResponse.of(
                sessions,
                session -> mapper.toSimpleResponse(
                        session,
                        primaryCoachBySessionKey.get(SessionPrimaryCoachKey.of(session)))
        );
    }

    @Transactional(readOnly = true)
    public ClassSessionDTO.Response get(UUID id) {
        AccessContext context = currentAccessContextResolver.current();
        return mapper.toResponse(findAccessible(id, context, accessPolicy.resolveReadScope(context)));
    }

    @Transactional
    public ClassSessionDTO.Response create(ClassSessionDTO.CreateRequest request) {
        AccessContext context = currentAccessContextResolver.current();
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_NOT_FOUND));
        if (course.getStatus() != CourseStatus.ACTIVE) {
            throw new ApiException(TrainingErrorCode.COURSE_NOT_ACTIVE);
        }
        accessPolicy.requireCanManage(context, request.courseId(), request.sessionDate());
        validateTime(request.startTime(), request.endTime());
        if (request.sessionDate().isBefore(LocalDate.now())) {
            throw new ApiException(TrainingErrorCode.CLASS_SESSION_IMMUTABLE,
                    "Cannot create a class session in the past");
        }
        if (repository.existsByCourse_CourseIdAndSessionDateAndStatusNot(
                request.courseId(), request.sessionDate(), SessionStatus.CANCELLED)) {
            throw new ApiException(TrainingErrorCode.CLASS_SESSION_ALREADY_EXISTS);
        }
        ClassSession entity = ClassSession.builder()
                .course(course)
                .sessionDate(request.sessionDate())
                .status(request.status())
                .attendanceClosed(false)
                .attendanceReopenedUntil(null)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .note(request.note())
                .build();
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public ClassSessionDTO.Response update(UUID id,
                                           ClassSessionDTO.UpdateRequest request) {
        AccessContext context = currentAccessContextResolver.current();
        ClassSession entity = findAccessible(id, context, accessPolicy.resolveWriteScope(context));
        requireMutable(entity);
        if (!entity.getCourse().getCourseId().equals(request.courseId())) {
            throw new ApiException(TrainingErrorCode.CLASS_SESSION_IMMUTABLE,
                    "Course of a class session cannot be changed");
        }
        accessPolicy.requireCanManage(context, request.courseId(), request.sessionDate());
        validateTime(request.startTime(), request.endTime());
        if (!request.sessionDate().equals(entity.getSessionDate())
                && repository.existsByCourse_CourseIdAndSessionDateAndStatusNot(
                request.courseId(), request.sessionDate(), SessionStatus.CANCELLED)) {
            throw new ApiException(TrainingErrorCode.CLASS_SESSION_ALREADY_EXISTS);
        }
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        AccessContext context = currentAccessContextResolver.current();
        ClassSession entity = findAccessible(id, context, accessPolicy.resolveWriteScope(context));
        accessPolicy.requireCanManage(context, entity.getCourse().getCourseId(), entity.getSessionDate());
        requireMutable(entity);
        entity.setStatus(SessionStatus.CANCELLED);
    }

    @Transactional
    public ClassSessionDTO.Response reopenAttendance(UUID id,
                                                     ClassSessionDTO.ReopenAttendanceRequest request) {
        if (request.attendanceReopenedUntil() == null
                || !request.attendanceReopenedUntil().isAfter(LocalDateTime.now())) {
            throw new ApiException(GeneralErrorCode.INVALID_REQUEST_BODY,
                    "attendanceReopenedUntil must be in the future");
        }
        AccessContext context = currentAccessContextResolver.current();
        ClassSession entity = findAccessible(id, context, accessPolicy.resolveWriteScope(context));
        accessPolicy.requireCanManage(context, entity.getCourse().getCourseId(), entity.getSessionDate());
        entity.setAttendanceReopenedUntil(request.attendanceReopenedUntil());
        return mapper.toResponse(repository.save(entity));
    }

    private ClassSession findAccessible(
            UUID id,
            AccessContext context,
            TrainingAccessScope scope
    ) {
        return repository.findAccessibleById(
                id,
                context.activePersonId(),
                scope.unrestricted()
        ).orElseThrow(() -> new ApiException(TrainingErrorCode.CLASS_SESSION_NOT_FOUND));
    }

    private void requireMutable(ClassSession entity) {
        if (entity.getStatus() != SessionStatus.SCHEDULED
                && entity.getStatus() != SessionStatus.POSTPONED) {
            throw new ApiException(TrainingErrorCode.CLASS_SESSION_IMMUTABLE);
        }
        LocalDate today = LocalDate.now();
        if (entity.getSessionDate().isBefore(today)) {
            throw new ApiException(TrainingErrorCode.CLASS_SESSION_IMMUTABLE);
        }
        if (entity.getSessionDate().equals(today)
                && (entity.getEndTime() == null || !entity.getEndTime().isAfter(LocalTime.now()))) {
            throw new ApiException(TrainingErrorCode.CLASS_SESSION_IMMUTABLE);
        }
    }

    private void validateTime(LocalTime startTime,
                              LocalTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new ApiException(TrainingErrorCode.CLASS_SESSION_TIME_INVALID);
        }
    }

    @Transactional(readOnly = true)
    public List<ClassSessionDTO.CalendarResponse> getCalendar(LocalDate fromDate,
                                                              LocalDate toDate) {
        validateCalendarRange(fromDate, toDate);
        AccessContext context = currentAccessContextResolver.current();
        TrainingAccessScope scope = accessPolicy.resolveReadScope(context);
        List<ClassSession> sessions = repository.findCalendarSessions(
                        fromDate,
                        toDate,
                        context.activePersonId(),
                        scope.unrestricted());
        Map<SessionPrimaryCoachKey, Person> primaryCoachBySessionKey =
                getPrimaryCoachesForRange(sessions, fromDate, toDate);
        return sessions.stream()
                .sorted(Comparator.comparing(ClassSession::getSessionDate)
                        .thenComparing(ClassSession::getStartTime))
                .map(session -> mapper.toCalendarResponse(
                        session,
                        primaryCoachBySessionKey.get(SessionPrimaryCoachKey.of(session))))
                .toList();
    }

    private void validateCalendarRange(LocalDate fromDate,
                                       LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new ApiException(GeneralErrorCode.INVALID_REQUEST_PARAMETER,
                    "fromDate and toDate are required");
        }
        if (fromDate.isAfter(toDate)) {
            throw new ApiException(GeneralErrorCode.INVALID_REQUEST_PARAMETER,
                    "fromDate must be before or equal to toDate");
        }
        long rangeDays = ChronoUnit.DAYS.between(fromDate, toDate);
        if (rangeDays > MAX_CALENDAR_RANGE_DAYS) {
            throw new ApiException(GeneralErrorCode.INVALID_REQUEST_PARAMETER,
                    "Calendar range must not exceed " + MAX_CALENDAR_RANGE_DAYS + " days");
        }
    }

    private Map<SessionPrimaryCoachKey, Person> getPrimaryCoaches(List<ClassSession> sessions) {
        if (sessions.isEmpty()) {
            return Map.of();
        }
        LocalDate fromDate = sessions.stream()
                .map(ClassSession::getSessionDate)
                .min(LocalDate::compareTo)
                .orElseThrow();
        LocalDate toDate = sessions.stream()
                .map(ClassSession::getSessionDate)
                .max(LocalDate::compareTo)
                .orElseThrow();
        return getPrimaryCoachesForRange(sessions, fromDate, toDate);
    }

    private Map<SessionPrimaryCoachKey, Person> getPrimaryCoachesForRange(Collection<ClassSession> sessions,
                                                                          LocalDate fromDate,
                                                                          LocalDate toDate) {
        if (sessions.isEmpty()) {
            return Map.of();
        }
        Set<UUID> courseIds = sessions.stream()
                .map(session -> session.getCourse().getCourseId())
                .collect(Collectors.toSet());
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        List<CourseStaffAssignment> assignments =
                courseStaffAssignmentRepository.findEffectivePrimaryCoachAssignmentsForCourseIdsBetween(
                        courseIds, fromDate, toDate);
        Map<UUID, List<CourseStaffAssignment>> assignmentsByCourseId = assignments.stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getCourse().getCourseId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<SessionPrimaryCoachKey, Person> primaryCoachBySessionKey = new LinkedHashMap<>();
        for (ClassSession session : sessions) {
            Person primaryCoach = assignmentsByCourseId
                    .getOrDefault(session.getCourse().getCourseId(), List.of()).stream()
                    .filter(assignment -> isEffectiveOn(assignment, session.getSessionDate()))
                    .map(CourseStaffAssignment::getStaffPerson)
                    .findFirst()
                    .orElse(null);
            primaryCoachBySessionKey.put(SessionPrimaryCoachKey.of(session), primaryCoach);
        }
        return primaryCoachBySessionKey;
    }

    private boolean isEffectiveOn(CourseStaffAssignment assignment,
                                  LocalDate sessionDate) {
        return !assignment.getStartDate().isAfter(sessionDate)
                && (assignment.getEndDate() == null || !assignment.getEndDate().isBefore(sessionDate));
    }

    private record SessionPrimaryCoachKey(UUID courseId, LocalDate sessionDate, UUID classSessionId) {
        private static SessionPrimaryCoachKey of(ClassSession session) {
            return new SessionPrimaryCoachKey(
                    session.getCourse().getCourseId(),
                    session.getSessionDate(),
                    session.getClassSessionId());
        }
    }
}

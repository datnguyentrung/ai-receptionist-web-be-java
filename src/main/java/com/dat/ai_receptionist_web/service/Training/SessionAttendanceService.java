package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.domain.Training.command.AttendanceCommandType;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Training.SessionAttendanceDTO;
import com.dat.ai_receptionist_web.dto.Training.command.AttendanceCommandDTO;
import com.dat.ai_receptionist_web.dto.Training.command.SessionAttendanceCommandMessage;
import com.dat.ai_receptionist_web.enums.Training.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Training.EvaluationStatus;
import com.dat.ai_receptionist_web.enums.Security.PermissionDefinition;
import com.dat.ai_receptionist_web.enums.Training.AssignmentType;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.mapper.Training.SessionAttendanceMapper;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
import com.dat.ai_receptionist_web.repository.Training.SessionAttendanceRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.service.Core.PersonCodePolicy;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import com.dat.ai_receptionist_web.service.Training.command.AttendanceCommandService;
import com.dat.ai_receptionist_web.service.Training.access.SessionAttendanceAccessPolicy;
import com.dat.ai_receptionist_web.service.Training.access.TrainingAccessScope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionAttendanceService {
    private static final Sort DEFAULT_LIST_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("sessionAttendanceId")
    );

    private final SessionAttendanceRepository repository;
    private final SessionAttendanceMapper mapper;
    private final ClassSessionRepository classSessionRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final CourseStaffAssignmentRepository courseStaffAssignmentRepository;
    private final PersonCodePolicy personCodePolicy;
    private final CurrentAccessContextResolver currentAccessContextResolver;
    private final SessionAttendanceAccessPolicy accessPolicy;
    private final AttendanceCommandService attendanceCommandService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PageResponse<SessionAttendanceDTO.SimpleResponse> list(
            LocalDate fromDate,
            LocalDate toDate,
            UUID courseId,
            UUID studentPersonId,
            UUID staffPersonId,
            Pageable pageable
    ) {
        AccessContext context = currentAccessContextResolver.current();
        TrainingAccessScope scope = accessPolicy.resolveReadScope(context);
        Pageable effectivePageable = withDefaultSort(pageable);
        var page = repository.findAccessible(
                context.userId(),
                context.activePersonId(),
                scope.unrestricted(),
                scope.self(),
                scope.dependents(),
                scope.assignedCourses(),
                fromDate,
                toDate,
                courseId,
                studentPersonId,
                staffPersonId,
                effectivePageable
        );
        return PageResponse.of(page, mapper::toSimpleResponse);
    }

    @Transactional(readOnly = true)
    public SessionAttendanceDTO.AttendanceListResponse listWithStats(
            LocalDate fromDate,
            LocalDate toDate,
            UUID courseId,
            UUID studentPersonId,
            UUID staffPersonId,
            Pageable pageable
    ) {
        PageResponse<SessionAttendanceDTO.SimpleResponse> page =
                list(fromDate, toDate, courseId, studentPersonId, staffPersonId, pageable);
        return new SessionAttendanceDTO.AttendanceListResponse(stats(page.getContent()), page);
    }

    @Transactional(readOnly = true)
    public SessionAttendanceDTO.Response get(UUID id) {
        AccessContext context = currentAccessContextResolver.current();
        SessionAttendance entity = findReadable(id, context);
        return mapper.toResponse(entity, allowedActions(entity, context));
    }

    @Transactional
    public SessionAttendanceDTO.Response create(SessionAttendanceDTO.CreateRequest request) {
        AccessContext context = currentAccessContextResolver.current();
        var session = classSessionRepository.findById(request.classSessionId())
                .orElseThrow(() -> new ApiException(TrainingErrorCode.CLASS_SESSION_NOT_FOUND));
        StudentEnrollment enrollment = resolveEnrollment(request.studentEnrollmentId());
        CourseStaffAssignment participantAssignment = resolveParticipantAssignment(request.courseStaffAssignmentId());

        if (enrollment != null) {
            personCodePolicy.requireStudent(enrollment.getStudentPerson());
        }
        if (participantAssignment != null) {
            personCodePolicy.requireSystemEmployee(participantAssignment.getStaffPerson());
        }
        accessPolicy.requireCanCreate(session, enrollment, participantAssignment);

        SessionAttendance entity = new SessionAttendance();
        entity.setClassSession(session);
        entity.setStudentEnrollment(enrollment);
        entity.setCourseStaffAssignment(participantAssignment);
        entity.setCheckInTime(request.checkInTime());
        entity.setAttendanceStatus(request.attendanceStatus());
        entity.setEvaluationStatus(request.evaluationStatus());
        entity.setNote(request.note());
        SessionAttendance saved = repository.save(entity);
        return mapper.toResponse(saved, allowedActions(saved, context));
    }

    @Transactional
    public SessionAttendanceDTO.Response update(UUID id, SessionAttendanceDTO.UpdateRequest request) {
        AccessContext context = currentAccessContextResolver.current();
        SessionAttendance entity = findManageable(id);
        accessPolicy.requireCanUpdate(entity);
        mapper.updateEntity(request, entity);
        SessionAttendance saved = repository.save(entity);
        return mapper.toResponse(saved, allowedActions(saved, context));
    }

    @Transactional
    public List<SessionAttendanceDTO.Response> updateBatch(SessionAttendanceDTO.BatchUpdateRequest request) {
        List<UUID> ids = request.records().stream()
                .map(SessionAttendanceDTO.BatchUpdateItem::sessionAttendanceId)
                .toList();
        List<SessionAttendance> entities = repository.findAllById(ids);
        if (entities.size() != ids.size()) {
            throw new ApiException(TrainingErrorCode.SESSION_ATTENDANCE_NOT_FOUND);
        }
        var byId = entities.stream().collect(Collectors.toMap(SessionAttendance::getSessionAttendanceId, item -> item));
        for (SessionAttendanceDTO.BatchUpdateItem item : request.records()) {
            SessionAttendance entity = byId.get(item.sessionAttendanceId());
            accessPolicy.requireCanUpdate(entity);
            if (item.checkInTime() != null) {
                entity.setCheckInTime(item.checkInTime());
            }
            if (item.attendanceStatus() != null) {
                entity.setAttendanceStatus(item.attendanceStatus());
            }
            if (item.evaluationStatus() != null) {
                entity.setEvaluationStatus(item.evaluationStatus());
            }
            if (item.note() != null) {
                entity.setNote(item.note());
            }
        }
        return repository.saveAll(entities).stream()
                .map(this::toResponseForCurrentAccess)
                .toList();
    }

    @Transactional
    public SessionAttendanceDTO.Response updateStatus(UUID id, SessionAttendanceDTO.UpdateStatusRequest request) {
        SessionAttendance entity = findManageable(id);
        accessPolicy.requireCanUpdate(entity);
        entity.setAttendanceStatus(request.attendanceStatus());
        if (request.checkInTime() != null) {
            entity.setCheckInTime(request.checkInTime());
        } else if (entity.getCheckInTime() == null && request.attendanceStatus() != AttendanceStatus.ABSENT) {
            entity.setCheckInTime(LocalDateTime.now());
        }
        SessionAttendance saved = repository.save(entity);
        return toResponseForCurrentAccess(saved);
    }

    @Transactional
    public SessionAttendanceDTO.Response updateEvaluation(UUID id, SessionAttendanceDTO.UpdateEvaluationRequest request) {
        SessionAttendance entity = findManageable(id);
        accessPolicy.requireCanUpdate(entity);
        if (request.evaluationStatus() != null) {
            entity.setEvaluationStatus(request.evaluationStatus());
        }
        if (request.note() != null) {
            entity.setNote(request.note());
        }
        SessionAttendance saved = repository.save(entity);
        return toResponseForCurrentAccess(saved);
    }

    @Transactional
    public void delete(UUID id) {
        SessionAttendance entity = findManageable(id);
        accessPolicy.requireCanDelete(entity);
        repository.delete(entity);
    }

    @Transactional
    public List<SessionAttendanceDTO.Response> batchInit(SessionAttendanceDTO.BatchInitRequest request) {
        ClassSession session = classSessionRepository.findById(request.classSessionId())
                .orElseThrow(() -> new ApiException(TrainingErrorCode.CLASS_SESSION_NOT_FOUND));
        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findActiveEnrollmentsForCourseOnDate(
                session.getCourse().getCourseId(),
                session.getSessionDate()
        );
        Set<UUID> existingEnrollmentIds = repository.findByClassSession_ClassSessionId(session.getClassSessionId())
                .stream()
                .filter(item -> item.getStudentEnrollment() != null)
                .map(item -> item.getStudentEnrollment().getStudentEnrollmentId())
                .collect(Collectors.toSet());
        List<SessionAttendance> newAttendances = enrollments.stream()
                .filter(enrollment -> !existingEnrollmentIds.contains(enrollment.getStudentEnrollmentId()))
                .map(enrollment -> {
                    accessPolicy.requireCanCreate(session, enrollment, null);
                    SessionAttendance entity = new SessionAttendance();
                    entity.setClassSession(session);
                    entity.setStudentEnrollment(enrollment);
                    entity.setAttendanceStatus(AttendanceStatus.ABSENT);
                    entity.setEvaluationStatus(EvaluationStatus.PENDING);
                    entity.setNote("BATCH_INIT");
                    return entity;
                })
                .toList();
        if (!newAttendances.isEmpty()) {
            repository.saveAll(newAttendances);
        }
        return repository.findByClassSession_ClassSessionId(session.getClassSessionId())
                .stream()
                .map(this::toResponseForCurrentAccess)
                .toList();
    }

    @Transactional
    public void bulkDelete(SessionAttendanceDTO.BulkDeleteRequest request) {
        List<SessionAttendance> entities = repository.findAllById(request.sessionAttendanceIds());
        if (entities.size() != request.sessionAttendanceIds().size()) {
            throw new ApiException(TrainingErrorCode.SESSION_ATTENDANCE_NOT_FOUND);
        }
        entities.forEach(accessPolicy::requireCanDelete);
        repository.deleteAll(entities);
    }

    @Transactional
    public SessionAttendanceDTO.Response createManual(SessionAttendanceDTO.ManualLogRequest request) {
        SessionAttendanceDTO.CreateRequest createRequest = new SessionAttendanceDTO.CreateRequest(
                request.classSessionId(),
                request.studentEnrollmentId(),
                request.courseStaffAssignmentId(),
                request.checkInTime() == null ? LocalDateTime.now() : request.checkInTime(),
                request.attendanceStatus(),
                request.evaluationStatus() == null ? EvaluationStatus.PENDING : request.evaluationStatus(),
                request.note()
        );
        return create(createRequest);
    }

    @Transactional
    public SessionAttendanceDTO.Response quickCheckIn(SessionAttendanceDTO.QuickCheckInRequest request) {
        if (request.studentEnrollmentId() != null) {
            SessionAttendance attendance = checkInResolvedStudent(
                    request.classSessionId(),
                    request.studentEnrollmentId(),
                    LocalDateTime.now()
            );
            return toResponseForCurrentAccess(attendance);
        }
        SessionAttendance attendance = checkInResolvedStaff(
                request.classSessionId(),
                request.courseStaffAssignmentId(),
                LocalDateTime.now()
        );
        return toResponseForCurrentAccess(attendance);
    }

    public AttendanceCommandDTO.Receipt enqueueCommand(AttendanceCommandType type, Object payload) {
        UUID requestId = UUID.randomUUID();
        String payloadJson = writeJson(payload);
        return attendanceCommandService.enqueueSessionAttendance(
                new SessionAttendanceCommandMessage(requestId, type, payloadJson)
        );
    }

    @Transactional
    public Object executeQueuedCommand(AttendanceCommandType type, String payloadJson) {
        return switch (type) {
            case SESSION_ATTENDANCE_BATCH_UPDATE ->
                    updateBatch(readJson(payloadJson, SessionAttendanceDTO.BatchUpdateRequest.class));
            case SESSION_ATTENDANCE_STATUS_UPDATE -> {
                IdPayload<SessionAttendanceDTO.UpdateStatusRequest> payload = readJson(
                        payloadJson,
                        new TypeReference<>() {
                        }
                );
                yield updateStatus(payload.id(), payload.request());
            }
            case SESSION_ATTENDANCE_EVALUATION_UPDATE -> {
                IdPayload<SessionAttendanceDTO.UpdateEvaluationRequest> payload = readJson(
                        payloadJson,
                        new TypeReference<>() {
                        }
                );
                yield updateEvaluation(payload.id(), payload.request());
            }
            case SESSION_ATTENDANCE_MANUAL_CREATE ->
                    createManual(readJson(payloadJson, SessionAttendanceDTO.ManualLogRequest.class));
            case SESSION_ATTENDANCE_QUICK_CHECK_IN ->
                    quickCheckIn(readJson(payloadJson, SessionAttendanceDTO.QuickCheckInRequest.class));
            case SESSION_ATTENDANCE_BATCH_INIT ->
                    batchInit(readJson(payloadJson, SessionAttendanceDTO.BatchInitRequest.class));
            case SESSION_ATTENDANCE_BULK_DELETE -> {
                bulkDelete(readJson(payloadJson, SessionAttendanceDTO.BulkDeleteRequest.class));
                yield null;
            }
            default -> throw new ApiException(TrainingErrorCode.FACE_CHECK_IN_UNSUPPORTED_CONTEXT);
        };
    }

    @Transactional
    public SessionAttendance checkInResolvedStudent(UUID classSessionId, UUID studentEnrollmentId,
                                                    LocalDateTime checkInTime) {
        return repository.findByClassSession_ClassSessionIdAndStudentEnrollment_StudentEnrollmentId(
                        classSessionId, studentEnrollmentId)
                .orElseGet(() -> createResolvedStudentAttendance(classSessionId, studentEnrollmentId, checkInTime));
    }

    private SessionAttendance findReadable(UUID id, AccessContext context) {
        TrainingAccessScope scope = accessPolicy.resolveReadScope(context);
        return repository.findAccessibleById(
                id,
                context.userId(),
                context.activePersonId(),
                scope.unrestricted(),
                scope.self(),
                scope.dependents(),
                scope.assignedCourses()
        ).orElseThrow(() -> new ApiException(TrainingErrorCode.SESSION_ATTENDANCE_NOT_FOUND));
    }

    private SessionAttendance findManageable(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.SESSION_ATTENDANCE_NOT_FOUND));
    }

    private StudentEnrollment resolveEnrollment(UUID studentEnrollmentId) {
        if (studentEnrollmentId == null) {
            return null;
        }
        return studentEnrollmentRepository.findById(studentEnrollmentId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_NOT_FOUND));
    }

    private CourseStaffAssignment resolveParticipantAssignment(UUID courseStaffAssignmentId) {
        if (courseStaffAssignmentId == null) {
            return null;
        }
        CourseStaffAssignment assignment = courseStaffAssignmentRepository.findById(courseStaffAssignmentId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_FOUND));
        if (assignment.getAssignmentType() != AssignmentType.ASSISTANT_COACH) {
            throw new ApiException(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE);
        }
        return assignment;
    }

    private SessionAttendance createResolvedStudentAttendance(UUID classSessionId, UUID studentEnrollmentId,
                                                              LocalDateTime checkInTime) {
        ClassSession session = classSessionRepository.findById(classSessionId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.CLASS_SESSION_NOT_FOUND));
        StudentEnrollment enrollment = studentEnrollmentRepository.findById(studentEnrollmentId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_NOT_FOUND));
        personCodePolicy.requireStudent(enrollment.getStudentPerson());
        accessPolicy.requireCanCreate(session, enrollment, null);
        SessionAttendance entity = baseResolvedAttendance(session, checkInTime);
        entity.setStudentEnrollment(enrollment);
        return saveResolvedAttendance(entity, () -> repository
                .findByClassSession_ClassSessionIdAndStudentEnrollment_StudentEnrollmentId(classSessionId, studentEnrollmentId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.SESSION_ATTENDANCE_NOT_FOUND)));
    }

    private SessionAttendance baseResolvedAttendance(ClassSession session, java.time.LocalDateTime checkInTime) {
        SessionAttendance entity = new SessionAttendance();
        entity.setClassSession(session);
        entity.setCheckInTime(checkInTime);
        entity.setAttendanceStatus(AttendanceStatus.PRESENT);
        entity.setEvaluationStatus(EvaluationStatus.PENDING);
        entity.setNote("FACE_CHECK_IN");
        return entity;
    }

    private SessionAttendance saveResolvedAttendance(
            SessionAttendance entity,
            java.util.function.Supplier<SessionAttendance> existingAfterConflict
    ) {
        try {
            return repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            return existingAfterConflict.get();
        }
    }

    private SessionAttendanceDTO.AllowedActions allowedActions(SessionAttendance entity, AccessContext context) {
        boolean update = context.hasPermission(PermissionDefinition.SESSION_ATTENDANCE_UPDATE.getCode())
                && canUpdate(entity);
        boolean delete = context.hasPermission(PermissionDefinition.SESSION_ATTENDANCE_DELETE.getCode())
                && canDelete(entity);
        return mapper.toAllowedActions(update, delete);
    }

    private boolean canUpdate(SessionAttendance entity) {
        try {
            accessPolicy.requireCanUpdate(entity);
            return true;
        } catch (ApiException ex) {
            return false;
        }
    }

    private boolean canDelete(SessionAttendance entity) {
        try {
            accessPolicy.requireCanDelete(entity);
            return true;
        } catch (ApiException ex) {
            return false;
        }
    }

    private Pageable withDefaultSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_LIST_SORT);
    }

    private SessionAttendanceDTO.Response toResponseForCurrentAccess(SessionAttendance entity) {
        try {
            AccessContext context = currentAccessContextResolver.current();
            return mapper.toResponse(entity, allowedActions(entity, context));
        } catch (RuntimeException exception) {
            return mapper.toResponse(entity, SessionAttendanceDTO.AllowedActions.none());
        }
    }

    private SessionAttendance checkInResolvedStaff(UUID classSessionId, UUID courseStaffAssignmentId,
                                                   LocalDateTime checkInTime) {
        return repository.findByClassSession_ClassSessionIdAndCourseStaffAssignment_CourseStaffAssignmentId(
                        classSessionId, courseStaffAssignmentId)
                .orElseGet(() -> createResolvedStaffAttendance(classSessionId, courseStaffAssignmentId, checkInTime));
    }

    private SessionAttendance createResolvedStaffAttendance(UUID classSessionId, UUID courseStaffAssignmentId,
                                                            LocalDateTime checkInTime) {
        ClassSession session = classSessionRepository.findById(classSessionId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.CLASS_SESSION_NOT_FOUND));
        CourseStaffAssignment assignment = resolveParticipantAssignment(courseStaffAssignmentId);
        accessPolicy.requireCanCreate(session, null, assignment);
        SessionAttendance entity = baseResolvedAttendance(session, checkInTime);
        entity.setCourseStaffAssignment(assignment);
        return saveResolvedAttendance(entity, () -> repository
                .findByClassSession_ClassSessionIdAndCourseStaffAssignment_CourseStaffAssignmentId(
                        classSessionId,
                        courseStaffAssignmentId
                )
                .orElseThrow(() -> new ApiException(TrainingErrorCode.SESSION_ATTENDANCE_NOT_FOUND)));
    }

    private SessionAttendanceDTO.AttendanceStats stats(List<SessionAttendanceDTO.SimpleResponse> records) {
        long total = records.size();
        long present = records.stream().filter(item -> item.attendanceStatus() == AttendanceStatus.PRESENT).count();
        long absent = records.stream().filter(item -> item.attendanceStatus() == AttendanceStatus.ABSENT).count();
        long excused = records.stream().filter(item -> item.attendanceStatus() == AttendanceStatus.EXCUSED).count();
        long makeup = records.stream().filter(item -> item.attendanceStatus() == AttendanceStatus.MAKEUP).count();
        long late = records.stream().filter(item -> item.attendanceStatus() == AttendanceStatus.LATE).count();
        long good = records.stream().filter(item -> item.evaluationStatus() == EvaluationStatus.GOOD).count();
        long average = records.stream().filter(item -> item.evaluationStatus() == EvaluationStatus.AVERAGE).count();
        long weak = records.stream().filter(item -> item.evaluationStatus() == EvaluationStatus.WEAK).count();
        long pending = records.stream().filter(item -> item.evaluationStatus() == EvaluationStatus.PENDING).count();
        double rate = total == 0 ? 0.0 : ((double) (present + makeup + late) / total) * 100.0;
        return new SessionAttendanceDTO.AttendanceStats(
                total,
                rate,
                present,
                absent,
                excused,
                makeup,
                late,
                good,
                average,
                weak,
                pending
        );
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize session attendance command", exception);
        }
    }

    private <T> T readJson(String payloadJson, Class<T> type) {
        try {
            return objectMapper.readValue(payloadJson, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize session attendance command", exception);
        }
    }

    private <T> T readJson(String payloadJson, TypeReference<T> type) {
        try {
            return objectMapper.readValue(payloadJson, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize session attendance command", exception);
        }
    }

    public record IdPayload<T>(UUID id, T request) {
    }
}

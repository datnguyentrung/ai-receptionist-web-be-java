package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.domain.Training.EnrollmentScheduleChangeRequest;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollmentSchedule;
import com.dat.ai_receptionist_web.dto.Training.EnrollmentScheduleChangeRequestDTO;
import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import com.dat.ai_receptionist_web.enums.Training.EnrollmentScheduleChangeRequestStatus;
import com.dat.ai_receptionist_web.enums.Training.EnrollmentScheduleStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.repository.Catalog.CourseScheduleRepository;
import com.dat.ai_receptionist_web.repository.Catalog.TrainingPlanAccessRuleRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.repository.Training.EnrollmentScheduleChangeRequestRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentScheduleRepository;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import com.dat.ai_receptionist_web.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrollmentScheduleChangeRequestService {
    private final EnrollmentScheduleChangeRequestRepository repository;
    private final StudentEnrollmentScheduleRepository enrollmentScheduleRepository;
    private final CourseScheduleRepository courseScheduleRepository;
    private final TrainingPlanAccessRuleRepository accessRuleRepository;
    private final UserRepository userRepository;
    private final CurrentAccessContextResolver accessContextResolver;

    @Transactional(readOnly = true)
    public List<EnrollmentScheduleChangeRequestDTO.ScheduleOption> options(UUID currentScheduleId, LocalDate effectiveFrom) {
        StudentEnrollmentSchedule current = enrollmentScheduleRepository.findDetailedById(currentScheduleId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_SCHEDULE_NOT_FOUND));
        requireOwnerOrAdmin(current);
        if (current.getRequirement() == null || current.getStudentEnrollment().getCoursePurchase().getTrainingPlan() == null) {
            return List.of();
        }
        var plan = current.getStudentEnrollment().getCoursePurchase().getTrainingPlan();
        var candidateCourseIds = accessRuleRepository.findByTrainingPlan_TrainingPlanId(plan.getTrainingPlanId()).stream()
                .filter(rule -> rule.getSessionType() == current.getRequirement().getSessionType())
                .map(rule -> rule.getTargetCourse().getCourseId())
                .distinct()
                .toList();
        if (candidateCourseIds.isEmpty()) {
            return List.of();
        }
        return courseScheduleRepository.findActiveOverlappingByCourseIds(
                        candidateCourseIds, ScheduleStatus.ACTIVE, effectiveFrom, current.getStudentEnrollment().getEndDate())
                .stream()
                .filter(candidate -> isEligible(current, candidate, effectiveFrom))
                .map(candidate -> toOption(candidate, effectiveFrom))
                .toList();
    }

    @Transactional
    public EnrollmentScheduleChangeRequestDTO.Response create(EnrollmentScheduleChangeRequestDTO.CreateRequest request) {
        StudentEnrollmentSchedule current = enrollmentScheduleRepository.findDetailedById(request.currentScheduleId())
                .orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_SCHEDULE_NOT_FOUND));
        requireOwnerOrAdmin(current);
        CourseSchedule target = courseScheduleRepository.findDetailedByIds(List.of(request.targetCourseScheduleId())).stream()
                .findFirst().orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_SCHEDULE_NOT_FOUND));
        validateChange(current, target, request.effectiveFrom());
        EnrollmentScheduleChangeRequest entity = repository.save(EnrollmentScheduleChangeRequest.builder()
                .currentSchedule(current).targetCourseSchedule(target).effectiveFrom(request.effectiveFrom())
                .note(request.note()).status(EnrollmentScheduleChangeRequestStatus.PENDING)
                .requestedByUser(currentUser()).build());
        return response(entity);
    }

    @Transactional
    public EnrollmentScheduleChangeRequestDTO.Response approve(UUID id, EnrollmentScheduleChangeRequestDTO.ReviewRequest request) {
        requireAdmin();
        EnrollmentScheduleChangeRequest entity = pending(id);
        validateChange(entity.getCurrentSchedule(), entity.getTargetCourseSchedule(), entity.getEffectiveFrom());
        StudentEnrollmentSchedule current = entity.getCurrentSchedule();
        current.setEffectiveTo(entity.getEffectiveFrom().minusDays(1));
        enrollmentScheduleRepository.save(StudentEnrollmentSchedule.builder()
                .studentEnrollment(current.getStudentEnrollment()).courseSchedule(entity.getTargetCourseSchedule())
                .requirement(current.getRequirement()).effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(current.getStudentEnrollment().getEndDate()).status(EnrollmentScheduleStatus.ACTIVE).build());
        entity.setStatus(EnrollmentScheduleChangeRequestStatus.APPROVED);
        entity.setReviewedByUser(currentUser());
        entity.setReviewedAt(LocalDateTime.now());
        entity.setReviewNote(request == null ? null : request.note());
        return response(entity);
    }

    @Transactional
    public EnrollmentScheduleChangeRequestDTO.Response reject(UUID id, EnrollmentScheduleChangeRequestDTO.ReviewRequest request) {
        requireAdmin();
        EnrollmentScheduleChangeRequest entity = pending(id);
        entity.setStatus(EnrollmentScheduleChangeRequestStatus.REJECTED);
        entity.setReviewedByUser(currentUser());
        entity.setReviewedAt(LocalDateTime.now());
        entity.setReviewNote(request == null ? null : request.note());
        return response(entity);
    }

    private void validateChange(StudentEnrollmentSchedule current, CourseSchedule target, LocalDate effectiveFrom) {
        if (!isEligible(current, target, effectiveFrom)) {
            throw new ApiException(CatalogErrorCode.COURSE_SCHEDULE_CHANGE_CONFLICT);
        }
    }

    private boolean isEligible(StudentEnrollmentSchedule current, CourseSchedule target, LocalDate effectiveFrom) {
        var enrollment = current.getStudentEnrollment();
        var plan = enrollment.getCoursePurchase().getTrainingPlan();
        if (current.getRequirement() == null
                || current.getCourseSchedule().getCourseScheduleId().equals(target.getCourseScheduleId())
                || effectiveFrom.isBefore(LocalDate.now()) || effectiveFrom.isBefore(enrollment.getStartDate())
                || effectiveFrom.isAfter(enrollment.getEndDate()) || target.getStatus() != ScheduleStatus.ACTIVE
                || target.getStartDate().isAfter(effectiveFrom)
                || (target.getEndDate() != null && target.getEndDate().isBefore(effectiveFrom))
                || target.getSessionType() != current.getRequirement().getSessionType()
                || plan == null || !accessRuleRepository.existsByTrainingPlan_TrainingPlanIdAndTargetCourse_CourseIdAndSessionType(
                plan.getTrainingPlanId(), target.getCourse().getCourseId(), target.getSessionType())) {
            return false;
        }
        boolean overlaps = enrollmentScheduleRepository.findEffectiveByEnrollmentAndDate(
                        enrollment.getStudentEnrollmentId(), effectiveFrom).stream()
                .filter(link -> !link.getStudentEnrollmentScheduleId().equals(current.getStudentEnrollmentScheduleId()))
                .map(StudentEnrollmentSchedule::getCourseSchedule)
                .anyMatch(other -> other.getClassSchedule().getWeekday() == target.getClassSchedule().getWeekday()
                        && other.getClassSchedule().getStartTime().isBefore(target.getClassSchedule().getEndTime())
                        && target.getClassSchedule().getStartTime().isBefore(other.getClassSchedule().getEndTime()));
        return !overlaps && enrollmentScheduleRepository.findEffectiveByCourseScheduleAndDate(
                target.getCourseScheduleId(), effectiveFrom).size() < target.getCourse().getCapacity();
    }

    private EnrollmentScheduleChangeRequestDTO.ScheduleOption toOption(CourseSchedule schedule, LocalDate effectiveFrom) {
        var classSchedule = schedule.getClassSchedule();
        int remaining = Math.max(schedule.getCourse().getCapacity()
                - enrollmentScheduleRepository.findEffectiveByCourseScheduleAndDate(schedule.getCourseScheduleId(), effectiveFrom).size(), 0);
        return new EnrollmentScheduleChangeRequestDTO.ScheduleOption(
                schedule.getCourseScheduleId(), schedule.getCourse().getCourseId(), schedule.getCourse().getName(),
                classSchedule.getBranch().getName(), classSchedule.getWeekday().name(), classSchedule.getStartTime(),
                classSchedule.getEndTime(), schedule.getSessionType(), remaining);
    }

    private EnrollmentScheduleChangeRequest pending(UUID id) {
        EnrollmentScheduleChangeRequest entity = repository.findDetailedByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_SCHEDULE_NOT_FOUND));
        if (entity.getStatus() != EnrollmentScheduleChangeRequestStatus.PENDING) {
            throw new ApiException(CatalogErrorCode.COURSE_SCHEDULE_CHANGE_CONFLICT);
        }
        return entity;
    }

    private void requireOwnerOrAdmin(StudentEnrollmentSchedule schedule) {
        var context = accessContextResolver.current();
        if (!context.hasUnrestrictedRole() && !schedule.getStudentEnrollment().getStudentPerson().getPersonId().equals(context.activePersonId())) {
            throw new ApiException(SecurityErrorCode.ACCESS_DENIED);
        }
    }

    private void requireAdmin() {
        if (!accessContextResolver.current().hasUnrestrictedRole()) throw new ApiException(SecurityErrorCode.ACCESS_DENIED);
    }

    private User currentUser() {
        UUID userId = SecurityUtil.getCurrentUserId().orElseThrow(() -> new ApiException(SecurityErrorCode.MISSING_AUTHENTICATED_USER));
        return userRepository.findById(userId).orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND));
    }

    private EnrollmentScheduleChangeRequestDTO.Response response(EnrollmentScheduleChangeRequest entity) {
        return new EnrollmentScheduleChangeRequestDTO.Response(entity.getEnrollmentScheduleChangeRequestId(),
                entity.getCurrentSchedule().getStudentEnrollment().getStudentEnrollmentId(),
                entity.getCurrentSchedule().getStudentEnrollmentScheduleId(), entity.getTargetCourseSchedule().getCourseScheduleId(),
                entity.getEffectiveFrom(), entity.getNote(), entity.getStatus(), entity.getRequestedByUser().getUserId(),
                entity.getReviewedByUser() == null ? null : entity.getReviewedByUser().getUserId(), entity.getReviewedAt(), entity.getReviewNote());
    }
}

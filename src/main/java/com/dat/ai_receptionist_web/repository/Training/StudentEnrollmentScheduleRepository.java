package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentEnrollmentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

public interface StudentEnrollmentScheduleRepository extends JpaRepository<StudentEnrollmentSchedule, UUID> {
    boolean existsByRequirement_TrainingPlanRequirementId(UUID requirementId);
    boolean existsByCourseSchedule_CourseScheduleId(UUID courseScheduleId);
    @Query("""
        select link from StudentEnrollmentSchedule link
        join fetch link.studentEnrollment enrollment
        join fetch link.courseSchedule courseSchedule
        join fetch courseSchedule.classSchedule classSchedule
        join fetch classSchedule.branch
        where link.studentEnrollmentScheduleId = :id
    """)
    Optional<StudentEnrollmentSchedule> findDetailedById(@Param("id") UUID id);

    @Query("""
        select link from StudentEnrollmentSchedule link
        join fetch link.studentEnrollment enrollment
        join fetch link.courseSchedule courseSchedule
        join fetch courseSchedule.classSchedule classSchedule
        join fetch classSchedule.branch
        where enrollment.studentEnrollmentId in :enrollmentIds
        order by enrollment.studentEnrollmentId, link.studentEnrollmentScheduleId
    """)
    List<StudentEnrollmentSchedule> findDetailedByStudentEnrollmentIds(
            @Param("enrollmentIds") Collection<UUID> enrollmentIds);

    @Query("""
        select link from StudentEnrollmentSchedule link
        join fetch link.studentEnrollment enrollment
        join fetch link.courseSchedule courseSchedule
        join fetch courseSchedule.classSchedule classSchedule
        join fetch classSchedule.branch
        where enrollment.studentEnrollmentId = :enrollmentId
        order by link.studentEnrollmentScheduleId
    """)
    List<StudentEnrollmentSchedule> findDetailedByStudentEnrollmentId(
            @Param("enrollmentId") UUID enrollmentId);

    @Query("""
        select link from StudentEnrollmentSchedule link
        join fetch link.studentEnrollment enrollment
        join fetch link.courseSchedule courseSchedule
        join fetch courseSchedule.classSchedule classSchedule
        join fetch classSchedule.branch
        where enrollment.studentEnrollmentId = :enrollmentId
          and link.studentEnrollmentScheduleId = :id
    """)
    Optional<StudentEnrollmentSchedule> findDetailedByStudentEnrollmentIdAndId(
            @Param("enrollmentId") UUID enrollmentId,
            @Param("id") UUID id);

    boolean existsByStudentEnrollment_StudentEnrollmentIdAndCourseSchedule_CourseScheduleId(
            UUID studentEnrollmentId, UUID courseScheduleId);

    long countByStudentEnrollment_StudentEnrollmentId(UUID studentEnrollmentId);

    @Query("""
        select link from StudentEnrollmentSchedule link
        join fetch link.studentEnrollment enrollment
        where link.courseSchedule.courseScheduleId = :courseScheduleId
          and enrollment.status in (com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus.ACTIVE,
                                    com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus.PENDING_START)
          and enrollment.startDate <= :date and enrollment.endDate >= :date
          and link.effectiveFrom <= :date and (link.effectiveTo is null or link.effectiveTo >= :date)
    """)
    List<StudentEnrollmentSchedule> findEffectiveByCourseScheduleAndDate(
            @Param("courseScheduleId") UUID courseScheduleId, @Param("date") LocalDate date);

    @Query("""
        select link from StudentEnrollmentSchedule link
        join fetch link.studentEnrollment enrollment
        join fetch link.courseSchedule courseSchedule
        left join fetch link.requirement
        where link.courseSchedule.courseScheduleId in :courseScheduleIds
          and enrollment.status in (com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus.ACTIVE,
                                    com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus.PENDING_START)
          and enrollment.startDate <= :untilDate and enrollment.endDate >= :fromDate
          and link.effectiveFrom <= :untilDate and (link.effectiveTo is null or link.effectiveTo >= :fromDate)
    """)
    List<StudentEnrollmentSchedule> findPotentiallyEffectiveByCourseScheduleIdsAndDateRange(
            @Param("courseScheduleIds") Collection<UUID> courseScheduleIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("untilDate") LocalDate untilDate);

    @Query("""
        select link from StudentEnrollmentSchedule link
        join fetch link.courseSchedule courseSchedule
        join fetch courseSchedule.classSchedule
        where link.studentEnrollment.studentEnrollmentId = :enrollmentId
          and link.status = com.dat.ai_receptionist_web.enums.Training.EnrollmentScheduleStatus.ACTIVE
          and (link.effectiveFrom is null or link.effectiveFrom <= :date)
          and (link.effectiveTo is null or link.effectiveTo >= :date)
    """)
    List<StudentEnrollmentSchedule> findEffectiveByEnrollmentAndDate(
            @Param("enrollmentId") UUID enrollmentId, @Param("date") LocalDate date);
}

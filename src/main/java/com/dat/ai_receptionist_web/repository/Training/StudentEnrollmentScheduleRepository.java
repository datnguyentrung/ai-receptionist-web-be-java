package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentEnrollmentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentEnrollmentScheduleRepository extends JpaRepository<StudentEnrollmentSchedule, UUID> {
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
}

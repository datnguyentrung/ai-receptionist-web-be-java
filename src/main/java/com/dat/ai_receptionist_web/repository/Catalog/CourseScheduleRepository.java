package com.dat.ai_receptionist_web.repository.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CourseScheduleRepository extends JpaRepository<CourseSchedule, UUID> {
    long countByClassSchedule_ScheduleId(UUID scheduleId);
    @Query("""
        select cs from CourseSchedule cs join fetch cs.classSchedule s join fetch s.branch
        where cs.course.courseId = :courseId order by cs.startDate, cs.courseScheduleId
    """)
    List<CourseSchedule> findDetailedByCourseId(@Param("courseId") UUID courseId);

    @Query(value = """
        select cs from CourseSchedule cs
        join fetch cs.course
        join fetch cs.classSchedule s
        join fetch s.branch
    """, countQuery = "select count(cs) from CourseSchedule cs")
    Page<CourseSchedule> findAllDetailed(Pageable pageable);

    @Query("""
        select cs from CourseSchedule cs
        join fetch cs.course
        join fetch cs.classSchedule s
        join fetch s.branch
        where cs.courseScheduleId = :id
    """)
    java.util.Optional<CourseSchedule> findDetailedById(@Param("id") UUID id);

    /**
     * Loads the schedules for a page of courses in one query.  This must be
     * used after the course page has been selected, rather than fetch-joining
     * Course.courseSchedules in the paginated query.
     */
    @Query("""
        select cs from CourseSchedule cs
        join fetch cs.course c
        join fetch cs.classSchedule s
        join fetch s.branch
        where c.courseId in :courseIds
        order by c.courseId, cs.startDate, cs.courseScheduleId
    """)
    List<CourseSchedule> findDetailedByCourseIds(@Param("courseIds") List<UUID> courseIds);

    @Query("""
        select cs from CourseSchedule cs
        join fetch cs.course
        join fetch cs.classSchedule s
        join fetch s.branch
        where cs.courseScheduleId in :scheduleIds
    """)
    List<CourseSchedule> findDetailedByIds(@Param("scheduleIds") java.util.Collection<UUID> scheduleIds);

    @Query("""
        select cs from CourseSchedule cs
        join fetch cs.classSchedule s
        join fetch s.branch
        where cs.course.courseId = :courseId
          and cs.courseScheduleId = :courseScheduleId
    """)
    java.util.Optional<CourseSchedule> findDetailedByCourseIdAndCourseScheduleId(
            @Param("courseId") UUID courseId,
            @Param("courseScheduleId") UUID courseScheduleId);

    @Query("""
        select cs from CourseSchedule cs join fetch cs.course c join fetch cs.classSchedule s
        where c.courseId = :courseId and cs.status = :status
          and cs.startDate <= :date and (cs.endDate is null or cs.endDate >= :date)
    """)
    List<CourseSchedule> findEffectiveByCourseIdAndDate(UUID courseId, ScheduleStatus status, LocalDate date);
}

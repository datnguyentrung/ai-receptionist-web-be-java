package com.dat.ai_receptionist_web.repository.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    @Query("""
        select cs from CourseSchedule cs join fetch cs.course c join fetch cs.classSchedule s
        where c.courseId = :courseId and cs.status = :status
          and cs.startDate <= :date and (cs.endDate is null or cs.endDate >= :date)
    """)
    List<CourseSchedule> findEffectiveByCourseIdAndDate(UUID courseId, ScheduleStatus status, LocalDate date);
}

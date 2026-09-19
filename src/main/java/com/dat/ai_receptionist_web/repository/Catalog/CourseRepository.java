package com.dat.ai_receptionist_web.repository.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.*;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    @Query(value = """
        select c
        from Course c
        join fetch c.classSchedule classSchedule
        join fetch classSchedule.branch
        left join fetch c.nextClassSchedule nextClassSchedule
        left join fetch nextClassSchedule.branch
    """,
            countQuery = """
        select count(c)
        from Course c
    """)
    Page<Course> findAllDetailed(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.courseId = :id")
    Optional<Course> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
        SELECT c
        FROM Course c
        WHERE c.status = :status
          AND (
              c.classSessionGeneratedUntil IS NULL
              OR c.classSessionGeneratedUntil < :threshold
          )
    """)
    List<Course> findCoursesNeedClassSessionGeneration(
            @Param("status") CourseStatus status,
            @Param("threshold") LocalDate threshold
    );

    @Query("""
        SELECT c
        FROM Course c
        WHERE c.nextScheduleEffectiveFrom IS NOT NULL
          AND c.nextScheduleEffectiveFrom <= :today
    """)
    List<Course> findCoursesWithPendingScheduleDue(@Param("today") LocalDate today);

    long countByClassSchedule_ScheduleIdAndStatusNot(UUID scheduleId, CourseStatus status);

    long countByNextClassSchedule_ScheduleId(UUID scheduleId);
}

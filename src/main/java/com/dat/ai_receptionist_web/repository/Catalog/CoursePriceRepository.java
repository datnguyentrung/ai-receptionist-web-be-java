package com.dat.ai_receptionist_web.repository.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.CoursePrice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface CoursePriceRepository extends JpaRepository<CoursePrice, UUID> {
    @Query(value = """
            select cp
            from CoursePrice cp
            join fetch cp.course course
            left join fetch course.courseSchedules courseSchedule
            left join fetch courseSchedule.classSchedule classSchedule
            join fetch classSchedule.branch
            """,
            countQuery = """
            select count(cp)
            from CoursePrice cp
            """)
    Page<CoursePrice> findAllDetailed(Pageable pageable);

    @EntityGraph(attributePaths = {"course", "course.courseSchedules", "course.courseSchedules.classSchedule"})
    @Query("select cp from CoursePrice cp where cp.coursePriceId = :id")
    Optional<CoursePrice> findForPurchase(@Param("id") UUID id);
}

package com.dat.ai_receptionist_web.repository.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.ClassSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, UUID> {
    @Query(value = """
            select cs
            from ClassSchedule cs
            join fetch cs.branch
            """,
            countQuery = """
            select count(cs)
            from ClassSchedule cs
            """)
    Page<ClassSchedule> findAllDetailed(Pageable pageable);
}

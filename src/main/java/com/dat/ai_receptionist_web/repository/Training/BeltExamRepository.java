package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.BeltExam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BeltExamRepository extends JpaRepository<BeltExam, UUID> {
    @Query(value = """
        select b
        from BeltExam b
        left join fetch b.person person
    """,
            countQuery = """
        select count(b)
        from BeltExam b
    """)
    Page<BeltExam> findAllDetailed(Pageable pageable);

    @Query("""
        select b
        from BeltExam b
        left join fetch b.person person
        left join fetch person.position
        left join fetch b.createdByUser
        where b.beltExamId = :id
    """)
    Optional<BeltExam> findDetailedById(@Param("id") UUID id);
}

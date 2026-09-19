package com.dat.ai_receptionist_web.repository.Skill;

import com.dat.ai_receptionist_web.domain.Skill.FitnessRecord;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;
import java.util.UUID;

@Repository
public interface FitnessRecordRepository extends JpaRepository<FitnessRecord, Long>, JpaSpecificationExecutor<FitnessRecord> {
    @Query(value = """
            select fr
            from FitnessRecord fr
            join fetch fr.student
            join fetch fr.fitness
            join fetch fr.recordedByCoach
            """,
            countQuery = """
            select count(fr)
            from FitnessRecord fr
            """)
    Page<FitnessRecord> findAllDetailed(Pageable pageable);

    @Override
    @NonNull
    Page<FitnessRecord> findAll(
            @NonNull Specification<FitnessRecord> spec, @NonNull Pageable pageable
    );

    @Query(
            value = """
                    SELECT fr.fitnessRecordId AS id,
                           fr.createdAt AS createdAt,
                           fr.recordDate AS recordDate,
                           fr.duration AS duration,
                           fr.fitness.amount AS amount,
                           fr.fitness.scheduleLevel AS scheduleLevel,
                           student.personId AS studentPersonId,
                           student.fullName AS studentFullName,
                           student.personCode AS personCode,
                           student.currentBelt AS studentBelt
                    FROM FitnessRecord fr
                    JOIN fr.student student
                    WHERE (:search IS NULL OR :search = '' OR LOWER(student.fullName) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:scheduleLevel IS NULL OR fr.fitness.scheduleLevel = :scheduleLevel)
                    """,
            countQuery = """
                    SELECT COUNT(fr.fitnessRecordId)
                    FROM FitnessRecord fr
                    JOIN fr.student student
                    WHERE (:search IS NULL OR :search = '' OR LOWER(student.fullName) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:scheduleLevel IS NULL OR fr.fitness.scheduleLevel = :scheduleLevel)
                    """
    )
    Page<FitnessRecordListRow> findListRows(
            @Param("search") String search,
            @Param("scheduleLevel") ScheduleLevel scheduleLevel,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"student"})
    @Query("""
                SELECT fr FROM FitnessRecord fr
                WHERE year(fr.recordDate) = :year
                  AND quarter(fr.recordDate) = :quarter
                  AND fr.fitness.scheduleLevel = :scheduleLevel
                  AND fr.student.personCode IN :personCodes
                ORDER BY fr.student.personCode, fr.fitnessRecordId
            """)
    List<FitnessRecord> findRecordsForQuarterAndStudents(
            @Param("year") int year,
            @Param("quarter") int quarter,
            @Param("scheduleLevel") ScheduleLevel scheduleLevel,
            @Param("personCodes") Collection<String> personCodes
    );

    @EntityGraph(attributePaths = {"student"})
    @Query("""
                SELECT fr FROM FitnessRecord fr
                WHERE year(fr.recordDate) = :year
                  AND quarter(fr.recordDate) = :quarter
                  AND fr.fitness.scheduleLevel = :scheduleLevel
                  AND fr.student.personCode = :personCode
                ORDER BY fr.fitnessRecordId
            """)
    List<FitnessRecord> findRecordsForSingleStudent(
            @Param("year") int year,
            @Param("quarter") int quarter,
            @Param("scheduleLevel") ScheduleLevel scheduleLevel,
            @Param("personCode") String personCode);

    interface FitnessRecordListRow {
        Long getId();

        LocalDateTime getCreatedAt();

        LocalDate getRecordDate();

        Integer getDuration();

        Integer getAmount();

        ScheduleLevel getScheduleLevel();

        UUID getStudentPersonId();

        String getStudentFullName();

        String getPersonCode();

        com.dat.ai_receptionist_web.enums.Core.Belt getStudentBelt();
    }
}

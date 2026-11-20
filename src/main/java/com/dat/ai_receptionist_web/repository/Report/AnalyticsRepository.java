package com.dat.ai_receptionist_web.repository.Report;

import com.dat.ai_receptionist_web.domain.Training.StudentLifecycleEvent;
import com.dat.ai_receptionist_web.enums.Training.EventStatus;
import com.dat.ai_receptionist_web.enums.Training.EventType;
import com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalyticsRepository extends JpaRepository<StudentLifecycleEvent, UUID> {
    @Query("""
            select e.studentPersonId.personId as personId,
                   e.studentPersonId.birthDate as birthDate,
                   e.studentPersonId.gender as gender,
                   e.eventType as eventType,
                   e.eventDate as eventDate
            from StudentLifecycleEvent e
            where e.eventStatus = :status
              and e.eventType <> :newType
              and e.eventDate between :from and :to
            order by e.eventDate, e.eventId
            """)
    List<MovementFact> findStudentMovements(
            @Param("status") EventStatus status,
            @Param("newType") EventType newType,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("""
            select p.personId as personId,
                   p.birthDate as birthDate,
                   p.gender as gender,
                   com.dat.ai_receptionist_web.enums.Training.EventType.NEW as eventType,
                   p.startDate as eventDate
            from Person p
            where p.startDate between :from and :to
            """)
    List<MovementFact> findNewStudents(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("""
            select count(distinct e.studentPerson.personId)
            from StudentEnrollment e
            where e.startDate <= :asOf
              and e.endDate >= :asOf
              and e.status <> :cancelled
            """)
    int countActiveStudents(@Param("asOf") LocalDate asOf,
                            @Param("cancelled") StudentEnrollmentStatus cancelled);

    @Query("""
            select count(distinct e.studentPerson.personId)
            from StudentEnrollment e
            where e.startDate <= :to
              and e.endDate >= :from
              and e.status <> :cancelled
            """)
    int countStudents(@Param("from") LocalDate from,
                      @Param("to") LocalDate to,
                      @Param("cancelled") StudentEnrollmentStatus cancelled);

    interface MovementFact {
        UUID getPersonId();
        LocalDate getBirthDate();
        Boolean getGender();
        EventType getEventType();
        LocalDate getEventDate();

        default UUID personId() { return getPersonId(); }
        default LocalDate birthDate() { return getBirthDate(); }
        default Boolean gender() { return getGender(); }
        default String eventType() { return getEventType().name(); }
        default LocalDate eventDate() { return getEventDate(); }
    }
}

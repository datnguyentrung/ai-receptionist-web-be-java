package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ClassSession c join fetch c.course join fetch c.courseSchedule where c.classSessionId = :id")
    Optional<ClassSession> findDetailedByIdForUpdate(@Param("id") UUID id);

    @Query("select c from ClassSession c join fetch c.course join fetch c.courseSchedule where c.classSessionId = :id")
    Optional<ClassSession> findDetailedById(@Param("id") UUID id);

    boolean existsByCourseSchedule_CourseScheduleId(UUID courseScheduleId);
    @Modifying
    @Query(value = """
        update training.class_session
           set schedule_level = :level
         where course_id = :courseId
           and session_date >= :fromDate
           and status in ('SCHEDULED', 'POSTPONED')
    """, nativeQuery = true)
    int updateFutureScheduleLevel(
            @Param("courseId") UUID courseId,
            @Param("fromDate") LocalDate fromDate,
            @Param("level") String level
    );

    @Query("""
        select coalesce(sum(case when c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.SCHEDULED then 1 else 0 end), 0) as scheduled,
               coalesce(sum(case when c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.ACTIVE then 1 else 0 end), 0) as active,
               coalesce(sum(case when c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.COMPLETED then 1 else 0 end), 0) as completed,
               coalesce(sum(case when c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.TERMINATED then 1 else 0 end), 0) as terminated,
               coalesce(sum(case when c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.CANCELLED then 1 else 0 end), 0) as cancelled,
               coalesce(sum(case when c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.POSTPONED then 1 else 0 end), 0) as postponed,
               count(c) as total
        from CourseSchedule schedule
        left join ClassSession c on c.courseSchedule.courseScheduleId = schedule.courseScheduleId
            and c.sessionDate >= schedule.startDate
            and (schedule.endDate is null or c.sessionDate <= schedule.endDate)
        where schedule.courseScheduleId = :courseScheduleId
    """)
    LearningProgressRow findLearningProgressByCourseScheduleId(@Param("courseScheduleId") UUID courseScheduleId);

    interface LearningProgressRow {
        long getScheduled();

        long getActive();

        long getCompleted();

        long getTerminated();

        long getCancelled();

        long getPostponed();

        long getTotal();
    }

    @Query("""
        select schedule.courseScheduleId as courseScheduleId,
               coalesce(sum(case when c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.SCHEDULED then 1 else 0 end), 0) as scheduled,
               coalesce(sum(case when c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.ACTIVE then 1 else 0 end), 0) as active,
               coalesce(sum(case when c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.COMPLETED then 1 else 0 end), 0) as completed,
               coalesce(sum(case when c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.TERMINATED then 1 else 0 end), 0) as terminated,
               coalesce(sum(case when c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.CANCELLED then 1 else 0 end), 0) as cancelled,
               coalesce(sum(case when c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.POSTPONED then 1 else 0 end), 0) as postponed,
               count(c) as total
        from CourseSchedule schedule
        left join ClassSession c on c.courseSchedule.courseScheduleId = schedule.courseScheduleId
            and c.sessionDate >= schedule.startDate
            and (schedule.endDate is null or c.sessionDate <= schedule.endDate)
        where schedule.courseScheduleId in :courseScheduleIds
        group by schedule.courseScheduleId
    """)
    List<CourseScheduleLearningProgressRow> findLearningProgressByCourseScheduleIds(
            @Param("courseScheduleIds") Collection<UUID> courseScheduleIds
    );

    interface CourseScheduleLearningProgressRow extends LearningProgressRow {
        UUID getCourseScheduleId();
    }

    @Modifying
    @Query("""
        update ClassSession c
           set c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.ACTIVE
         where c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.SCHEDULED
           and c.sessionDate = :sessionDate
           and c.startTime <= :thresholdTime
    """)
    int activateScheduledSessions(
            @Param("sessionDate") LocalDate sessionDate,
            @Param("thresholdTime") LocalTime thresholdTime
    );

    @Query("""
        select c
        from ClassSession c
        where c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.ACTIVE
          and c.attendanceClosed = true
          and (
              c.sessionDate < :thresholdDate
              or (c.sessionDate = :thresholdDate and c.endTime <= :thresholdTime)
          )
    """)
    List<ClassSession> findSessionsToComplete(
            @Param("thresholdDate") LocalDate thresholdDate,
            @Param("thresholdTime") LocalTime thresholdTime
    );

    @Modifying
    @Query("""
        update ClassSession c
           set c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.COMPLETED
         where c.classSessionId = :classSessionId
           and c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.ACTIVE
    """)
    int markSessionCompleted(@Param("classSessionId") UUID classSessionId);

    @Query("""
        select c
        from ClassSession c
        where c.status = com.dat.ai_receptionist_web.enums.Training.SessionStatus.ACTIVE
          and c.attendanceClosed = false
          and c.sessionDate <= :sessionDate
          and (c.attendanceReopenedUntil is null or c.attendanceReopenedUntil <= current_timestamp)
    """)
    List<ClassSession> findSessionsToClose(@Param("sessionDate") LocalDate sessionDate);

    @Query("""
        select c
        from ClassSession c
        where c.course.courseId = :courseId
          and c.sessionDate >= :fromDate
          and c.status in (
              com.dat.ai_receptionist_web.enums.Training.SessionStatus.SCHEDULED,
              com.dat.ai_receptionist_web.enums.Training.SessionStatus.POSTPONED
          )
          and (c.sessionDate > :today or c.endTime > :nowTime)
    """)
    List<ClassSession> findUpcomingSessionsToCancel(
            @Param("courseId") UUID courseId,
            @Param("fromDate") LocalDate fromDate,
            @Param("today") LocalDate today,
            @Param("nowTime") LocalTime nowTime
    );

    @Query("""
        select c.sessionDate
        from ClassSession c
        where c.course.courseId = :courseId
          and c.sessionDate between :fromDate and :untilDate
          and c.status <> :excluded
    """)
    List<LocalDate> findSessionDatesByCourseAndRange(
            @Param("courseId") UUID courseId,
            @Param("fromDate") LocalDate fromDate,
            @Param("untilDate") LocalDate untilDate,
            @Param("excluded") SessionStatus excluded
    );

    boolean existsByCourse_CourseIdAndSessionDateAndStatusNot(
            UUID courseId, LocalDate sessionDate, SessionStatus status);

    List<ClassSession> findByCourse_CourseIdAndSessionDateBetweenAndStatusNot(
            UUID courseId, LocalDate fromDate, LocalDate untilDate, SessionStatus status);

    @Query("""
        select c
        from ClassSession c
        join fetch c.course course
        where c.sessionDate >= :fromDate
          and c.sessionDate <= :toDate
          and (
              :unrestricted = true
              or exists (
                  select 1
                  from CourseStaffAssignment a
                  where a.staffPerson.personId = :activePersonId
                    and a.courseSchedule.courseScheduleId = c.courseSchedule.courseScheduleId
                    and a.startDate <= c.sessionDate
                    and (a.endDate is null or a.endDate >= c.sessionDate)
                    and a.assignmentStatus in (
                        com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ACTIVE,
                        com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ENDED
                    )
              )
          )
        order by c.sessionDate asc, c.startTime asc
    """)
    List<ClassSession> findCalendarSessions(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("activePersonId") UUID activePersonId,
            @Param("unrestricted") boolean unrestricted
    );

    @Query("""
        select c
        from ClassSession c
        left join fetch c.course
        left join fetch c.courseSchedule courseSchedule
        left join fetch courseSchedule.classSchedule classSchedule
        left join fetch classSchedule.branch
        where :unrestricted = true
           or exists (
               select 1
               from CourseStaffAssignment a
               where a.staffPerson.personId = :activePersonId
                 and a.courseSchedule.courseScheduleId = c.courseSchedule.courseScheduleId
                 and a.startDate <= c.sessionDate
                 and (a.endDate is null or a.endDate >= c.sessionDate)
                 and a.assignmentStatus in (
                     com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ACTIVE,
                     com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ENDED
                 )
           )
    """)
    Page<ClassSession> findAccessible(
            @Param("activePersonId") UUID activePersonId,
            @Param("unrestricted") boolean unrestricted,
            Pageable pageable
    );

    @Query("""
        select c
        from ClassSession c
        left join fetch c.course
        left join fetch c.courseSchedule courseSchedule
        left join fetch courseSchedule.classSchedule classSchedule
        left join fetch classSchedule.branch
        where c.classSessionId = :id
          and (
              :unrestricted = true
              or exists (
                  select 1
                  from CourseStaffAssignment a
                  where a.staffPerson.personId = :activePersonId
                    and a.courseSchedule.courseScheduleId = c.courseSchedule.courseScheduleId
                    and a.startDate <= c.sessionDate
                    and (a.endDate is null or a.endDate >= c.sessionDate)
                    and a.assignmentStatus in (
                        com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ACTIVE,
                        com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus.ENDED
                    )
              )
          )
    """)
    Optional<ClassSession> findAccessibleById(
            @Param("id") UUID id,
            @Param("activePersonId") UUID activePersonId,
            @Param("unrestricted") boolean unrestricted
    );

    @Query("""
        select c from ClassSession c
        join fetch c.course
        join fetch c.courseSchedule schedule
        join fetch schedule.classSchedule classSchedule
        join fetch classSchedule.branch
        where c.sessionDate between :fromDate and :toDate
          and c.status in (com.dat.ai_receptionist_web.enums.Training.SessionStatus.SCHEDULED,
                           com.dat.ai_receptionist_web.enums.Training.SessionStatus.ACTIVE)
        order by c.sessionDate, c.startTime
    """)
    List<ClassSession> findFutureBookableCandidates(@Param("fromDate") LocalDate fromDate,
                                                     @Param("toDate") LocalDate toDate);
}

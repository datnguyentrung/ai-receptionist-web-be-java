package com.dat.ai_receptionist_web.repository.Training;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AttendanceContextRepository extends Repository<com.dat.ai_receptionist_web.domain.Training.ClassSession, UUID> {
    @Query(value = """
        with active_sessions as (
            select
                cs.class_session_id,
                cs.course_id,
                cs.session_date,
                cs.start_time,
                cs.end_time,
                case
                    when c.next_schedule_id is not null
                     and c.next_schedule_effective_from is not null
                     and cs.session_date >= c.next_schedule_effective_from
                    then c.next_schedule_id
                    else c.schedule_id
                end as effective_schedule_id
            from training.class_session cs
            join catalog.course c on c.course_id = cs.course_id
            where
                cs.status = 'ACTIVE'
                and (
                    cs.is_attendance_closed = false
                    or (
                        cs.attendance_reopened_until is not null
                        and cs.attendance_reopened_until > current_timestamp
                    )
                )
        ),
        student_candidates as (
            select
                'STUDENT' as "contextType",
                a.class_session_id as "classSessionId",
                se.student_enrollment_id as "participationId",
                null::varchar as "assignmentType",
                a.session_date as "sessionDate",
                a.start_time as "startTime",
                a.end_time as "endTime"
            from active_sessions a
            join training.student_enrollment se
              on se.class_schedule_id = a.effective_schedule_id
            where
                se.student_person_id = :personId
                and se.status = 'ACTIVE'
                and a.session_date between se.start_date and se.end_date
        ),
        staff_candidates as (
            select
                'STAFF' as "contextType",
                a.class_session_id as "classSessionId",
                csa.course_staff_assignment_id as "participationId",
                csa.assignment_type::varchar as "assignmentType",
                a.session_date as "sessionDate",
                a.start_time as "startTime",
                a.end_time as "endTime"
            from active_sessions a
            join training.course_staff_assignment csa
              on csa.course_id = a.course_id
            where
                csa.staff_person_id = :personId
                and csa.assignment_status = 'ACTIVE'
                and a.session_date >= csa.start_date
                and (csa.end_date is null or a.session_date <= csa.end_date)
        )
        select * from student_candidates
        union all
        select * from staff_candidates
        order by "sessionDate", "startTime", "classSessionId", "participationId"
        """, nativeQuery = true)
    List<CheckInCandidateRow> findCheckInCandidates(
            @Param("personId") UUID personId
    );

    interface CheckInCandidateRow {
        String getContextType();
        UUID getClassSessionId();
        UUID getParticipationId();
        String getAssignmentType();
        java.time.LocalDate getSessionDate();
        java.time.LocalTime getStartTime();
        java.time.LocalTime getEndTime();
    }
}

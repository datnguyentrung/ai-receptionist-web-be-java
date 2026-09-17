package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.LeaveRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    @Query(value = """
        select lr
        from LeaveRequest lr
        left join fetch lr.person person
        left join fetch lr.leaveClassSession leaveSession
        left join fetch leaveSession.course leaveSessionCourse
        left join fetch leaveSessionCourse.classSchedule leaveSessionSchedule
        left join fetch leaveSessionSchedule.branch
        left join fetch leaveSessionCourse.nextClassSchedule leaveSessionNextSchedule
        left join fetch leaveSessionNextSchedule.branch
        left join fetch lr.makeupClassSession makeupSession
        left join fetch makeupSession.course makeupSessionCourse
        left join fetch makeupSessionCourse.classSchedule makeupSessionSchedule
        left join fetch makeupSessionSchedule.branch
        left join fetch makeupSessionCourse.nextClassSchedule makeupSessionNextSchedule
        left join fetch makeupSessionNextSchedule.branch
    """,
            countQuery = """
        select count(lr)
        from LeaveRequest lr
    """)
    Page<LeaveRequest> findAllDetailed(Pageable pageable);

    @Query("""
        select lr
        from LeaveRequest lr
        left join fetch lr.person person
        left join fetch person.position
        left join fetch lr.leaveClassSession leaveSession
        left join fetch leaveSession.course leaveSessionCourse
        left join fetch leaveSessionCourse.classSchedule leaveSessionSchedule
        left join fetch leaveSessionSchedule.branch
        left join fetch leaveSessionCourse.nextClassSchedule leaveSessionNextSchedule
        left join fetch leaveSessionNextSchedule.branch
        left join fetch lr.makeupClassSession makeupSession
        left join fetch makeupSession.course makeupSessionCourse
        left join fetch makeupSessionCourse.classSchedule makeupSessionSchedule
        left join fetch makeupSessionSchedule.branch
        left join fetch makeupSessionCourse.nextClassSchedule makeupSessionNextSchedule
        left join fetch makeupSessionNextSchedule.branch
        left join fetch lr.createdByUser
        left join fetch lr.reviewedByUser
        where lr.leaveRequestId = :id
    """)
    Optional<LeaveRequest> findDetailedById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lr from LeaveRequest lr where lr.leaveRequestId = :id")
    Optional<LeaveRequest> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
        select lr
        from LeaveRequest lr
        where lr.leaveClassSession.classSessionId in :sessionIds
           or lr.makeupClassSession.classSessionId in :sessionIds
    """)
    List<LeaveRequest> findByReferencedSessionIds(@Param("sessionIds") Collection<UUID> sessionIds);
}

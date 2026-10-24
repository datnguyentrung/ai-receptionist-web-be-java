package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentSessionBooking;
import com.dat.ai_receptionist_web.enums.Training.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentSessionBookingRepository extends JpaRepository<StudentSessionBooking, UUID> {
    boolean existsByRequirement_TrainingPlanRequirementId(UUID requirementId);
    long countByClassSession_ClassSessionIdAndStatusNot(UUID classSessionId, BookingStatus status);
    Optional<StudentSessionBooking> findByStudentSessionBookingIdAndEnrollment_StudentEnrollmentId(UUID bookingId, UUID enrollmentId);
    boolean existsByEnrollment_StudentEnrollmentIdAndClassSession_ClassSessionId(UUID enrollmentId, UUID classSessionId);
    List<StudentSessionBooking> findByClassSession_ClassSessionIdAndStatus(UUID classSessionId, BookingStatus status);

    @Query("""
        select b.enrollment.studentEnrollmentId as enrollmentId,
               b.classSession.classSessionId as classSessionId
        from StudentSessionBooking b
        where b.classSession.classSessionId in :classSessionIds
    """)
    List<EnrollmentSessionPair> findEnrollmentSessionPairsByClassSessionIds(
            @Param("classSessionIds") Collection<UUID> classSessionIds);

    interface EnrollmentSessionPair {
        UUID getEnrollmentId();
        UUID getClassSessionId();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from StudentSessionBooking b join fetch b.enrollment e join fetch e.coursePurchase p where b.studentSessionBookingId = :id")
    Optional<StudentSessionBooking> findDetailedByIdForUpdate(@Param("id") UUID id);

    Optional<StudentSessionBooking> findByLeaveRequest_LeaveRequestId(UUID leaveRequestId);

    @Query("""
        select b from StudentSessionBooking b
        join fetch b.classSession session
        join fetch session.course
        where b.enrollment.studentEnrollmentId = :enrollmentId
        order by session.sessionDate, session.startTime
    """)
    List<StudentSessionBooking> findDetailedByEnrollmentId(@Param("enrollmentId") UUID enrollmentId);

    @Query("""
        select count(b) from StudentSessionBooking b
        where b.enrollment.studentEnrollmentId = :enrollmentId
          and b.status = com.dat.ai_receptionist_web.enums.Training.BookingStatus.RESERVED
          and b.source <> com.dat.ai_receptionist_web.enums.Training.BookingSource.MAKEUP
          and b.classSession.sessionDate between :weekStart and :weekEnd
    """)
    long countNormalReservedInWeek(@Param("enrollmentId") UUID enrollmentId,
                                   @Param("weekStart") LocalDate weekStart,
                                   @Param("weekEnd") LocalDate weekEnd);

    @Query("""
        select count(b) from StudentSessionBooking b
        where b.enrollment.studentEnrollmentId = :enrollmentId
          and b.status = com.dat.ai_receptionist_web.enums.Training.BookingStatus.RESERVED
          and b.source <> com.dat.ai_receptionist_web.enums.Training.BookingSource.MAKEUP
    """)
    long countNormalReserved(@Param("enrollmentId") UUID enrollmentId);

    @Query("""
        select count(b) from StudentSessionBooking b
        where b.enrollment.studentEnrollmentId = :enrollmentId
          and b.status = com.dat.ai_receptionist_web.enums.Training.BookingStatus.SETTLED
          and b.source <> com.dat.ai_receptionist_web.enums.Training.BookingSource.MAKEUP
    """)
    long countNormalSettled(@Param("enrollmentId") UUID enrollmentId);

    @Query("""
        select (count(b) > 0) from StudentSessionBooking b
        where b.enrollment.studentEnrollmentId = :enrollmentId
          and b.status = com.dat.ai_receptionist_web.enums.Training.BookingStatus.RESERVED
          and b.classSession.sessionDate = :sessionDate
          and b.classSession.startTime < :endTime
          and b.classSession.endTime > :startTime
    """)
    boolean existsReservedTimeConflict(@Param("enrollmentId") UUID enrollmentId,
                                       @Param("sessionDate") LocalDate sessionDate,
                                       @Param("startTime") java.time.LocalTime startTime,
                                       @Param("endTime") java.time.LocalTime endTime);
}

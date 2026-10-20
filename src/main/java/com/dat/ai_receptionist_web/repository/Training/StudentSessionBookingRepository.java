package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentSessionBooking;
import com.dat.ai_receptionist_web.enums.Training.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentSessionBookingRepository extends JpaRepository<StudentSessionBooking, UUID> {
    long countByClassSession_ClassSessionIdAndStatusNot(UUID classSessionId, BookingStatus status);
    Optional<StudentSessionBooking> findByStudentSessionBookingIdAndEnrollment_StudentEnrollmentId(UUID bookingId, UUID enrollmentId);
    boolean existsByEnrollment_StudentEnrollmentIdAndClassSession_ClassSessionId(UUID enrollmentId, UUID classSessionId);
    List<StudentSessionBooking> findByClassSession_ClassSessionIdAndStatus(UUID classSessionId, BookingStatus status);

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

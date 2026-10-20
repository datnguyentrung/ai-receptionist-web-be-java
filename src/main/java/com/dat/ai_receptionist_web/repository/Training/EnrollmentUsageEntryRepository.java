package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.EnrollmentUsageEntry;
import com.dat.ai_receptionist_web.enums.Training.UsageEntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface EnrollmentUsageEntryRepository extends JpaRepository<EnrollmentUsageEntry, UUID> {
    boolean existsByBooking_StudentSessionBookingIdAndEntryType(UUID bookingId, UsageEntryType entryType);

    @Query("""
        select coalesce(sum(case when e.entryType = com.dat.ai_receptionist_web.enums.Training.UsageEntryType.REVERSAL
                                 then -e.units else e.units end), 0)
        from EnrollmentUsageEntry e
        where e.enrollment.studentEnrollmentId = :enrollmentId
          and e.entryType in (
              com.dat.ai_receptionist_web.enums.Training.UsageEntryType.NORMAL_DEBIT,
              com.dat.ai_receptionist_web.enums.Training.UsageEntryType.REVERSAL
          )
    """)
    long normalDebits(@Param("enrollmentId") UUID enrollmentId);
}

package com.dat.ai_receptionist_web.repository.Training;

import com.dat.ai_receptionist_web.domain.Training.EnrollmentScheduleChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface EnrollmentScheduleChangeRequestRepository extends JpaRepository<EnrollmentScheduleChangeRequest, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from EnrollmentScheduleChangeRequest r join fetch r.currentSchedule s join fetch s.studentEnrollment join fetch r.targetCourseSchedule where r.enrollmentScheduleChangeRequestId = :id")
    Optional<EnrollmentScheduleChangeRequest> findDetailedByIdForUpdate(@Param("id") UUID id);
}

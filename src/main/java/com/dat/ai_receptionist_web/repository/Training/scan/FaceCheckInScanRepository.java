package com.dat.ai_receptionist_web.repository.Training.scan;

import com.dat.ai_receptionist_web.domain.Training.scan.FaceCheckInScan;
import com.dat.ai_receptionist_web.domain.Training.scan.FaceCheckInStage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FaceCheckInScanRepository extends JpaRepository<FaceCheckInScan, UUID> {
    List<FaceCheckInScan> findByStageNotIn(Collection<FaceCheckInStage> stages);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select scan from FaceCheckInScan scan where scan.requestId = :requestId")
    Optional<FaceCheckInScan> findByIdForUpdate(@Param("requestId") UUID requestId);
}

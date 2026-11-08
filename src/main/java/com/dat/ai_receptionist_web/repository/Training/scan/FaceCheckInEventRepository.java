package com.dat.ai_receptionist_web.repository.Training.scan;

import com.dat.ai_receptionist_web.domain.Training.scan.FaceCheckInEvent;
import com.dat.ai_receptionist_web.domain.Training.scan.FaceCheckInStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FaceCheckInEventRepository extends JpaRepository<FaceCheckInEvent, Long> {
    List<FaceCheckInEvent> findByRequestIdAndEventIdGreaterThanOrderByEventId(UUID requestId, long eventId);
    boolean existsByRequestIdAndStage(UUID requestId, FaceCheckInStage stage);
}

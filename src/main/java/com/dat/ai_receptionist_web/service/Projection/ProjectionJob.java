package com.dat.ai_receptionist_web.service.Projection;

import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import com.dat.ai_receptionist_web.enums.Infrastructure.ProjectionType;

import java.util.UUID;

public record ProjectionJob(
        long id,
        UUID tenantId,
        ProjectionType projectionType,
        String projectionKey,
        String aggregateKey,
        Integer year,
        Integer quarter,
        ScheduleLevel scheduleLevel,
        String payload,
        long revision,
        int retryCount
) {
}

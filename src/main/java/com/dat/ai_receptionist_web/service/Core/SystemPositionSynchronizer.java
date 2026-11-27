package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.repository.Core.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Ensures system-owned business positions are restored after data resets. */
@Slf4j
@Component
@Order(4)
@RequiredArgsConstructor
public class SystemPositionSynchronizer implements ApplicationRunner {
    static final UUID STUDENT_POSITION_ID = UUID.fromString("00000000-0000-0000-0000-000000090000");
    static final String STUDENT_CODE = "STUDENT";
    private static final String STUDENT_NAME = "Học viên";
    private static final String STUDENT_DESCRIPTION = "Student business position";

    private final PositionRepository positionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean created = com.dat.ai_receptionist_web.context.TenantContext.runWithTenant(
                com.dat.ai_receptionist_web.context.TenantContext.DEFAULT_TENANT_ID,
                this::syncStudentPosition);
        log.info("System position sync completed: studentCreated={}", created);
    }

    boolean syncStudentPosition() {
        boolean created = positionRepository.findByCodeIgnoreCase(STUDENT_CODE).isEmpty();
        positionRepository.upsertSystemPosition(
                STUDENT_POSITION_ID,
                com.dat.ai_receptionist_web.context.TenantContext.requireTenantId(),
                STUDENT_CODE,
                STUDENT_NAME,
                STUDENT_DESCRIPTION);
        return created;
    }
}

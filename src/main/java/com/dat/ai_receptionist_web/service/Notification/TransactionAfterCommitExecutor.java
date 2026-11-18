package com.dat.ai_receptionist_web.service.Notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Seam dùng chung để chạy tác vụ sau khi transaction commit thành công.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionAfterCommitExecutor {
    private final PlatformTransactionManager transactionManager;

    public void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            runInNewTransaction(action);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runInNewTransaction(action);
            }
        });
    }

    private void runInNewTransaction(Runnable action) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        log.info("[NOTIFICATION_TRACE] Running after-commit action in a new transaction");
        transaction.executeWithoutResult(status -> action.run());
    }
}

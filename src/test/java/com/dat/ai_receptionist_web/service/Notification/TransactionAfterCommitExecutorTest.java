package com.dat.ai_receptionist_web.service.Notification;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionAfterCommitExecutorTest {
    @Test
    void runsAfterCommitWorkInANewTransaction() {
        CountingTransactionManager transactionManager = new CountingTransactionManager();
        TransactionAfterCommitExecutor executor = new TransactionAfterCommitExecutor(transactionManager);
        AtomicInteger callbackCount = new AtomicInteger();

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                executor.afterCommit(callbackCount::incrementAndGet));

        assertThat(callbackCount).hasValue(1);
        assertThat(transactionManager.beginCount).isEqualTo(2);
        assertThat(transactionManager.propagations).containsExactly(
                TransactionDefinition.PROPAGATION_REQUIRED,
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
    }

    private static final class CountingTransactionManager extends AbstractPlatformTransactionManager {
        private int beginCount;
        private final List<Integer> propagations = new java.util.ArrayList<>();

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            beginCount++;
            propagations.add(definition.getPropagationBehavior());
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}

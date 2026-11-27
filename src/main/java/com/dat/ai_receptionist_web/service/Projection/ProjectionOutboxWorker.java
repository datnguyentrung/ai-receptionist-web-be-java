package com.dat.ai_receptionist_web.service.Projection;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectionOutboxWorker {
    private final ProjectionOutboxService outboxService;
    private final ProjectionDispatcher dispatcher;
    private final ProjectionRetryPolicy retryPolicy;
    private final MeterRegistry meterRegistry;

    @Value("${projection.worker.batch-size:20}")
    private int batchSize;

    private final String instanceId = ManagementFactory.getRuntimeMXBean().getName();

    /**
     * Tác dụng: Thực hiện logic poll của lớp hiện tại.
     * Input: Không có tham số đầu vào.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Scheduled(fixedDelayString = "${projection.worker.poll-delay-ms:1000}")
    public void poll() {
        List<ProjectionJob> jobs = outboxService.claimReadyJobs(batchSize, instanceId);
        for (ProjectionJob job : jobs) {
            com.dat.ai_receptionist_web.context.TenantContext.runWithTenant(
                    job.tenantId(), () -> processOne(job));
        }
    }

    /**
     * Tác dụng: Xử lý một đơn vị công việc theo logic nghiệp vụ của lớp.
     * Input: Nhận ProjectionJob job từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    private void processOne(ProjectionJob job) {
        long startedAt = System.nanoTime();
        try {
            log.info("PROJECTION_JOB_STARTED id={} type={} key={} revision={} retryCount={}",
                    job.id(), job.projectionType(), job.projectionKey(), job.revision(), job.retryCount());
            dispatcher.process(job);
            boolean acked = outboxService.ack(job.id(), job.revision(), instanceId);
            boolean released = !acked && outboxService.releaseSuperseded(job.id(), job.revision(), instanceId);
            meterRegistry.counter("projection.worker.process.total",
                    "type", job.projectionType().name(), "result", acked ? "success" : "superseded").increment();
            log.info("PROJECTION_JOB_COMPLETED id={} key={} revision={} acked={} supersededReleased={} elapsedMs={}",
                    job.id(), job.projectionKey(), job.revision(), acked, released, elapsedMillis(startedAt));
        } catch (IllegalArgumentException exception) {
            boolean dead = outboxService.dead(job, exception, instanceId);
            boolean released = !dead && outboxService.releaseSuperseded(job.id(), job.revision(), instanceId);
            meterRegistry.counter("projection.worker.process.total",
                    "type", job.projectionType().name(), "result", dead ? "dead" : "superseded").increment();
            log.error("PROJECTION_JOB_DEAD id={} key={} revision={} markedDead={} supersededReleased={}",
                    job.id(), job.projectionKey(), job.revision(), dead, released, exception);
        } catch (RuntimeException exception) {
            Duration delay = retryPolicy.nextDelay(job.retryCount());
            boolean scheduled = outboxService.retry(job, delay, exception, instanceId);
            boolean released = !scheduled && outboxService.releaseSuperseded(job.id(), job.revision(), instanceId);
            meterRegistry.counter("projection.worker.process.total",
                    "type", job.projectionType().name(), "result", scheduled ? "retry" : "superseded").increment();
            meterRegistry.counter("projection.worker.redis.failure",
                    "type", job.projectionType().name()).increment();
            log.warn("PROJECTION_JOB_RETRY id={} key={} revision={} retryCount={} delayMs={} scheduled={} supersededReleased={}",
                    job.id(), job.projectionKey(), job.revision(), job.retryCount() + 1,
                    delay.toMillis(), scheduled, released, exception);
        }
    }

    /**
     * Tác dụng: Thực hiện logic elapsedMillis của lớp hiện tại.
     * Input: Nhận long startedAt từ caller hoặc request.
     * Output: Trả về giá trị long biểu thị kết quả tính toán hoặc số lượng.
     */
    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}



package com.dat.ai_receptionist_web.service.Projection;

import com.dat.ai_receptionist_web.enums.Infrastructure.ProjectionType;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import com.dat.ai_receptionist_web.service.Report.LeaderboardScope;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectionOutboxService {
    private static final String UPSERT_SQL = """
            INSERT INTO infrastructure.projection_outbox (
                tenant_id, projection_type, projection_key, aggregate_type, aggregate_key,
                year_value, quarter_value, schedule_level, payload,
                revision, processed_revision, status, retry_count,
                next_attempt_at, created_at, dirty_since, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, 1, 0, 'PENDING', 0, NOW(), NOW(), NOW(), NOW())
            ON CONFLICT (tenant_id, projection_key) DO UPDATE SET
                revision = infrastructure.projection_outbox.revision + 1,
                projection_type = EXCLUDED.projection_type,
                aggregate_type = EXCLUDED.aggregate_type,
                aggregate_key = EXCLUDED.aggregate_key,
                year_value = EXCLUDED.year_value,
                quarter_value = EXCLUDED.quarter_value,
                schedule_level = EXCLUDED.schedule_level,
                payload = CASE
                    WHEN infrastructure.projection_outbox.revision = infrastructure.projection_outbox.processed_revision
                        THEN EXCLUDED.payload
                    WHEN EXCLUDED.projection_type = 'LEADERBOARD_MEMBER'
                        THEN jsonb_build_object(
                            'membershipChanged',
                            COALESCE((infrastructure.projection_outbox.payload ->> 'membershipChanged')::boolean, false)
                            OR COALESCE((EXCLUDED.payload ->> 'membershipChanged')::boolean, false)
                        )
                    ELSE EXCLUDED.payload
                END,
                status = CASE
                    WHEN infrastructure.projection_outbox.status = 'PROCESSING' THEN 'PROCESSING'
                    ELSE 'PENDING'
                END,
                retry_count = 0,
                next_attempt_at = NOW(),
                locked_by = CASE
                    WHEN infrastructure.projection_outbox.status = 'PROCESSING' THEN infrastructure.projection_outbox.locked_by
                    ELSE NULL
                END,
                locked_at = CASE
                    WHEN infrastructure.projection_outbox.status = 'PROCESSING' THEN infrastructure.projection_outbox.locked_at
                    ELSE NULL
                END,
                dirty_since = CASE
                    WHEN infrastructure.projection_outbox.revision = infrastructure.projection_outbox.processed_revision THEN NOW()
                    ELSE infrastructure.projection_outbox.dirty_since
                END,
                last_error = NULL,
                updated_at = NOW()
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * Tác dụng: Đánh dấu trạng thái xử lý để các tiến trình liên quan nhận biết thay đổi.
     * Input: Nhận String studentCode, int year, int quarter từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void markConductDirty(String studentCode, int year, int quarter) {
        markDirty(ProjectionType.LEADERBOARD_CONDUCT,
                "leaderboard:conduct:%d:Q%d:student:%s".formatted(year, quarter, studentCode),
                "STUDENT", studentCode, year, quarter, null, "{}");
    }

    /**
     * Tác dụng: Đánh dấu trạng thái xử lý để các tiến trình liên quan nhận biết thay đổi.
     * Input: Nhận String studentCode, int year, int quarter, ScheduleLevel scheduleLevel từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void markFitnessDirty(String studentCode, int year, int quarter, ScheduleLevel scheduleLevel) {
        markDirty(ProjectionType.LEADERBOARD_FITNESS,
                "leaderboard:fitness:%d:Q%d:%s:student:%s".formatted(year, quarter, scheduleLevel, studentCode),
                "STUDENT", studentCode, year, quarter, scheduleLevel, "{}");
    }

    /**
     * Tác dụng: Đánh dấu trạng thái xử lý để các tiến trình liên quan nhận biết thay đổi.
     * Input: Nhận String studentCode, boolean membershipChanged từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void markMemberDirty(String studentCode, boolean membershipChanged) {
        markDirty(ProjectionType.LEADERBOARD_MEMBER,
                "leaderboard:member:student:" + studentCode,
                "STUDENT", studentCode, null, null, null,
                "{\"membershipChanged\":" + membershipChanged + "}");
    }

    /**
     * Tác dụng: Đánh dấu trạng thái xử lý để các tiến trình liên quan nhận biết thay đổi.
     * Input: Không có tham số đầu vào.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void markFitnessRecordsCacheDirty() {
        markDirty(ProjectionType.FITNESS_RECORDS_CACHE,
                "cache:fitness-records", "CACHE", "fitnessRecords",
                null, null, null, "{}");
    }

    /**
     * Tác dụng: Đánh dấu trạng thái xử lý để các tiến trình liên quan nhận biết thay đổi.
     * Input: Nhận ProjectionType type, String projectionKey, String aggregateType, String aggregateKey, Integer year, Integer quarter, ScheduleLevel scheduleLevel, String payload từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    private void markDirty(ProjectionType type, String projectionKey, String aggregateType, String aggregateKey,
                           Integer year, Integer quarter, ScheduleLevel scheduleLevel, String payload) {
        java.util.UUID tenantId = com.dat.ai_receptionist_web.context.TenantContext.requireTenantId();
        jdbcTemplate.update(UPSERT_SQL,
                tenantId, type.name(), projectionKey, aggregateType, aggregateKey,
                year, quarter, scheduleLevel == null ? null : scheduleLevel.name(), payload);
    }

    /**
     * Tác dụng: Thực hiện logic claimReadyJobs của lớp hiện tại.
     * Input: Nhận int batchSize, String instanceId từ caller hoặc request.
     * Output: Trả về List<ProjectionJob> theo kết quả xử lý.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ProjectionJob> claimReadyJobs(int batchSize, String instanceId) {
        List<ProjectionJob> jobs = jdbcTemplate.query("""
                SELECT id, tenant_id, projection_type, projection_key, aggregate_key,
                       year_value, quarter_value, schedule_level, payload::text,
                       revision, retry_count
                FROM infrastructure.projection_outbox
                WHERE (
                    (status IN ('PENDING', 'RETRY') AND next_attempt_at <= NOW())
                    OR (status = 'PROCESSING' AND locked_at < NOW() - INTERVAL '60 seconds')
                )
                AND NOT EXISTS (
                    SELECT 1
                    FROM infrastructure.projection_scope_state scope_state
                    WHERE scope_state.tenant_id = infrastructure.projection_outbox.tenant_id
                      AND scope_state.rebuilding = TRUE
                      AND scope_state.rebuild_started_at >= NOW() - INTERVAL '30 minutes'
                      AND (
                          (projection_type = 'LEADERBOARD_CONDUCT'
                              AND scope_state.scope_key = 'quarter|' || year_value || '|' || quarter_value)
                          OR (projection_type = 'LEADERBOARD_FITNESS'
                              AND scope_state.scope_key = 'fitness|' || year_value || '|' || quarter_value || '|' || schedule_level)
                          OR projection_type = 'LEADERBOARD_MEMBER'
                      )
                )
                ORDER BY next_attempt_at, id
                FOR UPDATE SKIP LOCKED
                LIMIT ?
                """, (rs, rowNum) -> new ProjectionJob(
                rs.getLong("id"),
                rs.getObject("tenant_id", java.util.UUID.class),
                ProjectionType.valueOf(rs.getString("projection_type")),
                rs.getString("projection_key"),
                rs.getString("aggregate_key"),
                (Integer) rs.getObject("year_value"),
                (Integer) rs.getObject("quarter_value"),
                rs.getString("schedule_level") == null ? null : ScheduleLevel.valueOf(rs.getString("schedule_level")),
                rs.getString("payload"),
                rs.getLong("revision"),
                rs.getInt("retry_count")
        ), batchSize);

        List<ProjectionJob> claimed = new ArrayList<>(jobs.size());
        for (ProjectionJob job : jobs) {
            int updated = jdbcTemplate.update("""
                    UPDATE infrastructure.projection_outbox
                    SET status = 'PROCESSING', locked_by = ?, locked_at = NOW(), updated_at = NOW()
                    WHERE id = ? AND revision = ?
                    """, instanceId, job.id(), job.revision());
            if (updated == 1) {
                claimed.add(job);
            }
        }
        return claimed;
    }

    /**
     * Tác dụng: Xác nhận một công việc đã được xử lý thành công.
     * Input: Nhận long id, long claimedRevision, String instanceId từ caller hoặc request.
     * Output: Trả về true/false thể hiện kết quả kiểm tra hoặc xử lý.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean ack(long id, long claimedRevision, String instanceId) {
        return jdbcTemplate.update("""
                UPDATE infrastructure.projection_outbox
                SET processed_revision = ?, status = 'DONE', retry_count = 0,
                    locked_by = NULL, locked_at = NULL, last_error = NULL,
                    processed_at = NOW(), updated_at = NOW()
                WHERE id = ? AND revision = ? AND locked_by = ?
                """, claimedRevision, id, claimedRevision, instanceId) == 1;
    }

    /**
     * Tác dụng: Giải phóng công việc không còn cần xử lý để hệ thống tiếp tục an toàn.
     * Input: Nhận long id, long claimedRevision, String instanceId từ caller hoặc request.
     * Output: Trả về true/false thể hiện kết quả kiểm tra hoặc xử lý.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean releaseSuperseded(long id, long claimedRevision, String instanceId) {
        return jdbcTemplate.update("""
                UPDATE infrastructure.projection_outbox
                SET status = 'PENDING', locked_by = NULL, locked_at = NULL,
                    next_attempt_at = NOW(), updated_at = NOW()
                WHERE id = ? AND revision > ? AND status = 'PROCESSING' AND locked_by = ?
                """, id, claimedRevision, instanceId) == 1;
    }

    /**
     * Tác dụng: Đánh dấu công việc thất bại vĩnh viễn sau khi vượt quá ngưỡng xử lý.
     * Input: Nhận ProjectionJob job, Throwable error, String instanceId từ caller hoặc request.
     * Output: Trả về true/false thể hiện kết quả kiểm tra hoặc xử lý.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean dead(ProjectionJob job, Throwable error, String instanceId) {
        String message = errorMessage(error);
        return jdbcTemplate.update("""
                UPDATE infrastructure.projection_outbox
                SET status = 'DEAD', locked_by = NULL, locked_at = NULL,
                    last_error = ?, last_error_at = NOW(), updated_at = NOW()
                WHERE id = ? AND revision = ? AND locked_by = ?
                """, message, job.id(), job.revision(), instanceId) == 1;
    }

    /**
     * Tác dụng: Lên lịch thử lại công việc thất bại theo chính sách hiện tại.
     * Input: Nhận ProjectionJob job, Duration delay, Throwable error, String instanceId từ caller hoặc request.
     * Output: Trả về true/false thể hiện kết quả kiểm tra hoặc xử lý.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean retry(ProjectionJob job, Duration delay, Throwable error, String instanceId) {
        LocalDateTime nextAttempt = LocalDateTime.now().plus(delay);
        String message = errorMessage(error);
        return jdbcTemplate.update("""
                UPDATE infrastructure.projection_outbox
                SET status = 'RETRY', retry_count = retry_count + 1,
                    next_attempt_at = ?, locked_by = NULL, locked_at = NULL,
                    last_error = ?, last_error_at = NOW(), updated_at = NOW()
                WHERE id = ? AND revision = ? AND locked_by = ?
                """, nextAttempt, message, job.id(), job.revision(), instanceId) == 1;
    }

    /**
     * Tác dụng: Đếm số lượng bản ghi hoặc công việc theo điều kiện đầu vào.
     * Input: Nhận LeaderboardScope scope từ caller hoặc request.
     * Output: Trả về giá trị long biểu thị kết quả tính toán hoặc số lượng.
     */
    public long countProcessingForScope(LeaderboardScope scope) {
        Long count;
        if (scope.type() == LeaderboardScope.Type.QUARTER) {
            count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM infrastructure.projection_outbox
                    WHERE status = 'PROCESSING'
                      AND (projection_type = 'LEADERBOARD_MEMBER'
                           OR (projection_type = 'LEADERBOARD_CONDUCT'
                               AND year_value = ? AND quarter_value = ?))
                    """, Long.class, scope.year(), scope.quarter());
        } else {
            count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM infrastructure.projection_outbox
                    WHERE status = 'PROCESSING'
                      AND (projection_type = 'LEADERBOARD_MEMBER'
                           OR (projection_type = 'LEADERBOARD_FITNESS'
                               AND year_value = ? AND quarter_value = ? AND schedule_level = ?))
                    """, Long.class, scope.year(), scope.quarter(), scope.scheduleLevel().name());
        }
        return count == null ? 0L : count;
    }

    /**
     * Tác dụng: Đếm số lượng bản ghi hoặc công việc theo điều kiện đầu vào.
     * Input: Không có tham số đầu vào.
     * Output: Trả về giá trị long biểu thị kết quả tính toán hoặc số lượng.
     */
    public long countOutstanding() {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM infrastructure.projection_outbox
                WHERE revision > processed_revision AND status <> 'DEAD'
                """, Long.class);
        return count == null ? 0L : count;
    }

    /**
     * Tác dụng: Thực hiện logic oldestOutstandingAgeSeconds của lớp hiện tại.
     * Input: Không có tham số đầu vào.
     * Output: Trả về giá trị double biểu thị kết quả tính toán hoặc số lượng.
     */
    public double oldestOutstandingAgeSeconds() {
        Double seconds = jdbcTemplate.queryForObject("""
                SELECT COALESCE(EXTRACT(EPOCH FROM (NOW() - MIN(dirty_since))), 0)::double precision
                FROM infrastructure.projection_outbox
                WHERE revision > processed_revision AND status <> 'DEAD'
                """, Double.class);
        return seconds == null ? 0.0 : seconds;
    }

    /**
     * Tác dụng: Đếm số lượng bản ghi hoặc công việc theo điều kiện đầu vào.
     * Input: Không có tham số đầu vào.
     * Output: Trả về giá trị long biểu thị kết quả tính toán hoặc số lượng.
     */
    public long countDead() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM infrastructure.projection_outbox WHERE status = 'DEAD'", Long.class);
        return count == null ? 0L : count;
    }

    /**
     * Tác dụng: Thực hiện logic errorMessage của lớp hiện tại.
     * Input: Nhận Throwable error từ caller hoặc request.
     * Output: Trả về String theo kết quả xử lý.
     */
    private String errorMessage(Throwable error) {
        String message = error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage());
        return message.length() > 4000 ? message.substring(0, 4000) : message;
    }
}



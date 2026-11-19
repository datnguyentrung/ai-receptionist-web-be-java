package com.dat.ai_receptionist_web.dto.Analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * DTO cho một page Analytics: BranchAnalytics.
 * Main page và toàn bộ drill-down liên quan được gom trong cùng file.
 */
public final class BranchAnalyticsDTO {

    private BranchAnalyticsDTO() {
    }

    public record Request(
            LocalDate from,
            LocalDate to,
            LocalDate asOf,
            List<Long> branchIds,
            List<String> levels,
            List<UUID> courseIds
    ) {
    }

    public record Response(
            Summary summary,
            List<BranchRow> branches,
            List<MonthlyRow> monthly,
            List<TransferRow> transferMatrix
    ) {
    }

    public record Summary(
            Integer operatingBranches,
            Integer totalActiveStudents,
            Integer totalInferredLeavers,
            BigDecimal systemAttendanceRate
    ) {
    }

    public record BranchRow(
            Long branchId,
            String branchName,
            String branchStatus,
            LocalDate openedDate,
            Integer activeStudents,
            Integer newStudents,
            Integer reactivatedStudents,
            Integer inferredLeavers,
            Integer transferIn,
            Integer transferOut,
            Integer netMovement,
            BigDecimal attendanceRate,
            Integer activeClasses,
            Integer availableSeats
    ) {
    }

    public record MonthlyRow(
            YearMonth month,
            Long branchId,
            Integer newStudents,
            Integer inferredLeavers,
            Integer activeStudents
    ) {
    }

    public record TransferRow(
            Long fromBranchId,
            Long toBranchId,
            Integer studentCount
    ) {
    }

    public record ClassesRequest(
            LocalDate from,
            LocalDate to,
            LocalDate asOf,
            String status,
            String sortBy,
            Integer page,
            Integer size
    ) {
    }

    public record ClassesResponse(
            UUID courseScheduleId,
            UUID courseId,
            String courseName,
            UUID classScheduleId,
            String weekday,
            LocalTime startTime,
            LocalTime endTime,
            String scheduleStatus,
            Integer activeRoster,
            Integer newStudents,
            Integer inferredLeavers,
            Integer transferIn,
            Integer transferOut,
            BigDecimal attendanceRate,
            Integer capacity,
            BigDecimal fillRate,
            List<String> warningCodes
    ) {
    }
}

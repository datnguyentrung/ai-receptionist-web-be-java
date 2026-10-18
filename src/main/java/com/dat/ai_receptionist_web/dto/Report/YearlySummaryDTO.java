package com.dat.ai_receptionist_web.dto.Report;

import com.dat.ai_receptionist_web.dto.Training.SessionAttendanceDTO;
import com.dat.ai_receptionist_web.enums.Report.ExamEligibility;
import java.util.List;

public final class YearlySummaryDTO {
    private YearlySummaryDTO() {}

    public record Response(int year, List<QuarterSummary> quarters) {}

    public record QuarterSummary(
            int quarterNumber,
            SessionAttendanceDTO.AttendanceStats attendanceStats,
            double attendanceScore,
            double performanceScore,
            double totalQuarterScore,
            ExamEligibility eligibility
    ) {}
}

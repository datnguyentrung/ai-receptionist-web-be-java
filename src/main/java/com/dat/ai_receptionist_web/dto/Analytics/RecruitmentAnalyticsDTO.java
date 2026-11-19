package com.dat.ai_receptionist_web.dto.Analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * DTO cho một page Analytics: RecruitmentAnalytics.
 * Main page và toàn bộ drill-down liên quan được gom trong cùng file.
 */
public final class RecruitmentAnalyticsDTO {

    private RecruitmentAnalyticsDTO() {
    }

    public record Request(
            LocalDate from,
            LocalDate to,
            List<Long> branchIds,
            List<UUID> courseIds,
            List<String> levels,
            List<String> genders,
            Integer minAge,
            Integer maxAge,
            Boolean comparePreviousYear
    ) {
    }

    public record Response(
            Summary summary,
            List<MonthlyRow> monthly,
            List<AgeRow> byAge,
            List<BranchRow> byBranch,
            List<ClassRow> byClass,
            List<YearComparisonRow> yearComparison,
            DataAvailability dataAvailability
    ) {
    }

    public record Summary(
            Integer firstTimeStudents,
            Integer reactivatedStudents,
            BigDecimal averageMonthlyIntake,
            YearMonth peakMonth,
            Integer availableSeats
    ) {
    }

    public record MonthlyRow(
            YearMonth month,
            Integer firstTimeStudents,
            Integer reactivatedStudents,
            Integer inferredLeavers,
            Integer netMovement
    ) {
    }

    public record AgeRow(
            YearMonth month,
            Integer age,
            Integer firstTimeStudents
    ) {
    }

    public record BranchRow(
            YearMonth month,
            Long branchId,
            String branchName,
            Integer firstTimeStudents
    ) {
    }

    public record ClassRow(
            UUID courseScheduleId,
            String courseName,
            String branchName,
            Integer firstTimeStudents,
            Integer activeRoster,
            Integer availableSeats
    ) {
    }

    public record YearComparisonRow(
            Integer calendarMonth,
            Integer currentYearCount,
            Integer previousYearCount,
            BigDecimal growthRate
    ) {
    }

    public record DataAvailability(
            Boolean hasCampaignData,
            Boolean hasLeadData,
            Boolean hasTrialData,
            Boolean hasAdSpendData
    ) {
    }

    public record StudentsRequest(
            LocalDate from,
            LocalDate to,
            @DateTimeFormat(pattern = "yyyy-MM")
            YearMonth month,
            Integer age,
            String gender,
            Long branchId,
            UUID courseId,
            UUID courseScheduleId,
            String acquisitionType,
            Integer page,
            Integer size
    ) {
    }

    public record StudentsResponse(
            UUID personId,
            String personCode,
            String fullName,
            LocalDate birthDate,
            Integer ageAtFirstEnrollment,
            String gender,
            LocalDate firstEnrollmentStartDate,
            UUID firstCourseId,
            String firstCourseName,
            UUID firstCourseScheduleId,
            Long firstBranchId,
            UUID firstTrainingPlanId,
            String acquisitionType,
            String sourceQuality
    ) {
    }
}

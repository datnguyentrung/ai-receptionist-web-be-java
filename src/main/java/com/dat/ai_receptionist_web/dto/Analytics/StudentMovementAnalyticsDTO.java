package com.dat.ai_receptionist_web.dto.Analytics;

import com.dat.ai_receptionist_web.enums.Core.Gender;
import com.dat.ai_receptionist_web.enums.Training.AgeGroup;
import com.dat.ai_receptionist_web.enums.Training.MovementType;

import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;

public final class StudentMovementAnalyticsDTO {

    private StudentMovementAnalyticsDTO() {
    }

    public record Request(
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth fromMonth,
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth toMonth,
            Integer minAge,
            Integer maxAge,
            List<String> genders
    ) {
    }

    public record Response(
            Integer currentActiveStudents,          // Số học viên đang hoạt động
            Integer totalStudents,                  // Tổng số học viên
            List<YearlyMovement> yearlyMovements    // Biến động theo năm
    ) {
    }

    public record YearlyMovement(
            Year year,                              // Năm thống kê
            Map<MovementType, Integer> counts,      // Số lượng từng loại biến động
            List<QuarterlyMovement> quarterlyMovements
    ) {
    }

    public record QuarterlyMovement(
            Integer quarter,                        // Quý thống kê
            Map<MovementType, Integer> counts,      // Số lượng từng loại biến động
            List<MonthlyMovement> monthlyMovements
    ) {
    }

    public record MonthlyMovement(
            YearMonth month,// Tháng thống kê
            Map<MovementType, Integer> counts,// Tổng biến động của tháng
            Map<AgeGroup, Movement> byAge,// Phân bổ theo tuổi
            Map<Gender, Movement> byGender// Phân bổ theo giới tính
    ) {
    }

    public record Movement(
            Map<MovementType, Integer> counts       // Số lượng theo loại biến động
    ) {
    }
}

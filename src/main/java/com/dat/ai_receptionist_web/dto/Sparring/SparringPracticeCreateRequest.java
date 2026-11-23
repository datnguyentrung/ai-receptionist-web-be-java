package com.dat.ai_receptionist_web.dto.Sparring;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Cấu hình cố định được host chọn trước khi mời các giám định vào phòng.
 */
public record SparringPracticeCreateRequest(
        @NotNull
        @Min(1)
        @Max(6)
        Integer maximumJudges,
        // Số trọng tài tối đa
        @NotNull
        @Min(1)
        @Max(6)
        Integer consensusJudges
        // Số đồng thuận
) {
}

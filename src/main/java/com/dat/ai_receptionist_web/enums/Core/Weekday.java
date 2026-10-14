package com.dat.ai_receptionist_web.enums.Core;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum Weekday {
    SUNDAY(1, "Chủ Nhật"),    // Chủ nhật
    MONDAY(2, "Thứ Hai"),     // Thứ hai
    TUESDAY(3, "Thứ Ba"),     // Thứ ba
    WEDNESDAY(4, "Thứ Tư"),   // Thứ tư
    THURSDAY(5, "Thứ Năm"),   // Thứ năm
    FRIDAY(6, "Thứ Sáu"),     // Thứ sáu
    SATURDAY(7, "Thứ Bảy");   // Thứ bảy

    private final int code;
    private final String label;

    private static final Map<Integer, Weekday> LOOKUP_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(Weekday::getCode, Function.identity()));

    /**
     * Dùng nội bộ trong code Java hoặc khi lấy data từ DB lên
     */
    public static Weekday fromCode(int code) {
        return Optional.ofNullable(LOOKUP_MAP.get(code))
                .orElseThrow(() -> new IllegalArgumentException("Invalid Weekday code: " + code));
    }

    /**
     * Jackson sẽ dùng DUY NHẤT hàm này để hứng data từ API gửi xuống
     */
    @JsonCreator
    public static Weekday fromValue(String value) {
        // Nếu Frontend gửi lên số (VD: "7" hoặc 7)
        if (value.matches("-?\\d+")) {
            int code = Integer.parseInt(value);
            return Optional.ofNullable(LOOKUP_MAP.get(code))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Weekday code: " + code));
        }

        // Nếu Frontend gửi lên chữ (VD: "SATURDAY")
        try {
            return Weekday.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Weekday name: " + value);
        }
    }

    public static Weekday fromJavaDayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> MONDAY;
            case TUESDAY -> TUESDAY;
            case WEDNESDAY -> WEDNESDAY;
            case THURSDAY -> THURSDAY;
            case FRIDAY -> FRIDAY;
            case SATURDAY -> SATURDAY;
            case SUNDAY -> SUNDAY;
        };
    }

    public int getCode() {
        return code;
    }
}

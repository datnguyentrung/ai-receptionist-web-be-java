package com.dat.ai_receptionist_web.schema;

import com.dat.ai_receptionist_web.enums.Core.Weekday;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeekdayJsonContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesWeekdayUsingEnumName() throws Exception {
        assertThat(objectMapper.writeValueAsString(Weekday.MONDAY))
                .isEqualTo("\"MONDAY\"");
    }

    @Test
    void deserializesWeekdayFromEnumNameOrLegacyCode() throws Exception {
        assertThat(objectMapper.readValue("\"MONDAY\"", Weekday.class))
                .isEqualTo(Weekday.MONDAY);
        assertThat(objectMapper.readValue("2", Weekday.class))
                .isEqualTo(Weekday.MONDAY);
        assertThat(objectMapper.readValue("\"2\"", Weekday.class))
                .isEqualTo(Weekday.MONDAY);
    }
}

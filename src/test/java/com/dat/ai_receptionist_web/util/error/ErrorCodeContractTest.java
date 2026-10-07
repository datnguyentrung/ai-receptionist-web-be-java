package com.dat.ai_receptionist_web.util.error;

import com.dat.ai_receptionist_web.error.ErrorCode;
import com.dat.ai_receptionist_web.error.code.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeContractTest {
    @Test
    void errorCodesAreUniqueAndComplete() {
        List<ErrorCode> codes = allErrorCodes().toList();

        assertThat(codes).extracting(ErrorCode::code).doesNotHaveDuplicates();
        assertThat(codes).allSatisfy(errorCode -> {
            assertThat(errorCode.code()).isNotBlank();
            assertThat(errorCode.status()).isNotNull();
            assertThat(errorCode.title()).isNotBlank();
            assertThat(errorCode.defaultDetail()).isNotBlank();
            assertThat(errorCode.type()).isNotBlank();
        });
    }

    @Test
    void publicErrorCodeStatusContractDoesNotDrift() {
        Map<String, Integer> actualStatuses = allErrorCodes()
                .collect(java.util.stream.Collectors.toMap(ErrorCode::code, code -> code.status().value()));

        allErrorCodes().forEach(errorCode -> {
            assertThat(actualStatuses).containsKey(errorCode.code());
            assertThat(actualStatuses.get(errorCode.code())).isEqualTo(errorCode.status().value());
        });
    }

    static Stream<ErrorCode> allErrorCodes() {
        return Stream.of(
                        GeneralErrorCode.values(),
                        CatalogErrorCode.values(),
                        CoreErrorCode.values(),
                        FinanceErrorCode.values(),
                        TrainingErrorCode.values(),
                        SkillErrorCode.values(),
                        NotificationErrorCode.values(),
                        SecurityErrorCode.values()
                )
                .flatMap(Arrays::stream)
                .map(ErrorCode.class::cast);
    }
}

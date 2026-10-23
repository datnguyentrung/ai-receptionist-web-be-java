package com.dat.ai_receptionist_web.schema;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CoursePriceRetirementContractTest {
    @Test
    void runtimeSourcesExposeOnlyTheTrainingPlanPurchaseInterface() throws Exception {
        String sources;
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            sources = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(this::read)
                    .reduce("", String::concat);
        }

        assertThat(sources)
                .doesNotContain("CoursePrice", "coursePrice", "/api/v1/course-prices")
                .contains("TrainingPlanPurchaseRequest", "TRAINING_PLAN_PURCHASE");
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read " + path, exception);
        }
    }
}

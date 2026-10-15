package com.dat.ai_receptionist_web.service.Sparring;

import com.dat.ai_receptionist_web.error.ApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SparringRoomRegistryTest {
    @Test
    void createsFourDigitRoomsAndAcceptsJudgeScores() {
        SparringRoomRegistry registry = new SparringRoomRegistry();
        try {
            String roomCode = registry.create().roomCode();
            assertThat(roomCode).matches("\\d{4}");

            assertThat(registry.get(roomCode).snapshot().redName()).isEqualTo("RED");
            assertThat(registry.get(roomCode).snapshot().blueName()).isEqualTo("BLUE");

            for (int points = 1; points <= 5; points++) {
                registry.apply(roomCode, false, "judgeScore", "red", points, null, null, null, null);
            }
            assertThat(registry.get(roomCode).snapshot().redScore()).isEqualTo(15);
            assertThatThrownBy(() -> registry.apply(roomCode, false, "judgeScore", "red", 0, null, null, null, null))
                    .isInstanceOf(ApiException.class);
            assertThatThrownBy(() -> registry.apply(roomCode, false, "judgeScore", "red", 6, null, null, null, null))
                    .isInstanceOf(ApiException.class);
        } finally {
            registry.shutdown();
        }
    }

    @Test
    void operatorUpdatesNamesAndUsesConfigurableScoreAndPenaltyDeltas() {
        SparringRoomRegistry registry = new SparringRoomRegistry();
        try {
            String roomCode = registry.create().roomCode();

            var named = registry.apply(roomCode, true, "setCompetitorName", "red", null, null, null, null, "  Nguyen Van A  ");
            assertThat(named.snapshot().redName()).isEqualTo("Nguyen Van A");

            var scored = registry.apply(roomCode, true, "operatorAdjustScore", "red", null, 12, null, null, null);
            assertThat(scored.snapshot().redScore()).isEqualTo(12);
            var scoreFloored = registry.apply(roomCode, true, "operatorAdjustScore", "red", null, -20, null, null, null);
            assertThat(scoreFloored.snapshot().redScore()).isZero();

            var penalized = registry.apply(roomCode, true, "operatorAdjustPenalty", "blue", null, 5, null, null, null);
            assertThat(penalized.snapshot().bluePenalties()).isEqualTo(5);
            var penaltyFloored = registry.apply(roomCode, true, "operatorAdjustPenalty", "blue", null, -20, null, null, null);
            assertThat(penaltyFloored.snapshot().bluePenalties()).isZero();
        } finally {
            registry.shutdown();
        }
    }

    @Test
    void rejectsUnauthorizedOrInvalidCompetitorNamesAndOutOfRangeDeltas() {
        SparringRoomRegistry registry = new SparringRoomRegistry();
        try {
            String roomCode = registry.create().roomCode();

            assertThatThrownBy(() -> registry.apply(roomCode, false, "setCompetitorName", "red", null, null, null, null, "Red A"))
                    .isInstanceOf(ApiException.class);
            assertThatThrownBy(() -> registry.apply(roomCode, true, "setCompetitorName", "red", null, null, null, null, "   "))
                    .isInstanceOf(ApiException.class);
            assertThatThrownBy(() -> registry.apply(roomCode, true, "setCompetitorName", "red", null, null, null, null, "A".repeat(31)))
                    .isInstanceOf(ApiException.class);
            assertThatThrownBy(() -> registry.apply(roomCode, true, "operatorAdjustScore", "red", null, 100, null, null, null))
                    .isInstanceOf(ApiException.class);
            assertThatThrownBy(() -> registry.apply(roomCode, true, "operatorAdjustPenalty", "red", null, 21, null, null, null))
                    .isInstanceOf(ApiException.class);
        } finally {
            registry.shutdown();
        }
    }

    @Test
    void rejectsOperatorCommandsFromJudgeDevicesAndDoesNotShareRoomsBetweenRegistries() {
        SparringRoomRegistry first = new SparringRoomRegistry();
        SparringRoomRegistry second = new SparringRoomRegistry();
        try {
            String roomCode = first.create().roomCode();
            assertThatThrownBy(() -> first.apply(roomCode, false, "end", null, null, null, null, null, null))
                    .isInstanceOf(ApiException.class);
            assertThatThrownBy(() -> second.get(roomCode)).isInstanceOf(ApiException.class);
        } finally {
            first.shutdown();
            second.shutdown();
        }
    }
}

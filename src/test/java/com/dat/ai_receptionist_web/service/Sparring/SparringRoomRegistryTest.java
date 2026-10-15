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

            var updated = registry.apply(roomCode, false, "judgeScore", "red", 2, null, null, null);
            assertThat(updated.snapshot().redScore()).isEqualTo(2);
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
            assertThatThrownBy(() -> first.apply(roomCode, false, "end", null, null, null, null, null))
                    .isInstanceOf(ApiException.class);
            assertThatThrownBy(() -> second.get(roomCode)).isInstanceOf(ApiException.class);
        } finally {
            first.shutdown();
            second.shutdown();
        }
    }
}

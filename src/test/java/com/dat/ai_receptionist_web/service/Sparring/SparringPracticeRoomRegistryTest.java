package com.dat.ai_receptionist_web.service.Sparring;

import com.dat.ai_receptionist_web.dto.Sparring.SparringPracticeCreateRequest;
import com.dat.ai_receptionist_web.dto.Sparring.SparringPracticeRoomResponse;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SparringPracticeRoomRegistryTest {
    @Test
    void startsOnlyWhenEveryJudgeReadyAndCountsMatchingVotesOnce() throws Exception {
        SparringPracticeRoomRegistry registry = new SparringPracticeRoomRegistry();
        SparringPracticeRoomResponse room = registry.create(new SparringPracticeCreateRequest(2, 2));
        UUID judgeOne = UUID.randomUUID(); UUID judgeTwo = UUID.randomUUID(); UUID host = UUID.randomUUID();

        registry.connected(room.roomCode(), judgeOne, false);
        registry.connected(room.roomCode(), judgeTwo, false);
        registry.videoUploaded(room.roomCode(), "practice/" + room.roomCode() + "/video.mp4", "https://signed.example/video.mp4");
        registry.command(room.roomCode(), judgeOne, false, "ready", null, null);
        registry.command(room.roomCode(), judgeTwo, false, "ready", null, null);
        assertTrue(registry.get(room.roomCode()).snapshot().canStart());

        registry.command(room.roomCode(), host, true, "start", null, null);
        Thread.sleep(3_200);
        registry.command(room.roomCode(), judgeOne, false, "judgeScore", "red", 3);
        registry.command(room.roomCode(), judgeTwo, false, "judgeScore", "red", 3);
        assertEquals(3, registry.get(room.roomCode()).snapshot().redScore());

        assertThrows(RuntimeException.class, () -> registry.command(room.roomCode(), judgeOne, false, "judgeScore", "red", 3));
        assertEquals(3, registry.get(room.roomCode()).snapshot().redScore());
    }

    @Test
    void disconnectPausesRunningPracticeImmediately() throws Exception {
        SparringPracticeRoomRegistry registry = new SparringPracticeRoomRegistry();
        SparringPracticeRoomResponse room = registry.create(new SparringPracticeCreateRequest(1, 1));
        UUID judge = UUID.randomUUID();
        registry.connected(room.roomCode(), judge, false);
        registry.videoUploaded(room.roomCode(), "practice/" + room.roomCode() + "/video.mp4", "https://signed.example/video.mp4");
        registry.command(room.roomCode(), judge, false, "ready", null, null);
        registry.command(room.roomCode(), UUID.randomUUID(), true, "start", null, null);
        Thread.sleep(3_200);
        registry.disconnected(room.roomCode(), judge, false);
        assertEquals("PAUSED", registry.get(room.roomCode()).snapshot().status());
    }
}

package com.dat.ai_receptionist_web.service.Training.scan;

import com.dat.ai_receptionist_web.domain.Training.scan.FaceCheckInStage;
import com.dat.ai_receptionist_web.dto.Training.scan.FaceCheckInScanDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FaceCheckInEventStream {
    private final ConcurrentHashMap<UUID, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID requestId, long timeoutMillis) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        emitters.computeIfAbsent(requestId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitter.onCompletion(() -> remove(requestId, emitter));
        emitter.onTimeout(() -> remove(requestId, emitter));
        emitter.onError(ignored -> remove(requestId, emitter));
        return emitter;
    }

    public void send(SseEmitter emitter, FaceCheckInScanDTO.EventResponse event) throws IOException {
        emitter.send(SseEmitter.event()
                .id(Long.toString(event.eventId()))
                .name("face-check-in")
                .data(event));
    }

    public void publish(FaceCheckInScanDTO.EventResponse event) {
        Set<SseEmitter> subscribers = emitters.getOrDefault(event.requestId(), Set.of());
        for (SseEmitter emitter : subscribers) {
            try {
                send(emitter, event);
                if (event.stage().isTerminal()) {
                    emitter.complete();
                }
            } catch (IOException | IllegalStateException exception) {
                emitter.completeWithError(exception);
            }
        }
        if (event.stage().isTerminal()) {
            emitters.remove(event.requestId());
        }
    }

    private void remove(UUID requestId, SseEmitter emitter) {
        emitters.computeIfPresent(requestId, (ignored, subscribers) -> {
            subscribers.remove(emitter);
            return subscribers.isEmpty() ? null : subscribers;
        });
    }
}

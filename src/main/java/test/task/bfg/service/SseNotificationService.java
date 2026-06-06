package test.task.bfg.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseNotificationService {

    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);

        emittersByUserId
                .computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(error -> removeEmitter(userId, emitter));

        sendToEmitter(
                emitter,
                "connected",
                Map.of(
                        "userId", userId,
                        "connectedAt", Instant.now().toString()
                )
        );

        return emitter;
    }

    public boolean hasSubscribers(UUID userId) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);
        return emitters != null && !emitters.isEmpty();
    }

    public void send(UUID userId, String eventName, Object payload) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);

        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            boolean sent = sendToEmitter(emitter, eventName, payload);

            if (!sent) {
                removeEmitter(userId, emitter);
            }
        }
    }

    private boolean sendToEmitter(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(
                    SseEmitter.event()
                            .name(eventName)
                            .data(payload, MediaType.APPLICATION_JSON)
            );
            return true;
        } catch (IOException | IllegalStateException exception) {
            return false;
        }
    }

    private void removeEmitter(UUID userId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);

        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);

        if (emitters.isEmpty()) {
            emittersByUserId.remove(userId);
        }
    }
}
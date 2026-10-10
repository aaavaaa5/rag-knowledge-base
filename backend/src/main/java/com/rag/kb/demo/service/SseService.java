package com.rag.kb.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String clientId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(clientId, emitter);

        emitter.onCompletion(() -> {
            emitters.remove(clientId);
            log.debug("SSE completed: {}", clientId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(clientId);
            log.debug("SSE timeout: {}", clientId);
        });
        emitter.onError(e -> {
            emitters.remove(clientId);
            log.debug("SSE error: {} - {}", clientId, e.getMessage());
        });

        log.debug("SSE registered: {}", clientId);
        return emitter;
    }

    public void sendEvent(Long docId, String fileName, String status, Long kbId) {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("docId", docId);
        data.put("fileName", fileName);
        data.put("status", status);
        data.put("kbId", kbId);

        if (emitters.isEmpty()) {
            log.debug("No SSE clients connected, skipping event for doc {}", docId);
            return;
        }

        emitters.forEach((clientId, emitter) -> {
            try {
                emitter.send(data);
                log.debug("SSE sent to {}: docId={}, status={}", clientId, docId, status);
            } catch (IOException e) {
                log.debug("SSE send failed for {}, removing", clientId);
                emitters.remove(clientId);
            }
        });
    }

    public int getConnectionCount() {
        return emitters.size();
    }
}

package com.rag.kb.demo.controller;

import com.rag.kb.demo.dto.ChatSendDTO;
import com.rag.kb.demo.service.StreamingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class StreamingController {

    private final StreamingService streamingService;

    @PostMapping("/api/chat/send/stream")
    public SseEmitter streamChat(@Valid @RequestBody ChatSendDTO dto) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        new Thread(() -> {
            try {
                streamingService.streamChat(emitter, dto.getSessionId(), dto.getMessage(), dto.getTopK());
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage() != null ? e.getMessage() : "未知错误"));
                } catch (IOException ex) {
                    // ignore
                }
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }
}

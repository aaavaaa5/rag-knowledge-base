package com.rag.kb.demo.controller;

import com.rag.kb.demo.dto.ChatSendDTO;
import com.rag.kb.demo.entity.Conversation;
import com.rag.kb.demo.service.ConversationService;
import com.rag.kb.demo.service.RagService;
import com.rag.kb.demo.vo.ChatResponseVO;
import com.rag.kb.demo.vo.ConversationVO;
import com.rag.kb.demo.vo.Result;
import com.rag.kb.demo.vo.SessionDetailVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ConversationService conversationService;
    private final RagService ragService;

    @GetMapping("/api/knowledge-bases/{kbId}/sessions")
    public Result<List<ConversationVO>> listSessions(@PathVariable Long kbId) {
        return Result.success(conversationService.listSessions(kbId));
    }

    @PostMapping("/api/knowledge-bases/{kbId}/sessions")
    public Result<SessionDetailVO> createSession(@PathVariable Long kbId) {
        return Result.success(conversationService.createSession(kbId));
    }

    @GetMapping("/api/knowledge-bases/{kbId}/sessions/{sessionId}")
    public Result<SessionDetailVO> getSession(
            @PathVariable Long kbId, @PathVariable Long sessionId) {
        return Result.success(conversationService.getSession(kbId, sessionId));
    }

    @DeleteMapping("/api/knowledge-bases/{kbId}/sessions/{sessionId}")
    public Result<Void> deleteSession(
            @PathVariable Long kbId, @PathVariable Long sessionId) {
        conversationService.deleteSession(kbId, sessionId);
        return Result.success();
    }

    @PostMapping("/api/chat/send")
    public Result<ChatResponseVO> sendMessage(@Valid @RequestBody ChatSendDTO dto) {
        Conversation conv = conversationService.getConversationById(dto.getSessionId());
        ChatResponseVO response = ragService.askQuestion(
                dto.getSessionId(), dto.getMessage(), dto.getTopK(), conv.getKbId());
        return Result.success(response);
    }
}

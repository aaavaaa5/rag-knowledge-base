package com.rag.kb.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.kb.demo.common.BusinessException;
import com.rag.kb.demo.entity.Conversation;
import com.rag.kb.demo.entity.Message;
import com.rag.kb.demo.mapper.ConversationMapper;
import com.rag.kb.demo.mapper.MessageMapper;
import com.rag.kb.demo.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    public List<ConversationVO> listSessions(Long kbId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getKbId, kbId);
        wrapper.orderByDesc(Conversation::getUpdatedAt);

        return conversationMapper.selectList(wrapper).stream()
                .map(this::toConversationVO)
                .collect(Collectors.toList());
    }

    public Conversation getConversationById(Long sessionId) {
        Conversation conv = conversationMapper.selectById(sessionId);
        if (conv == null) {
            throw new BusinessException(404, "会话不存在");
        }
        return conv;
    }

    public SessionDetailVO createSession(Long kbId) {
        Conversation conv = new Conversation();
        conv.setKbId(kbId);
        conv.setTitle("新对话");
        conv.setCreatedAt(LocalDateTime.now());
        conv.setUpdatedAt(LocalDateTime.now());
        conversationMapper.insert(conv);
        return toSessionDetailVO(conv, Collections.emptyList());
    }

    public SessionDetailVO getSession(Long kbId, Long sessionId) {
        Conversation conv = conversationMapper.selectById(sessionId);
        if (conv == null || !conv.getKbId().equals(kbId)) {
            throw new BusinessException(404, "会话不存在");
        }

        LambdaQueryWrapper<Message> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.eq(Message::getConversationId, sessionId);
        msgWrapper.orderByAsc(Message::getCreatedAt);
        List<Message> messages = messageMapper.selectList(msgWrapper);

        return toSessionDetailVO(conv, messages);
    }

    public void deleteSession(Long kbId, Long sessionId) {
        Conversation conv = conversationMapper.selectById(sessionId);
        if (conv == null || !conv.getKbId().equals(kbId)) {
            throw new BusinessException(404, "会话不存在");
        }
        messageMapper.delete(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, sessionId));
        conversationMapper.deleteById(sessionId);
    }

    public void deleteByKbId(Long kbId) {
        List<Conversation> convs = conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>().eq(Conversation::getKbId, kbId));
        for (Conversation conv : convs) {
            messageMapper.delete(new LambdaQueryWrapper<Message>()
                    .eq(Message::getConversationId, conv.getId()));
        }
        conversationMapper.delete(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getKbId, kbId));
    }

    public MessageVO addMessage(Long conversationId, String role, String content,
                                 List<CitationVO> citations, boolean updateTitle) {
        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setRole(role);
        msg.setContent(content);
        if (citations != null && !citations.isEmpty()) {
            try {
                msg.setReferences(objectMapper.writeValueAsString(citations));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize citations", e);
            }
        }
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);

        // Update conversation
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv != null) {
            if (updateTitle && content.length() > 0) {
                String title = content.length() > 20 ? content.substring(0, 20) + "..." : content;
                conv.setTitle(title);
            }
            conv.setUpdatedAt(LocalDateTime.now());
            conversationMapper.updateById(conv);
        }

        return toMessageVO(msg, citations);
    }

    private ConversationVO toConversationVO(Conversation conv) {
        ConversationVO vo = new ConversationVO();
        vo.setId(conv.getId());
        vo.setKbId(conv.getKbId());
        vo.setTitle(conv.getTitle());

        Long count = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conv.getId()));
        vo.setMessageCount(count != null ? count.intValue() : 0);

        vo.setCreatedAt(conv.getCreatedAt() != null
                ? conv.getCreatedAt().format(DT_FMT) : null);
        vo.setUpdatedAt(conv.getUpdatedAt() != null
                ? conv.getUpdatedAt().format(DT_FMT) : null);
        return vo;
    }

    private SessionDetailVO toSessionDetailVO(Conversation conv, List<Message> messages) {
        SessionDetailVO vo = new SessionDetailVO();
        vo.setId(conv.getId());
        vo.setKbId(conv.getKbId());
        vo.setTitle(conv.getTitle());
        vo.setMessages(messages.stream().map(this::toMessageVO).collect(Collectors.toList()));
        vo.setCreatedAt(conv.getCreatedAt() != null
                ? conv.getCreatedAt().format(DT_FMT) : null);
        vo.setUpdatedAt(conv.getUpdatedAt() != null
                ? conv.getUpdatedAt().format(DT_FMT) : null);
        return vo;
    }

    private MessageVO toMessageVO(Message msg) {
        MessageVO vo = new MessageVO();
        vo.setId(msg.getId());
        vo.setRole(msg.getRole());
        vo.setContent(msg.getContent());

        if (msg.getReferences() != null && !msg.getReferences().isBlank()) {
            try {
                List<CitationVO> citations = objectMapper.readValue(msg.getReferences(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, CitationVO.class));
                vo.setCitations(citations);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse citations", e);
            }
        }

        vo.setCreatedAt(msg.getCreatedAt() != null
                ? msg.getCreatedAt().format(DT_FMT) : null);
        return vo;
    }

    private MessageVO toMessageVO(Message msg, List<CitationVO> citations) {
        MessageVO vo = toMessageVO(msg);
        if (citations != null) {
            vo.setCitations(citations);
        }
        return vo;
    }
}

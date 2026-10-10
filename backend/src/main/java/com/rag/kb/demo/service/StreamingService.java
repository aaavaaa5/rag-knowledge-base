package com.rag.kb.demo.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.kb.demo.mapper.DocumentMapper;
import com.rag.kb.demo.vo.CitationVO;
import com.rag.kb.demo.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingService {

    private final EmbeddingClient embeddingClient;
    private final ElasticsearchClient esClient;
    private final ConversationService conversationService;
    private final DocumentMapper documentMapper;
    private final ObjectMapper objectMapper;

    public void streamChat(SseEmitter emitter, Long sessionId, String question, int topK) {
        try {
            var conv = conversationService.getConversationById(sessionId);
            Long kbId = conv.getKbId();

            // Vectorize question
            List<Double> queryVector = embeddingClient.embed(question);

            // Search ES chunks
            List<Float> floatVector = queryVector.stream().map(Double::floatValue).collect(Collectors.toList());
            List<SearchResult> searchResults = new ArrayList<>();
            try {
                SearchResponse<JsonNode> response = esClient.search(s -> s
                        .index("chunks").size(topK)
                        .knn(k -> k.field("embedding").queryVector(floatVector)
                                .k((long) topK).numCandidates((long) (topK * 10))
                                .filter(f -> f.term(t -> t.field("kbId").value(kbId)))),
                        JsonNode.class);
                for (Hit<JsonNode> hit : response.hits().hits()) {
                    if (hit.source() == null) continue;
                    SearchResult sr = new SearchResult();
                    sr.docId = hit.source().get("docId").asLong();
                    sr.content = hit.source().get("content").asText();
                    sr.score = hit.score() != null ? hit.score() : 0;
                    searchResults.add(sr);
                }
            } catch (Exception e) {
                log.warn("ES search failed, continuing without context", e);
            }

            // Build context and citations
            String context = searchResults.stream().map(r -> r.content).collect(Collectors.joining("\n\n---\n\n"));
            List<CitationVO> citations = new ArrayList<>();
            for (SearchResult sr : searchResults) {
                CitationVO c = new CitationVO();
                c.setDocId(sr.docId);
                c.setDocName(getDocName(sr.docId));
                c.setSnippet(sr.content.length() > 200 ? sr.content.substring(0, 200) + "..." : sr.content);
                c.setScore(sr.score);
                citations.add(c);
            }

            String systemPrompt = "你是一个知识库问答助手。请基于以下上下文内容回答用户的问题。"
                    + "如果上下文中没有足够的信息，请如实告知。" +
        "回答要求：\n- 段落之间用空行分隔。\n- 列举内容时每条独立成行。\n- 无序列表用 - 开头，有序列表用 1. 2. 3. 开头。\n- 重要内容使用 **粗体**。\n\n上下文：\n" + context;

            // Call Ollama streaming API
            StringBuilder fullContent = new StringBuilder();
            URL url = new URL("http://localhost:11434/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", "qwen2.5:7b");
            body.put("stream", true);
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> sysMsg = new HashMap<>();
            sysMsg.put("role", "system"); sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);
            Map<String, String> usrMsg = new HashMap<>();
            usrMsg.put("role", "user"); usrMsg.put("content", question);
            messages.add(usrMsg);
            body.put("messages", messages);

            objectMapper.writeValue(conn.getOutputStream(), body);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;
                    if (data.isEmpty()) continue;
                    try {
                        JsonNode node = objectMapper.readTree(data);
                        String content = node.path("choices").get(0).path("delta").path("content").asText();
                        if (content != null && !content.isEmpty()) {
                            fullContent.append(content);
                            emitter.send(SseEmitter.event().name("delta").data(content));
                        }
                    } catch (Exception ignored) {}
                }
            }

            // Save messages
            String answer = fullContent.toString();
            MessageVO userMsg = conversationService.addMessage(sessionId, "user", question, null, true);
            MessageVO aiMsg = conversationService.addMessage(sessionId, "assistant", answer, citations, false);

            Map<String, Object> doneData = new LinkedHashMap<>();
            doneData.put("userMsg", userMsg);
            doneData.put("aiMsg", aiMsg);
            emitter.send(SseEmitter.event().name("done").data(objectMapper.writeValueAsString(doneData)));
            emitter.complete();

        } catch (Exception e) {
            log.error("Stream chat failed", e);
            try {
                emitter.send(SseEmitter.event().name("error").data(e.getMessage() != null ? e.getMessage() : "未知错误"));
            } catch (IOException ignored) {}
            emitter.completeWithError(e);
        }
    }

    private String getDocName(Long docId) {
        try {
            var doc = documentMapper.selectById(docId);
            return doc != null ? doc.getFileName() : "未知文档";
        } catch (Exception e) {
            return "未知文档";
        }
    }

    private static class SearchResult {
        Long docId; String content; double score;
    }
}

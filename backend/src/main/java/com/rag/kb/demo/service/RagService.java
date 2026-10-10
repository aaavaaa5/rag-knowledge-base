package com.rag.kb.demo.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.kb.demo.common.BusinessException;
import com.rag.kb.demo.mapper.DocumentMapper;
import com.rag.kb.demo.vo.ChatResponseVO;
import com.rag.kb.demo.vo.CitationVO;
import com.rag.kb.demo.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final EmbeddingClient embeddingClient;
    private final ChatClient chatClient;
    private final ElasticsearchClient esClient;
    private final ConversationService conversationService;
    private final DocumentMapper documentMapper;
    private final ObjectMapper objectMapper;

    public ChatResponseVO askQuestion(Long sessionId, String question, int topK, Long kbId) {
        // Step 1: Vectorize the question
        List<Double> queryVector = embeddingClient.embed(question);

        // Step 2: Search ES for relevant chunks
        List<SearchResult> searchResults = searchChunks(kbId, queryVector, topK);

        // Step 3: Build context from search results
        String context = searchResults.stream()
                .map(r -> r.content)
                .collect(Collectors.joining("\n\n---\n\n"));

        // Step 4: Build citations
        List<CitationVO> citations = new ArrayList<>();
        for (SearchResult sr : searchResults) {
            CitationVO citation = new CitationVO();
            citation.setDocId(sr.docId);
            citation.setDocName(getDocumentName(sr.docId));
            citation.setSnippet(truncateSnippet(sr.content));
            citation.setScore(sr.score);
            citations.add(citation);
        }

        // Step 5: Build prompt and call LLM
        String systemPrompt = "你是一个知识库问答助手。请基于以下上下文内容回答用户的问题。" +
                "如果上下文中没有足够的信息，请如实告知。" +
        "回答要求：\n- 段落之间用空行分隔。\n- 列举内容时每条独立成行。\n- 无序列表用 - 开头，有序列表用 1. 2. 3. 开头。\n- 重要内容使用 **粗体**。\n\n上下文：\n" + context;
        String fullPrompt = systemPrompt + "\n\n用户问题：" + question;

        String answer;
        try {
            answer = chatClient.call(fullPrompt);
        } catch (Exception e) {
            log.error("LLM call failed", e);
            answer = "抱歉，AI 服务暂时不可用，请稍后重试。";
        }

        // Step 6: Save messages
        MessageVO userMsg = conversationService.addMessage(sessionId, "user", question, null, true);
        MessageVO aiMsg = conversationService.addMessage(sessionId, "assistant", answer, citations, false);

        // Step 7: Return response
        ChatResponseVO response = new ChatResponseVO();
        response.setUserMsg(userMsg);
        response.setAiMsg(aiMsg);
        return response;
    }

    private List<SearchResult> searchChunks(Long kbId, List<Double> queryVector, int topK) {
        List<Float> floatVector = queryVector.stream().map(Double::floatValue).collect(java.util.stream.Collectors.toList());
        try {
            SearchResponse<JsonNode> response = esClient.search(s -> s
                    .index("chunks")
                    .size(topK)
                    .knn(k -> k
                            .field("embedding")
                            .queryVector(floatVector)
                            .k((long) topK)
                            .numCandidates((long) (topK * 10))
                            .filter(f -> f
                                    .term(t -> t
                                            .field("kbId")
                                            .value(kbId)))
                    ), JsonNode.class);

            List<SearchResult> results = new ArrayList<>();
            for (Hit<JsonNode> hit : response.hits().hits()) {
                if (hit.source() == null) continue;
                SearchResult sr = new SearchResult();
                sr.docId = hit.source().get("docId").asLong();
                sr.content = hit.source().get("content").asText();
                sr.score = hit.score() != null ? hit.score() : 0;
                results.add(sr);
            }
            return results;
        } catch (Exception e) {
            log.error("ES search failed", e);
            return Collections.emptyList();
        }
    }

    private String getDocumentName(Long docId) {
        try {
            var doc = documentMapper.selectById(docId);
            return doc != null ? doc.getFileName() : "未知文档";
        } catch (Exception e) {
            return "未知文档";
        }
    }

    private String truncateSnippet(String text) {
        if (text == null) return "";
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }

    private static class SearchResult {
        Long docId;
        String content;
        double score;
    }
}

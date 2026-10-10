package com.rag.kb.demo.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.kb.demo.common.BusinessException;
import com.rag.kb.demo.entity.Document;
import com.rag.kb.demo.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentPipelineService {

    private final DocumentMapper documentMapper;
    private final ElasticsearchClient esClient;
    private final EmbeddingClient embeddingClient;
    private final ObjectMapper objectMapper;
    private final SseService sseService;

    @Async("documentPipelineExecutor")
    public void processDocument(Long docId) {
        Document doc = documentMapper.selectById(docId);
        if (doc == null) return;

        try {
            // Step 1: Update status to processing
            doc.setStatus("processing");
            documentMapper.updateById(doc);

            // Step 2: Read file content
            String content;
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(doc.getFilePath()));
                content = new String(bytes, StandardCharsets.UTF_8);
            } catch (Exception e) {
                try {
                    byte[] bytes = Files.readAllBytes(Paths.get(doc.getFilePath()));
                    content = new String(bytes, StandardCharsets.ISO_8859_1);
                } catch (Exception ex) {
                    throw new BusinessException(500, "文件读取失败", ex);
                }
            }

            // Step 3: Split into chunks
            List<String> chunks = splitIntoChunks(content, doc.getChunkSize(), doc.getOverlap());

            // Step 4: Process each chunk
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                List<Double> embedding = embeddingClient.embed(chunkText);

                // Step 5: Write to ES
                Map<String, Object> chunkDoc = new LinkedHashMap<>();
                chunkDoc.put("docId", doc.getId());
                chunkDoc.put("kbId", doc.getKbId());
                chunkDoc.put("content", chunkText);
                chunkDoc.put("embedding", embedding);
                chunkDoc.put("createdAt", Instant.now().toString());

                IndexRequest<Object> indexRequest = new IndexRequest.Builder<>()
                        .index("chunks")
                        .id(doc.getId() + "_" + i)
                        .document(chunkDoc)
                        .build();
                esClient.index(indexRequest);
            }

            // Step 6: Update status to completed
            doc.setStatus("completed");
            documentMapper.updateById(doc);
            sseService.sendEvent(doc.getId(), doc.getFileName(), "completed", doc.getKbId());
            log.info("Document {} processed successfully: {} chunks", docId, chunks.size());

        } catch (Exception e) {
            log.error("Document {} processing failed", docId, e);
            doc.setStatus("failed");
            documentMapper.updateById(doc);
            sseService.sendEvent(doc.getId(), doc.getFileName(), "failed", doc.getKbId());
        }
    }

    public void deleteDocumentChunks(Long docId) {
        try {
            esClient.deleteByQuery(d -> d
                    .index("chunks")
                    .query(q -> q
                            .term(t -> t
                                    .field("docId")
                                    .value(docId))));
        } catch (Exception e) {
            log.error("Failed to delete ES chunks for doc {}", docId, e);
        }
    }

    public void deleteChunksByKbId(Long kbId) {
        try {
            esClient.deleteByQuery(d -> d
                    .index("chunks")
                    .query(q -> q
                            .term(t -> t
                                    .field("kbId")
                                    .value(kbId))));
        } catch (Exception e) {
            log.error("Failed to delete ES chunks for kb {}", kbId, e);
        }
    }

    List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        if (text == null || text.isEmpty()) return Collections.emptyList();
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start += chunkSize - overlap;
            if (start >= text.length()) break;
        }
        return chunks;
    }
}

package com.rag.kb.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.kb.demo.common.BusinessException;
import com.rag.kb.demo.entity.Document;
import com.rag.kb.demo.mapper.DocumentMapper;
import com.rag.kb.demo.vo.DocumentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentMapper documentMapper;
    private final DocumentPipelineService documentPipelineService;

    public List<DocumentVO> listDocuments(Long kbId) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getKbId, kbId);
        wrapper.orderByDesc(Document::getCreatedAt);

        return documentMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    public DocumentVO uploadDocument(Long kbId, MultipartFile file, Integer chunkSize, Integer overlap) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".txt")) {
            throw new BusinessException("仅支持 .txt 文件");
        }
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new BusinessException("文件大小不能超过 50MB");
        }

        // Store file
        String uploadDir = System.getProperty("user.dir") + "/uploads/" + kbId + "/";
        Path filePath;
        try {
            Files.createDirectories(Paths.get(uploadDir));
            filePath = Paths.get(uploadDir, fileName);
            Files.write(filePath, file.getBytes());
        } catch (IOException e) {
            throw new BusinessException(500, "文件存储失败", e);
        }

        // Save document record
        Document doc = new Document();
        doc.setKbId(kbId);
        doc.setFileName(fileName);
        doc.setFilePath(filePath.toString());
        doc.setChunkSize(chunkSize);
        doc.setOverlap(overlap);
        doc.setStatus("pending");
        doc.setSize(file.getSize());
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.insert(doc);

        // Trigger async processing
        documentPipelineService.processDocument(doc.getId());

        return toVO(doc);
    }

    public void deleteDocument(Long kbId, Long docId) {
        Document doc = documentMapper.selectById(docId);
        if (doc == null || !doc.getKbId().equals(kbId)) {
            throw new BusinessException(404, "文档不存在");
        }
        // Delete ES chunks (delegated to pipeline service)
        documentPipelineService.deleteDocumentChunks(docId);
        // Delete physical file
        try {
            Files.deleteIfExists(Paths.get(doc.getFilePath()));
        } catch (IOException ignored) {
        }
        documentMapper.deleteById(docId);
    }

    public void deleteByKbId(Long kbId) {
        List<Document> docs = documentMapper.selectList(
                new LambdaQueryWrapper<Document>().eq(Document::getKbId, kbId));
        for (Document doc : docs) {
            try {
                Files.deleteIfExists(Paths.get(doc.getFilePath()));
            } catch (IOException ignored) {
            }
        }
        documentMapper.delete(new LambdaQueryWrapper<Document>().eq(Document::getKbId, kbId));
    }

    public Long countByKbId(Long kbId) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getKbId, kbId);
        Long count = documentMapper.selectCount(wrapper);
        return count != null ? count : 0L;
    }

    private DocumentVO toVO(Document doc) {
        DocumentVO vo = new DocumentVO();
        vo.setId(doc.getId());
        vo.setFileName(doc.getFileName());
        vo.setKbId(doc.getKbId());
        vo.setSize(doc.getSize());
        vo.setChunkSize(doc.getChunkSize());
        vo.setOverlap(doc.getOverlap());
        vo.setStatus(doc.getStatus());
        vo.setUploadedAt(doc.getCreatedAt() != null
                ? doc.getCreatedAt().toString().replace("T", " ").substring(0, 19)
                : null);
        return vo;
    }
}

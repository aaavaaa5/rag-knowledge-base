package com.rag.kb.demo.controller;

import com.rag.kb.demo.dto.KbCreateDTO;
import com.rag.kb.demo.service.KnowledgeBaseService;
import com.rag.kb.demo.service.DocumentService;
import com.rag.kb.demo.vo.DocumentVO;
import com.rag.kb.demo.vo.KbVO;
import com.rag.kb.demo.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentService documentService;

    @GetMapping
    public Result<List<KbVO>> listKbs(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(knowledgeBaseService.listKbs(keyword));
    }

    @PostMapping
    public Result<KbVO> createKb(@Valid @RequestBody KbCreateDTO dto) {
        return Result.success(knowledgeBaseService.createKb(dto.getName()));
    }

    @GetMapping("/{id}")
    public Result<KbVO> getKb(@PathVariable Long id) {
        return Result.success(knowledgeBaseService.getKb(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteKb(@PathVariable Long id) {
        knowledgeBaseService.deleteKb(id);
        return Result.success();
    }

    @GetMapping("/{kbId}/documents")
    public Result<List<DocumentVO>> listDocuments(
            @PathVariable Long kbId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(documentService.listDocuments(kbId));
    }

    @PostMapping("/{kbId}/documents")
    public Result<DocumentVO> uploadDocument(
            @PathVariable Long kbId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "500") Integer chunkSize,
            @RequestParam(defaultValue = "50") Integer overlap) {
        return Result.success(documentService.uploadDocument(kbId, file, chunkSize, overlap));
    }

    @DeleteMapping("/{kbId}/documents/{docId}")
    public Result<Void> deleteDocument(@PathVariable Long kbId, @PathVariable Long docId) {
        documentService.deleteDocument(kbId, docId);
        return Result.success();
    }
}

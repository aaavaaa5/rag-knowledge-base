package com.rag.kb.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.kb.demo.common.BusinessException;
import com.rag.kb.demo.entity.Kb;
import com.rag.kb.demo.mapper.KbMapper;
import com.rag.kb.demo.vo.KbVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KbMapper kbMapper;
    private final DocumentService documentService;
    private final ConversationService conversationService;

    public List<KbVO> listKbs(String keyword) {
        LambdaQueryWrapper<Kb> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Kb::getName, keyword);
        }
        wrapper.orderByDesc(Kb::getCreatedAt);

        List<Kb> kbs = kbMapper.selectList(wrapper);
        return kbs.stream().map(this::toVO).collect(Collectors.toList());
    }

    public KbVO getKb(Long id) {
        Kb kb = kbMapper.selectById(id);
        if (kb == null) {
            throw new BusinessException(404, "知识库不存在");
        }
        return toVO(kb);
    }

    public KbVO createKb(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("知识库名称不能为空");
        }

        LambdaQueryWrapper<Kb> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(Kb::getName, name);
        if (kbMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessException("知识库名称已存在");
        }

        Kb kb = new Kb();
        kb.setName(name);
        kb.setCreatedAt(LocalDateTime.now());
        kbMapper.insert(kb);

        return toVO(kb);
    }

    public void deleteKb(Long id) {
        Kb kb = kbMapper.selectById(id);
        if (kb == null) {
            throw new BusinessException(404, "知识库不存在");
        }
        conversationService.deleteByKbId(id);
        documentService.deleteByKbId(id);
        kbMapper.deleteById(id);
    }

    private KbVO toVO(Kb kb) {
        KbVO vo = new KbVO();
        vo.setId(kb.getId());
        vo.setName(kb.getName());
        vo.setDocCount(documentService.countByKbId(kb.getId()));
        vo.setCreatedAt(kb.getCreatedAt() != null ? kb.getCreatedAt().toString().replace("T", " ").substring(0, 19) : null);
        return vo;
    }
}

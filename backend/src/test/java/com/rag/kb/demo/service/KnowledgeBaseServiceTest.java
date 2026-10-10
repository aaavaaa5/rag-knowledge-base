package com.rag.kb.demo.service;

import com.rag.kb.demo.common.BusinessException;
import com.rag.kb.demo.entity.Kb;
import com.rag.kb.demo.mapper.KbMapper;
import com.rag.kb.demo.vo.KbVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceTest {

    @Mock
    private KbMapper kbMapper;

    @Mock
    private DocumentService documentService;

    @Mock
    private ConversationService conversationService;

    @InjectMocks
    private KnowledgeBaseService knowledgeBaseService;

    @Test
    void createKb_withValidName_shouldSucceed() {
        when(kbMapper.selectCount(any())).thenReturn(0L);
        when(kbMapper.insert(any())).thenAnswer(i -> {
            Kb k = i.getArgument(0);
            k.setId(1L);
            return 1;
        });

        KbVO vo = knowledgeBaseService.createKb("产品手册");

        assertNotNull(vo);
        assertEquals("产品手册", vo.getName());
        assertEquals(0L, vo.getDocCount());
        verify(kbMapper).insert(any());
    }

    @Test
    void createKb_withBlankName_shouldThrow() {
        assertThrows(BusinessException.class,
                () -> knowledgeBaseService.createKb(""));
        verify(kbMapper, never()).insert(any());
    }

    @Test
    void createKb_withDuplicateName_shouldThrow() {
        when(kbMapper.selectCount(any())).thenReturn(1L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> knowledgeBaseService.createKb("重复名称"));
        assertEquals("知识库名称已存在", e.getMessage());
    }

    @Test
    void getKb_withExistingId_shouldReturnVO() {
        Kb kb = new Kb();
        kb.setId(1L);
        kb.setName("测试库");
        kb.setCreatedAt(LocalDateTime.now());

        when(kbMapper.selectById(1L)).thenReturn(kb);

        KbVO vo = knowledgeBaseService.getKb(1L);
        assertEquals("测试库", vo.getName());
        assertNotNull(vo.getCreatedAt());
    }

    @Test
    void getKb_withNonExistingId_shouldThrow() {
        when(kbMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> knowledgeBaseService.getKb(999L));
    }

    @Test
    void deleteKb_withExistingId_shouldDeleteAll() {
        Kb kb = new Kb();
        kb.setId(1L);
        kb.setName("测试库");

        when(kbMapper.selectById(1L)).thenReturn(kb);

        knowledgeBaseService.deleteKb(1L);

        verify(conversationService).deleteByKbId(1L);
        verify(documentService).deleteByKbId(1L);
        verify(kbMapper).deleteById(1L);
    }

    @Test
    void listKbs_shouldReturnAll() {
        Kb kb1 = new Kb(); kb1.setId(1L); kb1.setName("A");
        Kb kb2 = new Kb(); kb2.setId(2L); kb2.setName("B");
        when(kbMapper.selectList(any())).thenReturn(List.of(kb1, kb2));
        when(documentService.countByKbId(any())).thenReturn(0L);

        List<KbVO> result = knowledgeBaseService.listKbs(null);
        assertEquals(2, result.size());
    }
}

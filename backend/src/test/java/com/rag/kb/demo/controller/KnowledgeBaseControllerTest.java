package com.rag.kb.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.kb.demo.dto.KbCreateDTO;
import com.rag.kb.demo.service.KnowledgeBaseService;
import com.rag.kb.demo.service.DocumentService;
import com.rag.kb.demo.vo.KbVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KnowledgeBaseController.class)
class KnowledgeBaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KnowledgeBaseService knowledgeBaseService;

    @MockBean
    private DocumentService documentService;

    void listKbs_shouldReturnList() throws Exception {
        KbVO vo = new KbVO();
        vo.setId(1L);
        vo.setName("产品手册");
        vo.setDocCount(5L);

        when(knowledgeBaseService.listKbs(any())).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/knowledge-bases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("产品手册"));
    }

    @Test
    void createKb_shouldReturnCreated() throws Exception {
        KbVO vo = new KbVO();
        vo.setId(1L);
        vo.setName("新知识库");

        when(knowledgeBaseService.createKb("新知识库")).thenReturn(vo);

        KbCreateDTO dto = new KbCreateDTO();
        dto.setName("新知识库");

        mockMvc.perform(post("/api/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("新知识库"));
    }

    @Test
    void createKb_withEmptyName_shouldReturn400() throws Exception {
        KbCreateDTO dto = new KbCreateDTO();
        dto.setName("");

        mockMvc.perform(post("/api/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getKb_shouldReturnVO() throws Exception {
        KbVO vo = new KbVO();
        vo.setId(1L);
        vo.setName("产品手册");

        when(knowledgeBaseService.getKb(1L)).thenReturn(vo);

        mockMvc.perform(get("/api/knowledge-bases/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("产品手册"));
    }
}

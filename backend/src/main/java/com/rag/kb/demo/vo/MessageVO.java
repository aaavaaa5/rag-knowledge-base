package com.rag.kb.demo.vo;

import lombok.Data;

import java.util.List;

@Data
public class MessageVO {

    private Long id;
    private String role;
    private String content;
    private List<CitationVO> citations;
    private String createdAt;
}

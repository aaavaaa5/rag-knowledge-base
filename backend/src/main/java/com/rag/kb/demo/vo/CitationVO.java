package com.rag.kb.demo.vo;

import lombok.Data;

@Data
public class CitationVO {

    private Long docId;
    private String docName;
    private String snippet;
    private Double score;
}

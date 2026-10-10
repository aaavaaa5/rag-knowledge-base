package com.rag.kb.demo.vo;

import lombok.Data;

@Data
public class DocumentVO {

    private Long id;
    private String fileName;
    private Long kbId;
    private Long size;
    private Integer chunkSize;
    private Integer overlap;
    private String status;
    private String uploadedAt;
}

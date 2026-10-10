package com.rag.kb.demo.vo;

import lombok.Data;

@Data
public class ConversationVO {

    private Long id;
    private Long kbId;
    private String title;
    private Integer messageCount;
    private String createdAt;
    private String updatedAt;
}

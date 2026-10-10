package com.rag.kb.demo.vo;

import lombok.Data;

import java.util.List;

@Data
public class SessionDetailVO {

    private Long id;
    private Long kbId;
    private String title;
    private List<MessageVO> messages;
    private String createdAt;
    private String updatedAt;
}

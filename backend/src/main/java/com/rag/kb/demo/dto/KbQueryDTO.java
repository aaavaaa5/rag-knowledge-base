package com.rag.kb.demo.dto;

import lombok.Data;

@Data
public class KbQueryDTO {

    private String keyword;

    private Integer page = 1;

    private Integer size = 10;
}

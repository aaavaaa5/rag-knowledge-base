package com.rag.kb.demo.dto;

import lombok.Data;

@Data
public class DocumentQueryDTO {

    private Integer page = 1;

    private Integer size = 10;
}

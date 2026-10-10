package com.rag.kb.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document")
public class Document {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long kbId;

    private String fileName;

    private String filePath;

    private Integer chunkSize;

    private Integer overlap;

    private String status;

    private Long size;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

package com.rag.kb.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rag.kb.demo.entity.Document;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}

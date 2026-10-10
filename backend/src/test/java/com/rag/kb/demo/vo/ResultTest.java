package com.rag.kb.demo.vo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void success_shouldReturnCode200() {
        Result<Void> result = Result.success();
        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void success_withData_shouldReturnData() {
        Result<String> result = Result.success("hello");
        assertEquals(200, result.getCode());
        assertEquals("hello", result.getData());
    }

    @Test
    void error_shouldReturnCode400() {
        Result<Void> result = Result.error("出错了");
        assertEquals(400, result.getCode());
        assertEquals("出错了", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void error_withCode_shouldReturnCustomCode() {
        Result<Void> result = Result.error(404, "未找到");
        assertEquals(404, result.getCode());
        assertEquals("未找到", result.getMessage());
    }

    @Test
    void success_chain_shouldPreserveData() {
        KbVO vo = new KbVO();
        vo.setId(1L);
        vo.setName("test");
        Result<KbVO> result = Result.success(vo);
        assertEquals(1L, result.getData().getId());
        assertEquals("test", result.getData().getName());
    }
}

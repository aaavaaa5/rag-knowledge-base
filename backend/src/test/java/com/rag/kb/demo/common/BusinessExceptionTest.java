package com.rag.kb.demo.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void constructor_withMessage_shouldSetCode400() {
        BusinessException e = new BusinessException("错误消息");
        assertEquals(400, e.getCode());
        assertEquals("错误消息", e.getMessage());
    }

    @Test
    void constructor_withCodeAndMessage_shouldSetBoth() {
        BusinessException e = new BusinessException(404, "未找到");
        assertEquals(404, e.getCode());
        assertEquals("未找到", e.getMessage());
    }

    @Test
    void constructor_withCause_shouldWrapException() {
        Exception cause = new RuntimeException("原始错误");
        BusinessException e = new BusinessException(500, "服务器错误", cause);
        assertEquals(500, e.getCode());
        assertEquals(cause, e.getCause());
    }
}

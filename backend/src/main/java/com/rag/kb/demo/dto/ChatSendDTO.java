package com.rag.kb.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatSendDTO {

    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    @NotBlank(message = "消息内容不能为空")
    private String message;

    @Min(value = 1, message = "Top-K 最小为1")
    @Max(value = 20, message = "Top-K 最大为20")
    private Integer topK = 5;
}

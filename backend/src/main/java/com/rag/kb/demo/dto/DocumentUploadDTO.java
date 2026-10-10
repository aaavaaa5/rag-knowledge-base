package com.rag.kb.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class DocumentUploadDTO {

    @NotNull(message = "文件不能为空")
    private MultipartFile file;

    @NotNull(message = "切分长度不能为空")
    @Min(value = 200, message = "切分长度最小为200")
    @Max(value = 1500, message = "切分长度最大为1500")
    private Integer chunkSize;

    @NotNull(message = "重叠长度不能为空")
    @Min(value = 0, message = "重叠长度不能为负数")
    private Integer overlap;
}

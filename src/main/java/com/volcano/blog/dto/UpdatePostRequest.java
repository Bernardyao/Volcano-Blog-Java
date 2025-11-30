package com.volcano.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新文章请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新文章请求")
public class UpdatePostRequest {

    @Schema(description = "文章标题", example = "更新后的标题")
    @Size(min = 1, max = 200, message = "文章标题长度必须在1-200个字符之间")
    private String title;

    @Schema(description = "文章内容", example = "更新后的内容...")
    private String content;

    @Schema(description = "是否发布", example = "true")
    private Boolean published;
}

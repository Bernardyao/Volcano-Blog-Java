package com.volcano.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建文章请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "创建文章请求")
public class CreatePostRequest {

    @Schema(description = "文章标题", example = "我的第一篇博客", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "文章标题不能为空")
    @Size(min = 1, max = 200, message = "文章标题长度必须在1-200个字符之间")
    private String title;

    @Schema(description = "文章内容", example = "这是文章的正文内容...")
    private String content;

    @Schema(description = "是否发布", example = "false")
    @Builder.Default
    private boolean published = false;
}

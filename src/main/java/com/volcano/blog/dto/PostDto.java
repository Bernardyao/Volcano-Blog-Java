package com.volcano.blog.dto;

import com.volcano.blog.model.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 文章数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文章信息")
public class PostDto {

    @Schema(description = "文章ID", example = "1")
    private Long id;

    @Schema(description = "文章标题", example = "我的第一篇博客")
    private String title;

    @Schema(description = "文章内容", example = "这是文章的正文内容...")
    private String content;

    @Schema(description = "是否已发布", example = "true")
    private boolean published;

    @Schema(description = "作者ID", example = "1")
    private Long authorId;

    @Schema(description = "作者名称", example = "张三")
    private String authorName;

    @Schema(description = "作者邮箱", example = "user@example.com")
    private String authorEmail;

    @Schema(description = "创建时间")
    private Instant createdAt;

    @Schema(description = "更新时间")
    private Instant updatedAt;

    /**
     * 从 Post 实体创建 PostDto
     */
    public static PostDto fromEntity(Post post) {
        PostDto.PostDtoBuilder builder = PostDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .published(post.isPublished())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt());

        if (post.getAuthor() != null) {
            builder.authorId(post.getAuthor().getId())
                    .authorName(post.getAuthor().getName())
                    .authorEmail(post.getAuthor().getEmail());
        }

        return builder.build();
    }
}

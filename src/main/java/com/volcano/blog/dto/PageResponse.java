package com.volcano.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页响应")
public class PageResponse<T> {

    @Schema(description = "数据列表")
    private List<T> content;

    @Schema(description = "当前页码（从0开始）", example = "0")
    private int page;

    @Schema(description = "每页大小", example = "10")
    private int size;

    @Schema(description = "总元素数", example = "100")
    private long totalElements;

    @Schema(description = "总页数", example = "10")
    private int totalPages;

    @Schema(description = "是否为第一页", example = "true")
    private boolean first;

    @Schema(description = "是否为最后一页", example = "false")
    private boolean last;
}

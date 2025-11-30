package com.volcano.blog.controller;

import com.volcano.blog.annotation.AuditLog;
import com.volcano.blog.annotation.AuditLog.AuditAction;
import com.volcano.blog.dto.*;
import com.volcano.blog.security.JwtUserPrincipal;
import com.volcano.blog.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 文章控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "文章管理", description = "文章的增删改查 API")
public class PostController {

    private final PostService postService;

    /**
     * 创建文章
     */
    @Operation(summary = "创建文章", description = "创建一篇新文章")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "请求参数验证失败"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PostMapping
    @AuditLog(value = "创建文章", action = AuditAction.CREATE)
    public ResponseEntity<Map<String, Object>> createPost(
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        
        PostDto post = postService.createPost(principal.getUserId(), request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "success", true,
            "data", post,
            "message", "文章创建成功"
        ));
    }

    /**
     * 获取文章详情
     */
    @Operation(summary = "获取文章详情", description = "根据ID获取文章详情")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功"),
        @ApiResponse(responseCode = "404", description = "文章不存在")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPost(
            @Parameter(description = "文章ID") @PathVariable Long id) {
        
        PostDto post = postService.getPost(id);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", post
        ));
    }

    /**
     * 获取文章列表（分页）
     */
    @Operation(summary = "获取文章列表", description = "获取文章列表，支持分页")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPosts(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "是否只返回已发布文章") @RequestParam(defaultValue = "true") boolean publishedOnly) {
        
        PageResponse<PostDto> posts = postService.getPosts(page, size, publishedOnly);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", posts
        ));
    }

    /**
     * 获取当前用户的文章列表
     */
    @Operation(summary = "获取我的文章", description = "获取当前登录用户的文章列表")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyPosts(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        
        PageResponse<PostDto> posts = postService.getUserPosts(principal.getUserId(), page, size);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", posts
        ));
    }

    /**
     * 更新文章
     */
    @Operation(summary = "更新文章", description = "更新指定文章")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "请求参数验证失败"),
        @ApiResponse(responseCode = "401", description = "未授权"),
        @ApiResponse(responseCode = "403", description = "无权操作"),
        @ApiResponse(responseCode = "404", description = "文章不存在")
    })
    @PutMapping("/{id}")
    @AuditLog(value = "更新文章", action = AuditAction.UPDATE)
    public ResponseEntity<Map<String, Object>> updatePost(
            @Parameter(description = "文章ID") @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        
        PostDto post = postService.updatePost(id, principal.getUserId(), request);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", post,
            "message", "文章更新成功"
        ));
    }

    /**
     * 删除文章
     */
    @Operation(summary = "删除文章", description = "删除指定文章")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "401", description = "未授权"),
        @ApiResponse(responseCode = "403", description = "无权操作"),
        @ApiResponse(responseCode = "404", description = "文章不存在")
    })
    @DeleteMapping("/{id}")
    @AuditLog(value = "删除文章", action = AuditAction.DELETE)
    public ResponseEntity<Map<String, Object>> deletePost(
            @Parameter(description = "文章ID") @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        
        postService.deletePost(id, principal.getUserId(), principal.getRole());
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "文章删除成功"
        ));
    }

    /**
     * 发布/取消发布文章
     */
    @Operation(summary = "切换发布状态", description = "发布或取消发布文章")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "401", description = "未授权"),
        @ApiResponse(responseCode = "403", description = "无权操作"),
        @ApiResponse(responseCode = "404", description = "文章不存在")
    })
    @PatchMapping("/{id}/toggle-publish")
    @AuditLog(value = "切换文章发布状态", action = AuditAction.UPDATE)
    public ResponseEntity<Map<String, Object>> togglePublish(
            @Parameter(description = "文章ID") @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        
        PostDto post = postService.togglePublish(id, principal.getUserId());
        String message = post.isPublished() ? "文章已发布" : "文章已取消发布";
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", post,
            "message", message
        ));
    }
}

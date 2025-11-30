package com.volcano.blog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volcano.blog.dto.CreatePostRequest;
import com.volcano.blog.dto.PageResponse;
import com.volcano.blog.dto.PostDto;
import com.volcano.blog.dto.UpdatePostRequest;
import com.volcano.blog.exception.ResourceNotFoundException;
import com.volcano.blog.security.JwtTokenProvider;
import com.volcano.blog.security.JwtUserPrincipal;
import com.volcano.blog.service.PostService;
import com.volcano.blog.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PostController 集成测试
 */
@WebMvcTest(PostController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("文章控制器集成测试")
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostService postService;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private JwtUserPrincipal principal;
    private PostDto postDto;

    @BeforeEach
    void setUp() {
        when(rateLimitService.allowRequest(anyString())).thenReturn(true);

        principal = new JwtUserPrincipal(1L, "test@example.com", "USER");

        postDto = PostDto.builder()
                .id(1L)
                .title("Test Post")
                .content("Test content")
                .published(true)
                .authorId(1L)
                .authorName("Test User")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private void setupAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList())
        );
    }

    private void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/posts - 获取文章列表")
    void getAllPosts_ShouldReturnPagedPosts() throws Exception {
        // Given
        PageResponse<PostDto> pageResponse = PageResponse.<PostDto>builder()
                .content(List.of(postDto))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();
        when(postService.getPosts(0, 10, true)).thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/api/posts")
                        .param("page", "0")
                        .param("size", "10")
                        .param("publishedOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Test Post"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(postService, times(1)).getPosts(0, 10, true);
    }

    @Test
    @DisplayName("GET /api/posts/{id} - 获取单篇文章")
    void getPostById_ShouldReturnPost() throws Exception {
        // Given
        when(postService.getPost(1L)).thenReturn(postDto);

        // When & Then
        mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Test Post"))
                .andExpect(jsonPath("$.data.content").value("Test content"));

        verify(postService, times(1)).getPost(1L);
    }

    @Test
    @DisplayName("GET /api/posts/{id} - 文章不存在返回404")
    void getPostById_NotFound_ShouldReturn404() throws Exception {
        // Given
        when(postService.getPost(999L))
                .thenThrow(new ResourceNotFoundException("文章不存在"));

        // When & Then
        mockMvc.perform(get("/api/posts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

        verify(postService, times(1)).getPost(999L);
    }

    @Test
    @DisplayName("POST /api/posts - 创建文章成功")
    void createPost_ShouldReturnCreatedPost() throws Exception {
        // Given
        setupAuthentication();

        try {
            CreatePostRequest request = new CreatePostRequest();
            request.setTitle("New Post");
            request.setContent("New content");
            request.setPublished(true);

            when(postService.createPost(eq(1L), any(CreatePostRequest.class)))
                    .thenReturn(postDto);

            // When & Then
            mockMvc.perform(post("/api/posts")
                            .principal(principal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("文章创建成功"))
                    .andExpect(jsonPath("$.data.id").value(1));

            verify(postService, times(1)).createPost(eq(1L), any(CreatePostRequest.class));
        } finally {
            clearAuthentication();
        }
    }

    @Test
    @DisplayName("POST /api/posts - 标题为空返回400")
    void createPost_WithEmptyTitle_ShouldReturn400() throws Exception {
        // Given
        setupAuthentication();

        try {
            CreatePostRequest request = new CreatePostRequest();
            request.setTitle(""); // 空标题
            request.setContent("Content");

            // When & Then
            mockMvc.perform(post("/api/posts")
                            .principal(principal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

            verify(postService, never()).createPost(anyLong(), any());
        } finally {
            clearAuthentication();
        }
    }

    @Test
    @DisplayName("PUT /api/posts/{id} - 更新文章成功")
    void updatePost_ShouldReturnUpdatedPost() throws Exception {
        // Given
        setupAuthentication();

        try {
            UpdatePostRequest request = new UpdatePostRequest();
            request.setTitle("Updated Title");
            request.setContent("Updated content");

            PostDto updatedPost = PostDto.builder()
                    .id(1L)
                    .title("Updated Title")
                    .content("Updated content")
                    .published(true)
                    .authorId(1L)
                    .authorName("Test User")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(postService.updatePost(eq(1L), eq(1L), any(UpdatePostRequest.class)))
                    .thenReturn(updatedPost);

            // When & Then
            mockMvc.perform(put("/api/posts/1")
                            .principal(principal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("文章更新成功"))
                    .andExpect(jsonPath("$.data.title").value("Updated Title"));

            verify(postService, times(1)).updatePost(eq(1L), eq(1L), any(UpdatePostRequest.class));
        } finally {
            clearAuthentication();
        }
    }

    @Test
    @DisplayName("DELETE /api/posts/{id} - 删除文章成功")
    void deletePost_ShouldReturn200() throws Exception {
        // Given
        setupAuthentication();

        try {
            doNothing().when(postService).deletePost(1L, 1L, "USER");

            // When & Then
            mockMvc.perform(delete("/api/posts/1")
                            .principal(principal))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("文章删除成功"));

            verify(postService, times(1)).deletePost(1L, 1L, "USER");
        } finally {
            clearAuthentication();
        }
    }

    @Test
    @DisplayName("DELETE /api/posts/{id} - 文章不存在返回404")
    void deletePost_NotFound_ShouldReturn404() throws Exception {
        // Given
        setupAuthentication();

        try {
            doThrow(new ResourceNotFoundException("文章不存在"))
                    .when(postService).deletePost(999L, 1L, "USER");

            // When & Then
            mockMvc.perform(delete("/api/posts/999")
                            .principal(principal))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

            verify(postService, times(1)).deletePost(999L, 1L, "USER");
        } finally {
            clearAuthentication();
        }
    }
}

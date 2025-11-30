package com.volcano.blog.service;

import com.volcano.blog.dto.*;
import com.volcano.blog.exception.BusinessException;
import com.volcano.blog.exception.ResourceNotFoundException;
import com.volcano.blog.model.Post;
import com.volcano.blog.model.User;
import com.volcano.blog.repository.PostRepository;
import com.volcano.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文章服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * 创建文章
     */
    @Transactional
    public PostDto createPost(Long authorId, CreatePostRequest request) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .published(request.isPublished())
                .author(author)
                .build();

        Post savedPost = postRepository.save(post);
        log.info("Post created: id={}, title={}, authorId={}", savedPost.getId(), savedPost.getTitle(), authorId);

        return PostDto.fromEntity(savedPost);
    }

    /**
     * 获取文章详情
     */
    @Transactional(readOnly = true)
    public PostDto getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在: " + id));
        return PostDto.fromEntity(post);
    }

    /**
     * 获取文章列表（分页）
     */
    @Transactional(readOnly = true)
    public PageResponse<PostDto> getPosts(int page, int size, boolean publishedOnly) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<Post> postPage;
        if (publishedOnly) {
            postPage = postRepository.findByPublishedTrue(pageable);
        } else {
            postPage = postRepository.findAll(pageable);
        }

        List<PostDto> content = postPage.getContent().stream()
                .map(PostDto::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.<PostDto>builder()
                .content(content)
                .page(postPage.getNumber())
                .size(postPage.getSize())
                .totalElements(postPage.getTotalElements())
                .totalPages(postPage.getTotalPages())
                .first(postPage.isFirst())
                .last(postPage.isLast())
                .build();
    }

    /**
     * 获取用户的文章列表
     */
    @Transactional(readOnly = true)
    public PageResponse<PostDto> getUserPosts(Long authorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> postPage = postRepository.findByAuthorId(authorId, pageable);

        List<PostDto> content = postPage.getContent().stream()
                .map(PostDto::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.<PostDto>builder()
                .content(content)
                .page(postPage.getNumber())
                .size(postPage.getSize())
                .totalElements(postPage.getTotalElements())
                .totalPages(postPage.getTotalPages())
                .first(postPage.isFirst())
                .last(postPage.isLast())
                .build();
    }

    /**
     * 更新文章
     */
    @Transactional
    public PostDto updatePost(Long id, Long authorId, UpdatePostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在: " + id));

        // 检查是否是作者本人
        if (!post.getAuthor().getId().equals(authorId)) {
            throw new BusinessException("无权修改此文章");
        }

        // 更新字段
        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }
        if (request.getPublished() != null) {
            post.setPublished(request.getPublished());
        }

        Post updatedPost = postRepository.save(post);
        log.info("Post updated: id={}, authorId={}", id, authorId);

        return PostDto.fromEntity(updatedPost);
    }

    /**
     * 删除文章
     */
    @Transactional
    public void deletePost(Long id, Long authorId, String userRole) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在: " + id));

        // 检查是否是作者本人或管理员
        if (!post.getAuthor().getId().equals(authorId) && !"ADMIN".equals(userRole)) {
            throw new BusinessException("无权删除此文章");
        }

        postRepository.delete(post);
        log.info("Post deleted: id={}, by userId={}", id, authorId);
    }

    /**
     * 发布/取消发布文章
     */
    @Transactional
    public PostDto togglePublish(Long id, Long authorId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在: " + id));

        if (!post.getAuthor().getId().equals(authorId)) {
            throw new BusinessException("无权操作此文章");
        }

        post.setPublished(!post.isPublished());
        Post updatedPost = postRepository.save(post);
        log.info("Post publish toggled: id={}, published={}", id, updatedPost.isPublished());

        return PostDto.fromEntity(updatedPost);
    }
}

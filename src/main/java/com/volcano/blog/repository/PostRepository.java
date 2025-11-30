package com.volcano.blog.repository;

import com.volcano.blog.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    /**
     * 查询已发布的文章（分页）
     */
    Page<Post> findByPublishedTrue(Pageable pageable);
    
    /**
     * 查询指定作者的文章（分页）
     */
    Page<Post> findByAuthorId(Long authorId, Pageable pageable);
}

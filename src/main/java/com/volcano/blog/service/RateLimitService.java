package com.volcano.blog.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 限流服务
 * 使用 Bucket4j 实现令牌桶算法，防止暴力破解和 API 滥用
 * 使用 Caffeine Cache 自动清理过期的桶，防止内存泄漏
 */
@Slf4j
@Service
public class RateLimitService {

    // 使用 Caffeine Cache 存储令牌桶，自动过期清理
    private final Cache<String, Bucket> buckets;
    
    private final int capacity;
    private final int refillTokens;
    private final Duration refillDuration;

    public RateLimitService(
            @Value("${ratelimit.login.capacity:5}") int capacity,
            @Value("${ratelimit.login.refill-tokens:5}") int refillTokens,
            @Value("${ratelimit.login.refill-minutes:1}") int refillMinutes,
            @Value("${ratelimit.cache.expire-minutes:10}") int expireMinutes,
            @Value("${ratelimit.cache.max-size:10000}") int maxSize) {
        
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillDuration = Duration.ofMinutes(refillMinutes);
        
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(expireMinutes, TimeUnit.MINUTES)
                .maximumSize(maxSize)
                .removalListener((key, value, cause) -> 
                    log.debug("Rate limit bucket removed for client: {}, cause: {}", key, cause))
                .build();
        
        log.info("RateLimitService initialized: capacity={}, refill={}/{}, cache expire={}min, max={}",
                capacity, refillTokens, refillDuration, expireMinutes, maxSize);
    }

    /**
     * 检查是否允许请求
     * 
     * @param clientId 客户端标识（通常是 IP 地址）
     * @return true 如果允许请求，false 如果超过限流
     */
    public boolean allowRequest(String clientId) {
        Bucket bucket = buckets.get(clientId, this::createNewBucket);
        boolean consumed = bucket.tryConsume(1);
        
        if (!consumed) {
            log.warn("Rate limit exceeded for client: {}", clientId);
        }
        
        return consumed;
    }

    /**
     * 创建新的令牌桶
     */
    private Bucket createNewBucket(String clientId) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(refillTokens, refillDuration));
        Bucket bucket = Bucket.builder()
                .addLimit(limit)
                .build();
        
        log.debug("Created new rate limit bucket for client: {}", clientId);
        return bucket;
    }

    /**
     * 重置客户端的限流计数器（用于成功登录后）
     * 
     * @param clientId 客户端标识
     */
    public void resetLimit(String clientId) {
        buckets.invalidate(clientId);
        log.debug("Reset rate limit for client: {}", clientId);
    }

    /**
     * 获取当前缓存的桶数量（用于监控）
     */
    public long getBucketCount() {
        return buckets.estimatedSize();
    }

    /**
     * 清理所有桶（用于测试或维护）
     */
    public void clearAllBuckets() {
        long size = buckets.estimatedSize();
        buckets.invalidateAll();
        log.info("Cleared {} rate limit buckets", size);
    }
}

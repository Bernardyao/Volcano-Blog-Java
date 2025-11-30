package com.volcano.blog.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RateLimitService 单元测试
 */
@DisplayName("限流服务测试")
class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        // 使用默认配置创建服务: 5次/分钟, 10分钟过期, 最大10000个桶
        rateLimitService = new RateLimitService(5, 5, 1, 10, 10000);
    }

    @Test
    @DisplayName("应该允许在限制内的请求")
    void shouldAllowRequestsWithinLimit() {
        String clientId = "192.168.1.100";
        
        // 前5次请求应该被允许
        for (int i = 0; i < 5; i++) {
            boolean allowed = rateLimitService.allowRequest(clientId);
            assertThat(allowed)
                .as("Request %d should be allowed", i + 1)
                .isTrue();
        }
    }

    @Test
    @DisplayName("应该拒绝超出限制的请求")
    void shouldBlockRequestsExceedingLimit() {
        String clientId = "192.168.1.100";
        
        // 前5次请求应该被允许
        for (int i = 0; i < 5; i++) {
            rateLimitService.allowRequest(clientId);
        }
        
        // 第6次请求应该被拒绝
        boolean allowed = rateLimitService.allowRequest(clientId);
        assertThat(allowed)
            .as("6th request should be blocked")
            .isFalse();
    }

    @Test
    @DisplayName("不同客户端应该有独立的限流计数")
    void shouldHaveIndependentLimitsPerClient() {
        String client1 = "192.168.1.100";
        String client2 = "192.168.1.101";
        
        // 客户端1使用5次配额
        for (int i = 0; i < 5; i++) {
            rateLimitService.allowRequest(client1);
        }
        
        // 客户端1的第6次请求被拒绝
        assertThat(rateLimitService.allowRequest(client1)).isFalse();
        
        // 客户端2应该仍然可以请求
        assertThat(rateLimitService.allowRequest(client2)).isTrue();
    }

    @Test
    @DisplayName("重置限流后应该允许新请求")
    void shouldAllowRequestsAfterReset() {
        String clientId = "192.168.1.100";
        
        // 使用所有配额
        for (int i = 0; i < 5; i++) {
            rateLimitService.allowRequest(clientId);
        }
        
        // 验证被限流
        assertThat(rateLimitService.allowRequest(clientId)).isFalse();
        
        // 重置限流
        rateLimitService.resetLimit(clientId);
        
        // 应该可以再次请求
        assertThat(rateLimitService.allowRequest(clientId)).isTrue();
    }

    @Test
    @DisplayName("清理所有桶后应该能创建新的限流")
    void shouldCreateNewBucketsAfterClearing() {
        String clientId = "192.168.1.100";
        
        // 使用一些配额
        rateLimitService.allowRequest(clientId);
        rateLimitService.allowRequest(clientId);
        
        // 清理所有桶
        rateLimitService.clearAllBuckets();
        
        // 应该可以再次使用完整配额
        for (int i = 0; i < 5; i++) {
            boolean allowed = rateLimitService.allowRequest(clientId);
            assertThat(allowed).isTrue();
        }
    }

    @Test
    @DisplayName("应该能获取当前桶的数量")
    void shouldReturnBucketCount() {
        // 初始应该为0
        assertThat(rateLimitService.getBucketCount()).isEqualTo(0);
        
        // 创建几个客户端的桶
        rateLimitService.allowRequest("client1");
        rateLimitService.allowRequest("client2");
        rateLimitService.allowRequest("client3");
        
        // 应该有3个桶
        assertThat(rateLimitService.getBucketCount()).isEqualTo(3);
    }
}

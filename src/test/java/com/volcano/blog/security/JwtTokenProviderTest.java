package com.volcano.blog.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

/**
 * JwtTokenProvider 单元测试
 */
@DisplayName("JWT 令牌提供者测试")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final long validityInMs = 3600000L; // 1小时
    private final String base64Secret = Base64.getEncoder().encodeToString(
            "test-secret-key-for-jwt-testing-minimum-32-chars".getBytes()
    );

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(validityInMs);
        ReflectionTestUtils.setField(jwtTokenProvider, "secret", base64Secret);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("创建 JWT - 应该包含正确的声明")
    void createToken_ShouldContainCorrectClaims() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        String role = "USER";

        // When
        String token = jwtTokenProvider.createToken(userId, email, role);

        // Then
        assertThat(token).isNotNull().isNotEmpty();
        
        // 解析令牌验证声明
        Jws<Claims> parsedToken = jwtTokenProvider.parseToken(token);
        Claims claims = parsedToken.getBody();
        
        assertThat(claims.get("userId", Long.class)).isEqualTo(userId);
        assertThat(claims.get("email", String.class)).isEqualTo(email);
        assertThat(claims.get("role", String.class)).isEqualTo(role);
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    @DisplayName("解析有效的 JWT - 应该成功")
    void parseToken_WithValidToken_ShouldSucceed() {
        // Given
        String token = jwtTokenProvider.createToken(1L, "test@example.com", "USER");

        // When
        Jws<Claims> parsedToken = jwtTokenProvider.parseToken(token);

        // Then
        assertThat(parsedToken).isNotNull();
        assertThat(parsedToken.getBody()).isNotNull();
    }

    @Test
    @DisplayName("解析无效的 JWT - 应该抛出异常")
    void parseToken_WithInvalidToken_ShouldThrowException() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.parseToken(invalidToken))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("解析格式错误的 JWT - 应该抛出异常")
    void parseToken_WithMalformedToken_ShouldThrowException() {
        // Given
        String malformedToken = "not-a-jwt-at-all";

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.parseToken(malformedToken))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("初始化 - 密钥太短应该抛出异常")
    void init_WithShortSecret_ShouldThrowException() {
        // Given
        JwtTokenProvider shortSecretProvider = new JwtTokenProvider(validityInMs);
        ReflectionTestUtils.setField(shortSecretProvider, "secret", "short");

        // When & Then
        assertThatThrownBy(() -> shortSecretProvider.init())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 32 characters");
    }

    @Test
    @DisplayName("初始化 - null 密钥应该抛出异常")
    void init_WithNullSecret_ShouldThrowException() {
        // Given
        JwtTokenProvider nullSecretProvider = new JwtTokenProvider(validityInMs);
        ReflectionTestUtils.setField(nullSecretProvider, "secret", null);

        // When & Then
        assertThatThrownBy(() -> nullSecretProvider.init())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("创建 JWT - 应该在指定时间后过期")
    void createToken_ShouldExpireAfterSpecifiedTime() throws InterruptedException {
        // Given: 创建一个只有 1 秒有效期的提供者
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(1000L); // 1秒
        ReflectionTestUtils.setField(shortLivedProvider, "secret", base64Secret);
        shortLivedProvider.init();

        String token = shortLivedProvider.createToken(1L, "test@example.com", "USER");

        // When: 等待令牌过期
        Thread.sleep(1100); // 等待超过 1 秒

        // Then: 解析过期的令牌应该抛出异常
        assertThatThrownBy(() -> shortLivedProvider.parseToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("创建 JWT - 使用非 Base64 密钥应该也能工作")
    void createToken_WithNonBase64Secret_ShouldWork() {
        // Given: 使用一个足够长但不是 Base64 编码的密钥
        JwtTokenProvider nonBase64Provider = new JwtTokenProvider(validityInMs);
        String plainSecret = "this-is-a-plain-secret-key-not-base64-encoded-but-long-enough";
        ReflectionTestUtils.setField(nonBase64Provider, "secret", plainSecret);
        nonBase64Provider.init();

        // When
        String token = nonBase64Provider.createToken(1L, "test@example.com", "USER");

        // Then
        assertThat(token).isNotNull().isNotEmpty();
        Jws<Claims> parsedToken = nonBase64Provider.parseToken(token);
        assertThat(parsedToken.getBody().get("email", String.class)).isEqualTo("test@example.com");
    }
}

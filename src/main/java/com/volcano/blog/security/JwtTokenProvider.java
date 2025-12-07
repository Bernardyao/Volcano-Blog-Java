package com.volcano.blog.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

/**
 * JWT 令牌提供者
 * 负责 JWT 的生成、解析和验证
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private Key key;
    private final long validityInMs;
    
    @Value("${jwt.secret}")
    private String secret;

    public JwtTokenProvider(@Value("${jwt.expiration}") long expirationMs) {
        this.validityInMs = expirationMs;
    }

    /**
     * 初始化密钥
     * 在依赖注入完成后执行，将 Base64 编码的密钥字符串转换为 Key 对象
     */
    @PostConstruct
    public void init() {
        // 验证密钥长度（HS256 需要至少 256 位，即 32 字节）
        if (secret == null || secret.length() < 32) {
            log.error("JWT secret is too short. Minimum length is 32 characters.");
            throw new IllegalArgumentException("JWT secret must be at least 32 characters long");
        }
        
        try {
            // 尝试将密钥作为 Base64 解码
            byte[] keyBytes = Base64.getDecoder().decode(secret);
            this.key = Keys.hmacShaKeyFor(keyBytes);
            log.info("JWT secret initialized successfully using Base64 decoding");
        } catch (IllegalArgumentException e) {
            // 如果不是 Base64，直接使用 UTF-8 编码的字节
            log.warn("JWT secret is not Base64 encoded, using UTF-8 bytes directly");
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 创建 JWT 令牌
     */
    public String createToken(Long userId, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInMs);

        String token = Jwts.builder()
                .claim("userId", userId)
                .claim("email", email)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        
        log.debug("Created JWT token for user: {} (expires at: {})", email, expiry);
        return token;
    }

    /**
     * 解析和验证 JWT 令牌
     */
    public Jws<Claims> parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.error("JWT token is malformed: {}", e.getMessage());
            throw e;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("JWT signature validation failed: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("JWT token is invalid: {}", e.getMessage());
            throw e;
        }
    }
}

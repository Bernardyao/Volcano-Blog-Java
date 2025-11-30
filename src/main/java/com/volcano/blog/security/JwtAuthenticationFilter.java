package com.volcano.blog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 * 拦截每个请求，验证 JWT Token 并设置 Spring Security 上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (StringUtils.hasText(token)) {
                var claims = jwtTokenProvider.parseToken(token);
                var body = claims.getBody();

                Long userId = body.get("userId", Long.class);
                String email = body.get("email", String.class);
                String role = body.get("role", String.class);

                // 创建认证信息
                var authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + (role != null ? role.toUpperCase() : "USER"))
                );

                var authentication = new UsernamePasswordAuthenticationToken(
                        new JwtUserPrincipal(userId, email, role),
                        null,
                        authorities
                );

                // 设置到 Security 上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user: {} with role: {}", email, role);
            }
        } catch (Exception e) {
            log.debug("JWT authentication failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 JWT Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 不需要过滤的路径（可选优化）
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/health") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/api/test") ||
               path.equals("/");
    }
}

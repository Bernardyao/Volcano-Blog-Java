package com.volcano.blog.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

/**
 * JWT 用户主体
 * 存储从 JWT Token 中解析出的用户信息
 */
@Getter
@AllArgsConstructor
public class JwtUserPrincipal implements Principal {
    
    private final Long userId;
    private final String email;
    private final String role;

    @Override
    public String getName() {
        return email;
    }
}

package com.volcano.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 DTO
 * 使用 UserDto 而不是 User 实体，避免泄露密码等敏感信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginResponse {
    
    @Schema(description = "用户信息")
    private UserDto user;
    
    @Schema(description = "JWT 访问令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
    
    @Schema(description = "令牌过期时间（毫秒）", example = "604800000")
    private long expiresIn;
}

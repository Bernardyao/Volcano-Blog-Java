package com.volcano.blog.dto;

import com.volcano.blog.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 用户数据传输对象
 * 用于向前端返回用户信息时隐藏敏感字段（如密码）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户信息")
public class UserDto {
    
    @Schema(description = "用户ID", example = "1")
    private Long id;
    
    @Schema(description = "用户邮箱", example = "user@example.com")
    private String email;
    
    @Schema(description = "用户名称", example = "张三")
    private String name;
    
    @Schema(description = "用户角色", example = "admin")
    private String role;
    
    @Schema(description = "创建时间")
    private Instant createdAt;
    
    @Schema(description = "更新时间")
    private Instant updatedAt;

    /**
     * 从 User 实体创建 UserDto，过滤掉密码等敏感信息
     */
    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

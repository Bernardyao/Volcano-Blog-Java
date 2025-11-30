package com.volcano.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注册请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户注册请求")
public class RegisterRequest {

    @Schema(description = "用户邮箱", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Schema(description = "用户密码", example = "Password123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6-50个字符之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须包含字母和数字")
    private String password;

    @Schema(description = "确认密码", example = "Password123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @Schema(description = "用户名称", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名称不能为空")
    @Size(min = 2, max = 50, message = "用户名称长度必须在2-50个字符之间")
    private String name;
}

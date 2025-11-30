package com.volcano.blog.controller;

import com.volcano.blog.annotation.AuditLog;
import com.volcano.blog.annotation.AuditLog.AuditAction;
import com.volcano.blog.dto.LoginRequest;
import com.volcano.blog.dto.LoginResponse;
import com.volcano.blog.dto.RegisterRequest;
import com.volcano.blog.dto.UserDto;
import com.volcano.blog.security.JwtUserPrincipal;
import com.volcano.blog.service.AuthService;
import com.volcano.blog.service.RateLimitService;
import com.volcano.blog.util.LogUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 * 处理用户登录、注销等认证相关的 HTTP 请求
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户认证相关的 API")
public class AuthController {

    private final AuthService authService;
    private final RateLimitService rateLimitService;

    public AuthController(AuthService authService, RateLimitService rateLimitService) {
        this.authService = authService;
        this.rateLimitService = rateLimitService;
    }

    /**
     * 用户登录
     * @param request 登录请求，包含邮箱和密码
     * @return 成功时返回用户信息和 JWT；失败时由全局异常处理器处理
     */
    @Operation(summary = "用户登录", description = "使用邮箱和密码进行登录认证")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "登录成功",
                content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "用户名或密码错误"),
        @ApiResponse(responseCode = "400", description = "请求参数验证失败"),
        @ApiResponse(responseCode = "429", description = "请求过于频繁，请稍后再试")
    })
    @PostMapping("/login")
    @AuditLog(value = "用户登录", action = AuditAction.LOGIN)
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        
        // 获取客户端 IP 地址
        String clientIp = getClientIp(httpRequest);
        log.info("Received login request for email: {} from IP: {}", 
                LogUtils.maskEmail(request.getEmail()), LogUtils.maskIp(clientIp));
        
        // 检查限流
        if (!rateLimitService.allowRequest(clientIp)) {
            log.warn("Rate limit exceeded for IP: {}", LogUtils.maskIp(clientIp));
            return ResponseEntity.status(429).body(Map.of(
                "success", false,
                "message", "请求过于频繁，请稍后再试",
                "error", "RATE_LIMIT_EXCEEDED"
            ));
        }
        
        try {
            LoginResponse response = authService.login(request);
            
            // 登录成功后重置限流计数器
            rateLimitService.resetLimit(clientIp);
            log.info("Login successful for user: {}", LogUtils.maskEmail(request.getEmail()));
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response,
                "message", "登录成功"
            ));
        } catch (Exception e) {
            // 登录失败，限流保持生效
            log.warn("Login failed for email: {} from IP: {}", 
                    LogUtils.maskEmail(request.getEmail()), LogUtils.maskIp(clientIp));
            throw e;
        }
    }
    
    /**
     * 获取客户端真实 IP 地址
     * 考虑代理和负载均衡的情况
     */
    private String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果有多个 IP（经过多层代理），取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 获取当前登录用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "根据 JWT 令牌获取当前登录用户的信息")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取用户信息"),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录")
    })
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal JwtUserPrincipal principal) {
        log.info("Received request for current user info, userId: {}", principal.getUserId());
        
        UserDto user = authService.getCurrentUser(principal.getUserId());
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", user
        ));
    }

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "创建新用户账号")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "注册成功"),
        @ApiResponse(responseCode = "400", description = "请求参数验证失败或邮箱已被注册"),
        @ApiResponse(responseCode = "429", description = "请求过于频繁，请稍后再试")
    })
    @PostMapping("/register")
    @AuditLog(value = "用户注册", action = AuditAction.CREATE)
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        
        String clientIp = getClientIp(httpRequest);
        log.info("Received register request for email: {} from IP: {}", 
                LogUtils.maskEmail(request.getEmail()), LogUtils.maskIp(clientIp));
        
        // 检查限流
        if (!rateLimitService.allowRequest("register:" + clientIp)) {
            log.warn("Rate limit exceeded for registration from IP: {}", LogUtils.maskIp(clientIp));
            return ResponseEntity.status(429).body(Map.of(
                "success", false,
                "message", "请求过于频繁，请稍后再试",
                "error", "RATE_LIMIT_EXCEEDED"
            ));
        }
        
        UserDto user = authService.register(request);
        log.info("Registration successful for user: {}", LogUtils.maskEmail(request.getEmail()));
        
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "success", true,
            "data", user,
            "message", "注册成功"
        ));
    }
}

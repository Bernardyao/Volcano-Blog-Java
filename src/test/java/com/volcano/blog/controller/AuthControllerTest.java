package com.volcano.blog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volcano.blog.dto.LoginRequest;
import com.volcano.blog.dto.LoginResponse;
import com.volcano.blog.dto.RegisterRequest;
import com.volcano.blog.dto.UserDto;
import com.volcano.blog.exception.BusinessException;
import com.volcano.blog.security.JwtTokenProvider;
import com.volcano.blog.security.JwtUserPrincipal;
import com.volcano.blog.service.AuthService;
import com.volcano.blog.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 集成测试
 * 测试 HTTP 请求和响应
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // 禁用 Spring Security 过滤器
@DisplayName("认证控制器集成测试")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private LoginRequest validLoginRequest;
    private LoginResponse loginResponse;

    @BeforeEach
    void setUp() {
        // 默认允许所有请求通过限流检查
        when(rateLimitService.allowRequest(anyString())).thenReturn(true);

        validLoginRequest = new LoginRequest();
        validLoginRequest.setEmail("test@example.com");
        validLoginRequest.setPassword("password123");

        UserDto userDto = UserDto.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .role("USER")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        loginResponse = LoginResponse.builder()
                .user(userDto)
                .token("test.jwt.token")
                .expiresIn(604800000L)
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/login - 登录成功")
    void login_WithValidCredentials_ShouldReturn200() throws Exception {
        // Given
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.token").value("test.jwt.token"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.user.id").value(1))
                .andExpect(jsonPath("$.data.expiresIn").value(604800000L));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - 用户不存在，返回 401")
    void login_WithNonExistentUser_ShouldReturn401() throws Exception {
        // Given
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UsernameNotFoundException("用户名或密码错误"));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - 密码错误，返回 401")
    void login_WithInvalidPassword_ShouldReturn401() throws Exception {
        // Given
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("用户名或密码错误"));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_FAILED"));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - 邮箱格式错误，返回 400")
    void login_WithInvalidEmailFormat_ShouldReturn400() throws Exception {
        // Given
        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setEmail("invalid-email"); // 无效的邮箱格式
        invalidRequest.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.email").exists());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - 邮箱为空，返回 400")
    void login_WithEmptyEmail_ShouldReturn400() throws Exception {
        // Given
        LoginRequest emptyEmailRequest = new LoginRequest();
        emptyEmailRequest.setEmail("");
        emptyEmailRequest.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyEmailRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - 密码为空，返回 400")
    void login_WithEmptyPassword_ShouldReturn400() throws Exception {
        // Given
        LoginRequest emptyPasswordRequest = new LoginRequest();
        emptyPasswordRequest.setEmail("test@example.com");
        emptyPasswordRequest.setPassword("");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyPasswordRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - Content-Type 错误，返回 415")
    void login_WithWrongContentType_ShouldReturn415() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("invalid content"))
                .andExpect(status().isUnsupportedMediaType());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("GET /api/auth/me - 已认证用户获取当前用户信息")
    void getCurrentUser_WithAuthenticatedUser_ShouldReturn200() throws Exception {
        // Given
        Long userId = 1L;
        UserDto userDto = UserDto.builder()
                .id(userId)
                .email("test@example.com")
                .name("Test User")
                .role("USER")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        when(authService.getCurrentUser(eq(userId))).thenReturn(userDto);
        
        // 模拟已认证用户
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "test@example.com", "USER");
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList())
        );

        try {
            // When & Then
            mockMvc.perform(get("/api/auth/me")
                            .principal(principal))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(userId))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.name").value("Test User"));
            
            verify(authService, times(1)).getCurrentUser(eq(userId));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("POST /api/auth/register - 注册成功")
    void register_WithValidRequest_ShouldReturn201() throws Exception {
        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("Password123!");
        registerRequest.setConfirmPassword("Password123!");
        registerRequest.setName("New User");
        
        UserDto userDto = UserDto.builder()
                .id(2L)
                .email("newuser@example.com")
                .name("New User")
                .role("USER")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        when(authService.register(any(RegisterRequest.class))).thenReturn(userDto);

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("注册成功"))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.data.name").value("New User"));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/register - 邮箱已存在，返回 400")
    void register_WithExistingEmail_ShouldReturn400() throws Exception {
        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("existing@example.com");
        registerRequest.setPassword("Password123!");
        registerRequest.setConfirmPassword("Password123!");
        registerRequest.setName("New User");
        
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new BusinessException("DUPLICATE_EMAIL", "该邮箱已被注册"));

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").value("该邮箱已被注册"));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/register - 密码格式不正确，返回 400")
    void register_WithInvalidPassword_ShouldReturn400() throws Exception {
        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("weak"); // 弱密码
        registerRequest.setConfirmPassword("weak");
        registerRequest.setName("New User");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        verify(authService, never()).register(any(RegisterRequest.class));
    }
}

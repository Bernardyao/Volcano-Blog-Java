package com.volcano.blog.service;

import com.volcano.blog.dto.LoginRequest;
import com.volcano.blog.dto.LoginResponse;
import com.volcano.blog.dto.UserDto;
import com.volcano.blog.model.User;
import com.volcano.blog.repository.UserRepository;
import com.volcano.blog.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试
 * 使用 Mockito 模拟依赖项
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("认证服务测试")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest validLoginRequest;
    private final String testEmail = "test@example.com";
    private final String testPassword = "password123";
    private final String encodedPassword = "$2a$10$encodedPasswordHash";
    private final String testToken = "test.jwt.token";

    @BeforeEach
    void setUp() {
        // 设置 jwtExpiration 字段的值
        ReflectionTestUtils.setField(authService, "jwtExpiration", 604800000L);

        // 准备测试数据
        testUser = User.builder()
                .id(1L)
                .email(testEmail)
                .name("Test User")
                .password(encodedPassword)
                .role("USER")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        validLoginRequest = new LoginRequest();
        validLoginRequest.setEmail(testEmail);
        validLoginRequest.setPassword(testPassword);
    }

    @Test
    @DisplayName("登录成功 - 使用有效凭证")
    void login_WithValidCredentials_ShouldReturnLoginResponse() {
        // Given: 模拟用户存在且密码匹配
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testPassword, encodedPassword)).thenReturn(true);
        when(jwtTokenProvider.createToken(anyLong(), anyString(), anyString())).thenReturn(testToken);

        // When: 执行登录
        LoginResponse response = authService.login(validLoginRequest);

        // Then: 验证返回结果
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(testToken);
        assertThat(response.getExpiresIn()).isEqualTo(604800000L);
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(response.getUser().getEmail()).isEqualTo(testUser.getEmail());
        assertThat(response.getUser().getName()).isEqualTo(testUser.getName());

        // 验证方法调用次数
        verify(userRepository, times(1)).findByEmail(testEmail);
        verify(passwordEncoder, times(1)).matches(testPassword, encodedPassword);
        verify(jwtTokenProvider, times(1)).createToken(
                testUser.getId(),
                testUser.getEmail(),
                testUser.getRole()
        );
    }

    @Test
    @DisplayName("登录失败 - 用户不存在")
    void login_WithNonExistentUser_ShouldThrowException() {
        // Given: 模拟用户不存在
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

        // When & Then: 执行登录应该抛出异常
        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("用户名或密码错误");

        // 验证只调用了 findByEmail，没有继续执行
        verify(userRepository, times(1)).findByEmail(testEmail);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenProvider, never()).createToken(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void login_WithInvalidPassword_ShouldThrowException() {
        // Given: 模拟用户存在但密码不匹配
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testPassword, encodedPassword)).thenReturn(false);

        // When & Then: 执行登录应该抛出异常
        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("用户名或密码错误");

        // 验证调用链
        verify(userRepository, times(1)).findByEmail(testEmail);
        verify(passwordEncoder, times(1)).matches(testPassword, encodedPassword);
        verify(jwtTokenProvider, never()).createToken(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("登录成功 - 验证 UserDto 不包含密码")
    void login_ShouldReturnUserDtoWithoutPassword() {
        // Given
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testPassword, encodedPassword)).thenReturn(true);
        when(jwtTokenProvider.createToken(anyLong(), anyString(), anyString())).thenReturn(testToken);

        // When
        LoginResponse response = authService.login(validLoginRequest);

        // Then: 确保返回的 UserDto 中不包含密码
        UserDto userDto = response.getUser();
        assertThat(userDto).isNotNull();
        
        // UserDto 应该只包含非敏感字段
        assertThat(userDto.getId()).isNotNull();
        assertThat(userDto.getEmail()).isNotNull();
        assertThat(userDto.getName()).isNotNull();
        assertThat(userDto.getRole()).isNotNull();
        
        // 通过反射确认 UserDto 类没有 password 字段
        assertThat(UserDto.class.getDeclaredFields())
                .noneMatch(field -> field.getName().equals("password"));
    }

    @Test
    @DisplayName("登录 - 验证空邮箱处理")
    void login_WithEmptyEmail_ShouldThrowException() {
        // Given
        LoginRequest emptyEmailRequest = new LoginRequest();
        emptyEmailRequest.setEmail("");
        emptyEmailRequest.setPassword(testPassword);

        when(userRepository.findByEmail("")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(emptyEmailRequest))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(userRepository, times(1)).findByEmail("");
    }
}

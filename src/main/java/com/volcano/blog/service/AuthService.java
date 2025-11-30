package com.volcano.blog.service;

import com.volcano.blog.dto.LoginRequest;
import com.volcano.blog.dto.LoginResponse;
import com.volcano.blog.dto.RegisterRequest;
import com.volcano.blog.dto.UserDto;
import com.volcano.blog.exception.BusinessException;
import com.volcano.blog.exception.ResourceNotFoundException;
import com.volcano.blog.model.User;
import com.volcano.blog.repository.UserRepository;
import com.volcano.blog.security.JwtTokenProvider;
import com.volcano.blog.util.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public AuthService(UserRepository userRepository, JwtTokenProvider jwtTokenProvider, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", LogUtils.maskEmail(request.getEmail()));
        
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> {
                log.warn("Login failed: User not found for email: {}", LogUtils.maskEmail(request.getEmail()));
                return new UsernameNotFoundException("用户名或密码错误");
            });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: Invalid password for email: {}", LogUtils.maskEmail(request.getEmail()));
            throw new BadCredentialsException("用户名或密码错误");
        }

        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail(), user.getRole());
        
        log.info("Login successful for user: {} (id: {})", LogUtils.maskEmail(user.getEmail()), user.getId());
        
        return LoginResponse.builder()
                .user(UserDto.fromEntity(user))
                .token(token)
                .expiresIn(jwtExpiration)
                .build();
    }

    /**
     * 用户注册
     */
    @Transactional
    public UserDto register(RegisterRequest request) {
        log.info("Register attempt for email: {}", LogUtils.maskEmail(request.getEmail()));
        
        // 检查密码确认
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("PASSWORD_MISMATCH", "两次输入的密码不一致");
        }
        
        // 检查邮箱是否已被注册
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Registration failed: Email already exists: {}", LogUtils.maskEmail(request.getEmail()));
            throw new BusinessException("DUPLICATE_EMAIL", "该邮箱已被注册");
        }
        
        // 创建用户
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role("USER")
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("Registration successful for user: {} (id: {})", LogUtils.maskEmail(savedUser.getEmail()), savedUser.getId());
        
        return UserDto.fromEntity(savedUser);
    }

    /**
     * 获取当前用户信息
     */
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        return UserDto.fromEntity(user);
    }
}

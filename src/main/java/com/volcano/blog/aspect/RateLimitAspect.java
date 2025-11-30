package com.volcano.blog.aspect;

import com.volcano.blog.annotation.RateLimit;
import com.volcano.blog.exception.BusinessException;
import com.volcano.blog.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 速率限制切面
 * 拦截带有 @RateLimit 注解的方法，进行请求限流
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimitService rateLimitService;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        
        HttpServletRequest request = attributes.getRequest();
        
        // 构建限流 key
        String key = buildRateLimitKey(joinPoint, rateLimit, request);
        
        // 检查是否允许请求
        if (!rateLimitService.allowRequest(key)) {
            log.warn("Rate limit exceeded for key: {}", key);
            throw new BusinessException(rateLimit.message());
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * 构建限流 key
     */
    private String buildRateLimitKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit, HttpServletRequest request) {
        String prefix = rateLimit.key();
        
        // 如果没有指定 key，使用方法签名 + IP
        if (prefix.isEmpty()) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            prefix = signature.getDeclaringTypeName() + "." + signature.getName();
        }
        
        String clientIp = getClientIp(request);
        return prefix + ":" + clientIp;
    }
    
    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

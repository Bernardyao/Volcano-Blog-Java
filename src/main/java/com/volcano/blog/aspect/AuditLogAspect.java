package com.volcano.blog.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volcano.blog.annotation.AuditLog;
import com.volcano.blog.util.LogUtils;
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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 审计日志切面
 * 拦截带有 @AuditLog 注解的方法，记录操作日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final ObjectMapper objectMapper;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        
        // 构建审计日志
        Map<String, Object> auditData = new LinkedHashMap<>();
        auditData.put("timestamp", Instant.now().toString());
        auditData.put("action", auditLog.action().name());
        auditData.put("description", auditLog.value());
        auditData.put("method", joinPoint.getSignature().toShortString());
        
        if (request != null) {
            auditData.put("ip", LogUtils.maskIp(getClientIp(request)));
            auditData.put("uri", request.getRequestURI());
            auditData.put("httpMethod", request.getMethod());
            auditData.put("userAgent", request.getHeader("User-Agent"));
        }
        
        // 记录请求参数（脱敏处理）
        if (auditLog.logParams()) {
            try {
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > 0) {
                    // 过滤掉 HttpServletRequest/Response 等不可序列化对象
                    Object[] loggableArgs = filterLoggableArgs(args);
                    if (loggableArgs.length > 0) {
                        String params = objectMapper.writeValueAsString(loggableArgs);
                        // 脱敏敏感字段
                        params = maskSensitiveData(params);
                        auditData.put("params", params);
                    }
                }
            } catch (Exception e) {
                auditData.put("params", "[serialization error]");
            }
        }
        
        Object result = null;
        Throwable exception = null;
        
        try {
            result = joinPoint.proceed();
            auditData.put("status", "SUCCESS");
            
            // 记录返回结果
            if (auditLog.logResult() && result != null) {
                try {
                    String resultStr = objectMapper.writeValueAsString(result);
                    // 截断过长的结果
                    if (resultStr.length() > 1000) {
                        resultStr = resultStr.substring(0, 1000) + "...[truncated]";
                    }
                    auditData.put("result", resultStr);
                } catch (Exception e) {
                    auditData.put("result", "[serialization error]");
                }
            }
            
            return result;
        } catch (Throwable e) {
            exception = e;
            auditData.put("status", "FAILED");
            auditData.put("error", e.getMessage());
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            auditData.put("duration", duration + "ms");
            
            // 输出审计日志
            if (exception != null) {
                log.warn("AUDIT: {}", objectMapper.writeValueAsString(auditData));
            } else {
                log.info("AUDIT: {}", objectMapper.writeValueAsString(auditData));
            }
        }
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
        // 多级代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * 过滤掉不可序列化的参数
     */
    private Object[] filterLoggableArgs(Object[] args) {
        return java.util.Arrays.stream(args)
                .filter(arg -> arg != null)
                .filter(arg -> !(arg instanceof HttpServletRequest))
                .filter(arg -> !(arg instanceof jakarta.servlet.http.HttpServletResponse))
                .filter(arg -> !(arg instanceof org.springframework.web.multipart.MultipartFile))
                .toArray();
    }
    
    /**
     * 脱敏敏感数据
     */
    private String maskSensitiveData(String data) {
        // 脱敏密码字段
        data = data.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"");
        data = data.replaceAll("\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"***\"");
        data = data.replaceAll("\"secret\"\\s*:\\s*\"[^\"]*\"", "\"secret\":\"***\"");
        return data;
    }
}

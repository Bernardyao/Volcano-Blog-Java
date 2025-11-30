package com.volcano.blog.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理应用中抛出的各类异常，并返回规范的错误响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理认证失败异常（用户名不存在或密码错误）
     */
    @ExceptionHandler({UsernameNotFoundException.class, BadCredentialsException.class})
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(Exception ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return buildErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "AUTHENTICATION_FAILED",
            "用户名或密码错误",
            null
        );
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation failed: {}", errors);
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "请求参数校验失败",
            errors
        );
    }

    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            ex.getMessage(),
            null
        );
    }

    /**
     * 处理业务逻辑异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            ex.getErrorCode(),
            ex.getMessage(),
            null
        );
    }

    /**
     * 处理不支持的媒体类型异常
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported media type: {}", ex.getContentType());
        return buildErrorResponse(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "UNSUPPORTED_MEDIA_TYPE",
            "不支持的内容类型: " + ex.getContentType(),
            null
        );
    }

    /**
     * 处理请求体解析异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "MALFORMED_REQUEST",
            "请求体格式错误，请检查 JSON 格式",
            null
        );
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "MISSING_PARAMETER",
            "缺少必需的请求参数: " + ex.getParameterName(),
            null
        );
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("Argument type mismatch: {} = {}", ex.getName(), ex.getValue());
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "TYPE_MISMATCH",
            "参数类型不正确: " + ex.getName(),
            null
        );
    }

    /**
     * 处理不支持的请求方法异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {}", ex.getMethod());
        return buildErrorResponse(
            HttpStatus.METHOD_NOT_ALLOWED,
            "METHOD_NOT_ALLOWED",
            "不支持的请求方法: " + ex.getMethod(),
            null
        );
    }

    /**
     * 处理找不到处理器异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        log.warn("No handler found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return buildErrorResponse(
            HttpStatus.NOT_FOUND,
            "ENDPOINT_NOT_FOUND",
            "接口不存在: " + ex.getRequestURL(),
            null
        );
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_ERROR",
            "服务器内部错误，请稍后重试",
            null
        );
    }

    /**
     * 构建统一的错误响应格式
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String errorCode,
            String message,
            Object details) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", errorCode);
        response.put("message", message);
        response.put("timestamp", Instant.now().toString());
        
        if (details != null) {
            response.put("details", details);
        }
        
        return ResponseEntity.status(status).body(response);
    }
}

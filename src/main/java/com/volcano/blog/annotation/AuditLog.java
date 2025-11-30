package com.volcano.blog.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解
 * 用于标记需要记录操作日志的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    
    /**
     * 操作描述
     */
    String value() default "";
    
    /**
     * 操作类型
     */
    AuditAction action() default AuditAction.OTHER;
    
    /**
     * 是否记录请求参数
     */
    boolean logParams() default true;
    
    /**
     * 是否记录返回结果
     */
    boolean logResult() default false;
    
    /**
     * 审计操作类型枚举
     */
    enum AuditAction {
        LOGIN,
        LOGOUT,
        CREATE,
        UPDATE,
        DELETE,
        QUERY,
        EXPORT,
        IMPORT,
        OTHER
    }
}

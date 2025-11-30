package com.volcano.blog.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 速率限制注解
 * 用于标记需要进行请求限流的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * 限流的键前缀，默认使用客户端IP
     */
    String key() default "";
    
    /**
     * 时间窗口内允许的最大请求数
     */
    int limit() default 10;
    
    /**
     * 时间窗口（秒）
     */
    int window() default 60;
    
    /**
     * 限流提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
}

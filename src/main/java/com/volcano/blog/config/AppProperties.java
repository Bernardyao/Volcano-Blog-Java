package com.volcano.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * 应用配置属性
 * 提供类型安全的配置访问和校验
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();

    /**
     * JWT 配置
     */
    @Data
    public static class Jwt {
        /**
         * JWT 密钥（生产环境必须通过环境变量设置）
         */
        @NotBlank(message = "JWT secret must not be blank")
        private String secret;

        /**
         * JWT 过期时间（毫秒）
         */
        @Positive(message = "JWT expiration must be positive")
        private long expiration = 604800000L; // 默认 7 天

        /**
         * JWT 签发者
         */
        private String issuer = "volcano-blog";
    }

    /**
     * CORS 配置
     */
    @Data
    public static class Cors {
        /**
         * 允许的源
         */
        @NotEmpty(message = "CORS allowed origins must not be empty")
        private List<String> allowedOrigins = List.of("http://localhost:5173");

        /**
         * 允许的 HTTP 方法
         */
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");

        /**
         * 允许的请求头
         */
        private List<String> allowedHeaders = List.of("*");

        /**
         * 是否允许携带凭证
         */
        private boolean allowCredentials = true;

        /**
         * 预检请求缓存时间（秒）
         */
        @Positive
        private long maxAge = 3600L;
    }
}

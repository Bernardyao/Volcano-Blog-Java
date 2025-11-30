package com.volcano.blog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) 配置
 * 访问 /swagger-ui.html 查看交互式 API 文档
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Volcano Blog API")
                        .version("1.0.0")
                        .description("Volcano Blog 博客系统的 RESTful API 文档")
                        .contact(new Contact()
                                .name("Volcano Blog Team")
                                .email("bernardyao624@gmail.com")
                                .url("https://github.com/Bernardyao/Volcano-Blog"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 认证令牌 (在请求头中添加: Authorization: Bearer <token>)")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}

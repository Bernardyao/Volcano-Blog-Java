package com.volcano.blog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "健康检查", description = "系统健康状态检查")
public class HealthController {

    private final DataSource dataSource;

    @Operation(summary = "健康检查", description = "检查服务是否正常运行")
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Server is healthy");
        response.put("timestamp", Instant.now().toString());
        
        // 检查数据库连接
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("database", checkDatabaseHealth());
        response.put("components", components);
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "API 测试", description = "测试 API 是否可访问")
    @GetMapping("/api/test")
    public ResponseEntity<?> apiTest() {
        return ResponseEntity.ok(Map.of("success", true, "message", "API is working!"));
    }

    /**
     * 检查数据库连接状态
     */
    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> dbStatus = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();
        
        try (Connection conn = dataSource.getConnection()) {
            boolean isValid = conn.isValid(5); // 5秒超时
            long responseTime = System.currentTimeMillis() - startTime;
            
            dbStatus.put("status", isValid ? "UP" : "DOWN");
            dbStatus.put("responseTime", responseTime + "ms");
            dbStatus.put("database", conn.getMetaData().getDatabaseProductName());
            dbStatus.put("version", conn.getMetaData().getDatabaseProductVersion());
        } catch (Exception e) {
            log.error("Database health check failed", e);
            dbStatus.put("status", "DOWN");
            dbStatus.put("error", e.getMessage());
        }
        
        return dbStatus;
    }
}

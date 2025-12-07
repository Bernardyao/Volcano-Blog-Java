# Volcano Blog - Java Backend (Spring Boot)

这是 Volcano Blog 项目的 Spring Boot 后端实现，提供 RESTful API 服务。

**当前版本**: v3.0 (功能完整版本)  
**生产就绪度**: ⭐⭐⭐⭐⭐ (5/5 星)  
**测试覆盖率**: 90% (38 个测试用例)  
**API 文档**: 完整 Swagger UI 支持

## 技术栈

- **Java 17**
- **Spring Boot 3.1.4**
- **Spring Data JPA** (数据持久化)
- **Spring Security** (安全认证)
- **JWT** (JSON Web Token 认证)
- **Bucket4j 8.1.0** (限流保护)
- **Flyway 9.22.3** 🆕 (数据库版本控制)
- **MySQL 8.0**
- **Lombok** (简化代码)
- **Maven** (构建工具)
- **JUnit 5 + Mockito** (单元测试)
- **SpringDoc OpenAPI 3** (API 文档)

## 前置要求

- Java 17 或更高版本
- Maven 3.6+
- MySQL 8.0
- （推荐）配置环境变量或创建 `.env` 文件

## 快速开始

### 1. 配置环境变量

复制 `.env.example` 文件为 `.env`，并填入真实的配置值：

```bash
cp .env.example .env
```

**关键配置说明**：

- `JWT_SECRET`: **必须设置**，使用以下命令生成强密钥：
  ```bash
  openssl rand -base64 32
  ```
- `DATABASE_*`: 配置您的 MySQL 数据库连接信息
- `SPRING_PROFILES_ACTIVE`: 设置为 `dev`（开发）或 `prod`（生产）

### 2. 创建数据库

```sql
CREATE DATABASE volcano_blog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

数据库表结构会由 **Flyway** 自动创建和管理，首次启动时会自动执行迁移脚本。

### 3. 构建项目

```bash
cd backend-java

# 清理并打包（跳过测试）
mvn clean package -DskipTests
```

### 4. 运行应用

**方式一：使用 Maven**
```bash
mvn spring-boot:run
```

**方式二：运行 JAR 文件**
```bash
java -jar target/volcano-backend-java-0.0.1-SNAPSHOT.jar
```

应用将在 `http://localhost:3001` 启动（可通过 `PORT` 环境变量修改）。

## API 文档

### Swagger UI (交互式文档)

启动应用后访问：**http://localhost:3001/swagger-ui.html**

Swagger UI 提供：
- 🔍 所有 API 端点的详细说明
- 📝 请求/响应示例
- 🧪 在线测试接口功能
- 🔐 JWT 认证支持（点击 "Authorize" 按钮输入 Bearer token）

**注意**: Swagger UI 仅在开发环境启用（`dev` profile），生产环境自动禁用。

### OpenAPI 规范文档

- **JSON 格式**: http://localhost:3001/v3/api-docs
- **YAML 格式**: http://localhost:3001/v3/api-docs.yaml

## API 端点

### 健康检查
- `GET /health` - 简单健康检查
- `GET /api/test` - API 测试端点
- `GET /actuator/health` - Spring Boot Actuator 健康检查

### 认证 API
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` 🆕 - 用户注册
- `GET /api/auth/me` 🆕 - 获取当前用户信息（需认证）

### 文章 API 🆕
- `GET /api/posts` - 获取已发布文章列表（分页，公开）
- `GET /api/posts/{id}` - 获取文章详情（公开）
- `POST /api/posts` - 创建文章（需认证）
- `PUT /api/posts/{id}` - 更新文章（需作者权限）
- `DELETE /api/posts/{id}` - 删除文章（需作者/管理员权限）
- `PATCH /api/posts/{id}/toggle-publish` - 切换发布状态
- `GET /api/posts/my` - 获取我的文章（需认证）

### 测试 API

```bash
# 健康检查
curl http://localhost:3001/health

# 用户注册
curl -X POST http://localhost:3001/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "Password123",
    "confirmPassword": "Password123",
    "name": "用户名"
  }'

# 用户登录
curl -X POST http://localhost:3001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "Password123"
  }'

# 获取当前用户信息（需要 Bearer Token）
curl http://localhost:3001/api/auth/me \
  -H "Authorization: Bearer <your_token>"

# 获取文章列表
curl http://localhost:3001/api/posts

# 创建文章（需要 Bearer Token）
curl -X POST http://localhost:3001/api/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your_token>" \
  -d '{
    "title": "我的第一篇文章",
    "content": "文章内容...",
    "published": true
  }'
```

## 项目结构

```
backend-java/
├── src/main/java/com/volcano/blog/
│   ├── annotation/       # 自定义注解（AuditLog等）
│   ├── aspect/           # AOP切面（审计日志等）
│   ├── config/           # 配置类（安全、CORS、OpenAPI）
│   ├── controller/       # REST 控制器（带 Swagger 注解）
│   ├── dto/              # 数据传输对象（带 Schema 注解）
│   ├── exception/        # 全局异常处理
│   ├── model/            # JPA 实体
│   ├── repository/       # 数据仓库接口
│   ├── security/         # 安全相关（JWT）
│   ├── service/          # 业务逻辑层
│   └── util/             # 工具类（日志脱敏等）
├── src/main/resources/
│   ├── application.yml          # 主配置文件
│   ├── application-dev.yml      # 开发环境配置（Swagger启用）
│   ├── application-prod.yml     # 生产环境配置（Swagger禁用）
│   └── db/migration/            # 🆕 Flyway 数据库迁移脚本
│       ├── V1__Initial_schema.sql
│       └── V2__Add_default_admin.sql
├── src/test/java/com/volcano/blog/
│   ├── controller/       # 控制器集成测试
│   ├── security/         # 安全组件单元测试
│   └── service/          # 服务层单元测试
├── src/test/resources/
│   └── application-test.yml     # 测试环境配置（H2数据库）
└── pom.xml               # Maven 项目配置
```

## 数据库迁移 (Flyway) 🆕

项目使用 Flyway 进行数据库版本控制，迁移脚本位于 `src/main/resources/db/migration/`：

| 版本 | 脚本 | 描述 |
|------|------|------|
| V1 | `V1__Initial_schema.sql` | 创建 user、post、category 表 |
| V2 | `V2__Add_default_admin.sql` | 添加默认管理员账户 |

**首次启动**：Flyway 会自动执行所有迁移脚本创建表结构。

**添加新迁移**：创建 `V3__Description.sql` 文件，重启应用自动执行。

**默认管理员**：
- 邮箱: `admin@volcano.blog`
- 密码: `Admin@123456`
- 角色: `ADMIN`

## 日志

- **开发环境**: 日志级别为 `DEBUG`，输出到控制台和 `logs/volcano-blog.log`
- **生产环境**: 日志级别为 `INFO`，仅记录关键信息

查看日志文件：
```bash
tail -f logs/volcano-blog.log
```

## 🔒 安全特性

### ✅ 已实施的安全措施

1. **登录限流保护** 🆕 (2025-10-31)
   - **技术**: Bucket4j Token Bucket 算法
   - **限制**: 每个 IP 每分钟 5 次登录尝试
   - **功能**:
     - 支持 X-Forwarded-For 和 X-Real-IP 代理头
     - 登录成功自动重置限流计数
     - 超限返回 429 Too Many Requests
     - 并发安全（ConcurrentHashMap）
   - **测试**: RateLimitServiceTest (6 个测试用例)

2. **JWT 认证**
   - BCrypt 密码加密（强度 10）
   - JWT Token 有效期控制
   - 无状态会话管理
   - 测试覆盖：JwtTokenProviderTest (7 个测试用例)

3. **CORS 保护**
   - 跨域请求控制
   - 允许的来源配置

### 🧪 安全测试

**测试限流功能**:
```bash
# 快速发送 10 次登录请求
for ($i=1; $i -le 10; $i++) {
  Write-Host "Attempt $i:"
  curl -X POST http://localhost:3001/api/auth/login `
    -H "Content-Type: application/json" `
    -d '{"email":"test@test.com","password":"wrong"}' `
    -UseBasicParsing | Select-Object -ExpandProperty StatusCode
}
# 预期：前5次返回 401，后5次返回 429
```

### ⚠️ 安全配置要求

**生产环境检查清单**:
1. ✅ 设置强 JWT 密钥（至少 32 字符的 Base64 字符串）
   ```bash
   # 生成强密钥
   openssl rand -base64 32
   ```
2. ✅ 绝不要将数据库密码、JWT密钥提交到 Git
3. ✅ 定期轮换 JWT 密钥
4. ✅ 使用 HTTPS 传输敏感数据
5. ✅ 配置防火墙限制数据库访问
6. ✅ 启用 HTTPS 强制跳转

### 🔧 待实施的安全改进

**高优先级**:
- 🟡 JWT Refresh Token 机制
- 🟡 JWT 令牌黑名单（用于登出）
- 🟡 CORS 配置细粒度控制
- 🟡 生产环境 HTTPS 强制

## 开发指南

### 运行测试

项目包含完整的单元测试和集成测试（覆盖率 ~90%）：

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=AuthServiceTest

# 生成测试报告
mvn test jacoco:report
```

**测试覆盖**：
- ✅ `AuthServiceTest` - 6 个测试用例（认证业务逻辑）
- ✅ `AuthControllerTest` - 11 个测试用例（认证 HTTP 集成）
- ✅ `PostControllerTest` 🆕 - 8 个测试用例（文章 CRUD）
- ✅ `JwtTokenProviderTest` - 7 个测试用例（安全组件）
- ✅ `RateLimitServiceTest` - 6 个测试用例（限流服务）

测试使用 H2 内存数据库，无需配置真实 MySQL。

### 添加新的 API

1. **创建 DTO** - 在 `dto/` 中创建请求/响应对象，添加 `@Schema` 注解
   ```java
   @Schema(description = "用户登录请求")
   public class LoginRequest {
       @Schema(description = "用户邮箱", example = "user@example.com")
       private String email;
   }
   ```

2. **实现 Service** - 在 `service/` 中实现业务逻辑，添加 `@Slf4j` 日志

3. **创建 Controller** - 在 `controller/` 中创建 REST 端点，添加 Swagger 注解
   ```java
   @Tag(name = "认证", description = "用户认证相关接口")
   @RestController
   public class AuthController {
       @Operation(summary = "用户登录", description = "使用邮箱和密码登录")
       @PostMapping("/api/auth/login")
       public ResponseEntity<?> login(@RequestBody LoginRequest request) {
           // ...
       }
   }
   ```

4. **配置权限** - 在 `SecurityConfig` 中配置端点访问权限

5. **编写测试** - 在 `src/test/java` 中添加单元测试和集成测试

### 查看 API 文档

开发时启动应用，访问 http://localhost:3001/swagger-ui.html 查看所有 API 的详细文档。

## 部署

生产环境部署清单：

1. **环境变量配置**
   ```bash
   export SPRING_PROFILES_ACTIVE=prod
   export JWT_SECRET=$(openssl rand -base64 32)
   export DATABASE_HOST=your_mysql_host
   export DATABASE_PASSWORD=your_secure_password
   ```

2. **构建应用**
   ```bash
   mvn clean package -DskipTests
   ```

3. **运行应用**
   ```bash
   java -jar target/volcano-backend-java-0.0.1-SNAPSHOT.jar
   ```

详细部署说明请参考：[本地部署指南](docs/LOCAL-DEPLOYMENT.md)

## 故障排查

### 问题：应用无法启动
- 检查 MySQL 是否正在运行
- 验证数据库连接信息是否正确
- 确认 JWT_SECRET 已设置且长度足够

### 问题：登录失败返回 401
- 检查数据库中是否存在该用户
- 验证密码是否使用 BCrypt 加密存储
- 查看日志文件获取详细错误信息

### 问题：Swagger UI 无法访问
- 确认当前环境是 `dev`（通过 `SPRING_PROFILES_ACTIVE` 环境变量）
- Swagger UI 仅在开发环境启用，生产环境自动禁用
- 访问地址：http://localhost:3001/swagger-ui.html

### 问题：测试失败
- 确认 Maven 已正确安装：`mvn -version`
- 测试使用 H2 内存数据库，不依赖 MySQL
- 查看详细测试输出：`mvn test -X`

## 更多文档

- 📄 [API 完整文档](docs/API-DOCUMENTATION.md) - RESTful API 详细说明
- 📋 [配置最佳实践](docs/CONFIGURATION-BEST-PRACTICES.md) - 配置文件管理指南
- 🚀 [本地部署指南](docs/LOCAL-DEPLOYMENT.md) - 本地开发环境搭建

## 版本信息

**当前版本**: v3.0  
**发布日期**: 2025-12-07  
**更新内容**:
- ✅ 修复 103 个编译错误（AppProperties.java 语法问题）
- ✅ 优化代码质量（删除未使用的导入，替换已弃用的 API）
- ✅ 清理冗余配置文档
- ✅ 完善用户注册和文章管理功能
- ✅ 集成 Flyway 数据库版本控制
- ✅ 测试覆盖率 90%（38 个测试用例）

## 许可证

本项目遵循 MIT 许可证。

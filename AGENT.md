# Volcano Blog Backend - AI协作指南

## 项目概述

**Volcano Blog** 是一个基于 Spring Boot 3.1.4 构建的企业级博客系统后端，提供完整的 RESTful API 服务。

### 核心特性
- ✅ **认证授权**: JWT + Spring Security 实现无状态认证
- ✅ **限流保护**: Bucket4j 令牌桶算法（5次/分钟/IP）
- ✅ **数据库迁移**: Flyway 版本控制
- ✅ **API 文档**: SpringDoc OpenAPI 3（Swagger UI）
- ✅ **测试覆盖**: 90% 覆盖率（38 个测试用例）
- ✅ **生产就绪**: 完善的日志、监控、异常处理

### 技术栈
| 领域 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 语言 | Java | 17 | 基础语言 |
| 框架 | Spring Boot | 3.1.4 | Web框架 |
| 安全 | Spring Security | 6.x | 认证授权 |
| 数据 | Spring Data JPA | - | ORM框架 |
| 数据库 | MySQL | 8.0+ | 主数据库 |
| 迁移 | Flyway | 9.22.3 | 数据库版本控制 |
| 限流 | Bucket4j | 8.1.0 | 流量控制 |
| 构建 | Maven | 3.6+ | 依赖管理 |
| 文档 | SpringDoc OpenAPI | 2.2.0 | API文档 |
| 测试 | JUnit 5 + Mockito | - | 单元测试 |

---

## 项目结构规范

### 📁 目录结构
```
src/main/java/com/volcano/blog/
├── annotation/          # 自定义注解 (@AuditLog, @RateLimit)
├── aspect/             # AOP 切面 (审计日志、限流)
├── config/             # 配置类 (Security, OpenAPI, AppProperties)
├── controller/         # REST 控制器 (HTTP 层)
├── dto/               # 数据传输对象 (API 契约)
├── exception/         # 异常处理 (全局异常处理器)
├── model/             # JPA 实体 (领域模型)
├── repository/         # 数据仓库 (持久化层)
├── security/           # 安全组件 (JWT, 过滤器)
├── service/            # 业务逻辑 (核心业务)
└── util/              # 工具类
```

### 📝 命名规范

**类命名**：
- Controller: `XxxController.java` (如: `AuthController`)
- Service: `XxxService.java` (如: `PostService`)
- ServiceImpl: `XxxServiceImpl.java`
- Repository: `XxxRepository.java`
- Entity: `Xxx.java` (如: `User.java`, `Post.java`)
- DTO: `XxxDto.java`, `XxxRequest.java`, `XxxResponse.java`
- Exception: `XxxException.java`

**方法命名**：
- Repository: `findByXxx()`, `save()`, `deleteById()`
- Service: `getXxx()`, `createXxx()`, `updateXxx()`, `deleteXxx()`
- Controller: 使用 RESTful 动词 `getXxx()`, `createXxx()`, `updateXxx()`, `deleteXxx()`

**字段命名**：
- 数据库: `snake_case` (如: `created_at`)
- Java: `camelCase` (如: `createdAt`)
- JSON: `camelCase`

---

## 开发规范

### 🎯 代码风格

**1. 注解使用**
```java
// Controller 层
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户认证相关 API")
public class AuthController {
    @Operation(summary = "用户登录", description = "使用邮箱和密码登录")
    @ApiResponses({@ApiResponse(...)}) // Swagger 文档
    @AuditLog(value = "用户登录", action = AuditAction.LOGIN) // 审计日志
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        // ...
    }
}
```

**2. DTO 规范**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户信息")
public class UserDto {
    @Schema(description = "用户ID", example = "1")
    private Long id;

    // 静态转换方法
    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                // ...
                .build();
    }
}
```

**3. 实体规范**
```java
@Entity
@Table(name = "user")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        // ...
    }
}
```

### 🔒 安全规范

**1. 密码安全**
- 必须使用 BCrypt 加密：`passwordEncoder.encode(password)`
- 验证强度：至少 8 位，包含大小写字母、数字

**2. JWT 安全**
- 密钥长度：至少 32 字符 Base64
- Token 过期时间：合理设置（建议 1-24 小时）
- 不在 Token 中存储敏感信息

**3. CORS 配置**
- 生产环境严格限制来源域名
- 只允许必要的 HTTP 方法

**4. 限流配置**
- 登录接口：5次/分钟/IP
- API 接口：可配置不同策略
- 错误响应码：429 Too Many Requests

### 🧪 测试规范

**测试覆盖率要求**：
- 新功能必须包含测试
- 覆盖率保持 90% 以上
- 重点测试：认证、授权、业务逻辑

**测试类命名**：
- Service 测试: `XxxServiceTest.java`
- Controller 测试: `XxxControllerTest.java`
- 集成测试: `XxxIntegrationTest.java`

**测试示例**：
```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("应该成功注册新用户")
    void shouldRegisterUserSuccessfully() {
        // Given
        RegisterRequest request = createRegisterRequest();
        // When
        UserDto result = authService.register(request);
        // Then
        assertThat(result).isNotNull();
        // ...
    }
}
```

---

## 常用协作任务

### 🚀 新增 API 端点

**步骤**：
1. 创建/更新 DTO（`dto/` 目录）
   ```java
   @Schema(description = "创建请求")
   public class CreateXxxRequest {
       @NotBlank @Schema(description = "名称")
       private String name;
   }
   ```

2. 实现 Service 逻辑（`service/` 目录）
   ```java
   @Service
   @RequiredArgsConstructor
   @Transactional
   public class XxxService {
       public XxxDto create(CreateXxxRequest request) {
           // 业务逻辑
       }
   }
   ```

3. 创建 Controller（`controller/` 目录）
   ```java
   @RestController
   @RequestMapping("/api/xxx")
   @Tag(name = "Xxx管理")
   public class XxxController {
       @PostMapping
       @Operation(summary = "创建Xxx")
       public ResponseEntity<XxxDto> create(@Valid @RequestBody CreateXxxRequest request) {
           // ...
       }
   }
   ```

4. 配置安全权限（`config/SecurityConfig.java`）
   ```java
   .requestMatchers("/api/xxx/**").authenticated()
   ```

5. 编写测试（`src/test/java/`）
6. 运行测试：`mvn test`

### 🔧 修改数据库结构

**使用 Flyway 迁移**：
1. 创建迁移脚本：`src/main/resources/db/migration/V3__Description.sql`
   ```sql
   ALTER TABLE user ADD COLUMN phone VARCHAR(20);
   CREATE INDEX idx_user_phone ON user(phone);
   ```

2. 重启应用（Flyway 自动执行）

3. 更新实体类（同步添加字段）

4. 创建反向迁移（如需回滚）：`V3.1__Rollback_phone.sql`

### 🐛 调试问题

**查看日志**：
```bash
# 实时日志
tail -f logs/volcano-blog.log

# 搜索错误
grep -i error logs/volcano-blog.log | tail -50
```

**健康检查端点**：
- 应用状态：`GET /actuator/health`
- 简单检查：`GET /health`
- API 测试：`GET /api/test`

**测试特定功能**：
```bash
# 测试登录限流
for i in {1..10}; do
  curl -X POST http://localhost:3001/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@test.com","password":"wrong"}'
done
```

### 📊 性能优化

**1. 数据库优化**
- 使用索引：`@Table(indexes = {...})`
- 延迟加载：`@FetchType.LAZY`
- 分页查询：`Pageable`

**2. 缓存策略**
```java
// Service 层使用缓存
@Cacheable(value = "posts", key = "#id")
public PostDto getById(Long id) {
    // ...
}
```

**3. 查询优化**
```java
// 使用 Projection 减少数据传输
@Query("SELECT new com.volcano.blog.dto.PostSummary(p.id, p.title) FROM Post p WHERE p.published = true")
List<PostSummary> findPublishedSummaries();
```

---

## 配置文件说明

### 🔧 环境配置

**开发环境** (`application-dev.yml`)：
- 启用 Swagger UI
- 日志级别：DEBUG
- 数据库：本地 MySQL

**生产环境** (`application-prod.yml`)：
- 禁用 Swagger
- 日志级别：INFO
- 数据库：生产 MySQL

**测试环境** (`application-test.yml`)：
- H2 内存数据库
- 自动回滚

### 📝 环境变量

**必填配置**：
```bash
# JWT 密钥（必须）
JWT_SECRET=your-base64-secret

# 数据库配置
DATABASE_HOST=localhost
DATABASE_PORT=3306
DATABASE_NAME=volcano_blog
DATABASE_USERNAME=root
DATABASE_PASSWORD=password

# 环境
SPRING_PROFILES_ACTIVE=dev
```

**可选配置**：
```bash
# 限流配置
RATELIMIT_LOGIN_CAPACITY=5
RATELIMIT_LOGIN_REFILL_TOKENS=5

# 应用端口
PORT=3001
```

---

## 常见问题解决

### ❌ 应用启动失败

**检查清单**：
1. ✅ MySQL 服务是否运行：`systemctl status mysql`
2. ✅ 数据库是否存在：`CREATE DATABASE volcano_blog;`
3. ✅ JWT_SECRET 是否设置：`echo $JWT_SECRET`
4. ✅ 环境变量加载：`cat .env`

**错误排查**：
```bash
# 查看启动日志
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 详细错误信息
mvn clean compile -X
```

### ❌ 数据库连接失败

**解决方案**：
1. 检查 MySQL 版本（需要 8.0+）
2. 验证连接信息
3. 确认用户权限：`GRANT ALL PRIVILEGES ON volcano_blog.* TO 'root'@'localhost';`
4. 测试连接：`mysql -h localhost -u root -p volcano_blog`

### ❌ 登录返回 401

**可能原因**：
- 用户不存在（检查数据库）
- 密码未加密（BCrypt）
- JWT 密钥问题
- Token 过期

**调试方法**：
```bash
# 查看认证日志
grep -i "authentication" logs/volcano-blog.log

# 手动验证密码
# BCrypt 校验需要在代码中测试
```

### ❌ 测试失败

**解决方案**：
```bash
# 清理并重新测试
mvn clean test

# 运行特定测试
mvn test -Dtest=AuthServiceTest

# 生成覆盖率报告
mvn test jacoco:report
# 查看报告：target/site/jacoco/index.html
```

---

## API 文档

### 📖 文档访问

**开发环境**：
- Swagger UI: http://localhost:3001/swagger-ui.html
- OpenAPI JSON: http://localhost:3001/v3/api-docs
- OpenAPI YAML: http://localhost:3001/v3/api-docs.yaml

**生产环境**：
- Swagger UI: 禁用（安全考虑）
- 文档文件: `/docs/api/` 目录

### 🔍 常用端点

**健康检查**：
```bash
GET /health
GET /actuator/health
GET /api/test
```

**认证**：
```bash
POST /api/auth/register
POST /api/auth/login
GET /api/auth/me
```

**文章**：
```bash
GET /api/posts
GET /api/posts/{id}
POST /api/posts
PUT /api/posts/{id}
DELETE /api/posts/{id}
```

### 🔐 认证方式

**使用 Swagger UI**：
1. 启动应用（dev 环境）
2. 访问 http://localhost:3001/swagger-ui.html
3. 点击 "Authorize" 按钮
4. 输入格式：`Bearer <your_jwt_token>`

**使用 curl**：
```bash
# 1. 登录获取 token
TOKEN=$(curl -s -X POST http://localhost:3001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Password123"}' \
  | jq -r '.data.token')

# 2. 使用 token 访问需要认证的接口
curl -H "Authorization: Bearer $TOKEN" http://localhost:3001/api/auth/me
```

---

## 数据库设计

### 📊 表结构

**user 表**：
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| email | VARCHAR(100) | UNIQUE, NOT NULL | 邮箱（登录名）|
| password | VARCHAR(255) | NOT NULL | 密码（BCrypt）|
| name | VARCHAR(50) | - | 用户名 |
| role | VARCHAR(20) | DEFAULT 'USER' | 角色 |
| created_at | TIMESTAMP | DEFAULT NOW | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT NOW ON UPDATE | 更新时间 |

**post 表**：
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| title | VARCHAR(200) | NOT NULL | 标题 |
| content | TEXT | - | 内容 |
| published | BOOLEAN | DEFAULT FALSE | 发布状态 |
| author_id | BIGINT | FK -> user(id) | 作者 |
| created_at | TIMESTAMP | DEFAULT NOW | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT NOW ON UPDATE | 更新时间 |

### 🔍 索引

**user 表索引**：
- `idx_user_email`: email 字段唯一索引

**post 表索引**：
- `idx_post_author`: author_id 索引
- `idx_post_published`: published 索引
- `idx_post_created`: created_at 索引

### 🔄 迁移脚本

| 版本 | 脚本 | 描述 |
|------|------|------|
| V1 | `V1__Initial_schema.sql` | 创建表结构 |
| V2 | `V2__Add_default_admin.sql` | 添加默认管理员账户 |

**默认管理员**：
- 邮箱: `admin@volcano.blog`
- 密码: `Admin@123456`
- 角色: `ADMIN`

---

## 协作流程

### 📦 发布新功能

**步骤**：
1. 更新版本号（`pom.xml` 中的 `<version>`）
2. 运行所有测试：`mvn test`
3. 生成文档：`mvn site`（可选）
4. 构建 JAR：`mvn clean package -DskipTests`
5. 更新 `CHANGELOG.md`（如果存在）
6. 提交并推送代码

### 🐛 修复 Bug

**步骤**：
1. 创建 Issue（如果尚未创建）
2. 创建修复分支：`git checkout -b fix/issue-xxx`
3. 编写测试（重现 Bug）
4. 修复代码
5. 验证测试通过
6. 更新文档（如需要）
7. 提交 PR

### 💡 新增功能

**步骤**：
1. 创建 Feature 分支：`git checkout -b feature/xxx`
2. 编写设计文档（如需要）
3. 实现功能
4. 编写测试（单元 + 集成）
5. 运行完整测试套件
6. 更新 API 文档
7. 提交 PR

### 🔒 安全问题

**发现安全漏洞**：
1. **不要**在公开 Issue 中描述
2. 发送邮件到：security@volcano.blog
3. 等待维护团队确认
4. 修复后发布安全公告

---

## 工具链

### 🛠️ 开发工具

**推荐 IDE**：
- IntelliJ IDEA（首选）
- VS Code（轻量级）

**必需插件**：
- Lombok Plugin（IDEA）
- Spring Boot Extension Pack
- Java Extension Pack（VS Code）

### 📊 质量工具

| 工具 | 用途 | 配置 |
|------|------|------|
| Maven | 依赖管理 | `pom.xml` |
| JaCoCo | 测试覆盖率 | `mvn jacoco:report` |
| SpotBugs | 代码质量检查 | `mvn spotbugs:check` |
| Checkstyle | 代码规范检查 | `mvn checkstyle:check` |

### 🚀 部署工具

**本地构建**：
```bash
# 开发模式
mvn spring-boot:run

# 生产构建
mvn clean package -DskipTests
java -jar target/volcano-backend-java-0.0.1-SNAPSHOT.jar
```

**Docker 部署**（可选）：
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/volcano-backend-java-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

---

## 资源链接

### 📚 文档
- [README](README.md) - 项目概览
- [API 文档](docs/API-DOCUMENTATION.md) - 完整 API 说明
- [部署指南](docs/LOCAL-DEPLOYMENT.md) - 本地部署
- [配置说明](docs/CONFIGURATION-BEST-PRACTICES.md) - 配置最佳实践
- [设计原则](docs/CODE-DESIGN-PRINCIPLES.md) - 架构与设计

### 🔗 外部资源
- [Spring Boot 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security 参考](https://docs.spring.io/spring-security/reference/)
- [Spring Data JPA 指南](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [JWT 标准](https://jwt.io/)
- [Bucket4j 文档](https://bucket4j.com/)

### 💬 支持与反馈
- Issue Tracker: [GitHub Issues]
- 邮箱: support@volcano.blog
- 文档反馈: docs@volcano.blog

---

## 版本信息

**当前版本**: v3.0
**最后更新**: 2025-12-07
**维护团队**: Volcano Blog 开发组

**更新日志**：
- ✅ 修复 103 个编译错误
- ✅ 优化代码质量
- ✅ 完善用户注册和文章管理
- ✅ 集成 Flyway 数据库版本控制
- ✅ 90% 测试覆盖率

---

## AI 协作注意事项

### ✅ 应该做的

1. **遵循现有模式**
   - 参考现有代码风格和结构
   - 使用项目中已有的注解和工具类
   - 保持接口一致性

2. **编写测试**
   - 为新功能编写测试
   - 确保测试覆盖率不低于 90%
   - 单元测试 + 集成测试

3. **完善文档**
   - 为新 API 添加 Swagger 注解
   - 更新 API 文档
   - 添加必要的注释

4. **考虑安全**
   - 验证所有输入
   - 使用适当的权限控制
   - 遵循密码安全规范

### ❌ 不应该做的

1. **不要破坏现有 API**
   - 现有端点保持向后兼容
   - 使用版本控制（可选）

2. **不要提交敏感信息**
   - `.env` 文件
   - 密钥和密码
   - 日志中的敏感数据

3. **不要忽略性能**
   - 避免 N+1 查询
   - 合理使用索引
   - 及时清理资源

4. **不要跳过测试**
   - 运行完整测试套件
   - 修复失败的测试
   - 保持高覆盖率

### 🎯 优化建议

**代码质量**：
- 保持类和方法简短（< 100 行）
- 使用有意义的命名
- 避免重复代码（DRY 原则）
- 使用 Lombok 减少样板代码

**性能优化**：
- 使用分页查询大数据集
- 合理使用懒加载
- 优化数据库查询
- 使用缓存减少重复计算

**可维护性**：
- 遵循 SOLID 原则
- 编写清晰的文档
- 使用版本控制
- 及时重构代码

---

## ⚡ 性能优化指南 (Bolt 模式)

您是 **Bolt** ⚡ - 一个性能至上的助手，专注于让代码库更快，一次优化一个改进。

### 🎯 核心理念

- **速度就是特性** - 每毫秒都重要
- **先测量，后优化** - 不要过早优化
- **不牺牲可读性** - 代码必须保持清晰易懂
- **小步快跑** - 每次只做一个小的性能改进
- **可测量** - 每个优化都要有可衡量的性能提升

### 📋 优化流程

**1. 🔍 寻找机会**
   - 分析性能瓶颈
   - 识别慢查询
   - 检查重复计算
   - 评估缓存机会

**2. ⚡ 选择优化点**
   - 选择最有价值的改进
   - 确保可快速实现（< 50 行代码）
   - 风险低，不破坏功能
   - 有明确的性能提升

**3. 🔧 实施优化**
   - 编写清晰、可读的优化代码
   - 添加注释说明优化原理
   - 保持现有功能完整性
   - 考虑边界情况

**4. ✅ 验证效果**
   - 运行格式化检查：`mvn spotless:check`
   - 运行完整测试：`mvn test`
   - 验证性能提升
   - 确保无功能损坏

**5. 🎁 提交优化**
   - PR 标题：`⚡ Bolt: [性能改进描述]`
   - 详细说明：问题、解决方案、预期影响
   - 添加性能测量数据

### ✅ 应该做的性能优化

**数据库层面**：
- ⚡ 添加数据库索引到频繁查询字段
- ⚡ 使用分页查询大数据集
- ⚡ 解决 N+1 查询问题（使用 `@EntityGraph` 或 `JOIN FETCH`）
- ⚡ 使用查询提示优化复杂查询
- ⚡ 为只读操作使用 `@Transactional(readOnly = true)`

**JPA 优化**：
- ⚡ 使用懒加载（`@FetchType.LAZY`）减少不必要的查询
- ⚡ 使用 `@EntityGraph` 控制加载策略
- ⚡ 避免在循环中执行查询
- ⚡ 使用 `JOIN FETCH` 预加载关联数据
- ⚡ 使用 DTO 投影减少数据传输

**缓存策略**：
- ⚡ 缓存昂贵计算结果（如：统计数据）
- ⚡ 缓存不经常变化的数据（如：配置信息）
- ⚡ 使用 Caffeine Cache 进行本地缓存
- ⚡ 合理设置缓存过期时间

**API 优化**：
- ⚡ 使用分页返回大数据集
- ⚡ 只返回必要字段（DTO 投影）
- ⚡ 添加 GZIP 压缩响应
- ⚡ 使用 ETags 减少带宽
- ⚡ 实现条件 GET（If-None-Match）

**算法优化**：
- ⚡ 用 HashMap 查找替代 O(n²) 嵌套循环
- ⚡ 使用 Set 去除重复元素
- ⚡ 使用 Stream API 高效处理集合
- ⚡ 添加早返回跳过不必要处理

**Spring Boot 优化**：
- ⚡ 使用 `@Cacheable` 缓存服务层结果
- ⚡ 配置连接池大小（hikari cp）
- ⚡ 启用异步处理（`@Async`）
- ⚡ 使用 `@Transactional` 优化事务范围
- ⚡ 配置合适的 JVM 参数

### ❌ 避免的优化

**不要做的**：
- ❌ 微优化没有可测量影响
- ❌ 牺牲代码可读性
- ❌ 过度复杂的缓存策略
- ❌ 未经分析的"猜测"优化
- ❌ 修改核心架构
- ❌ 在冷路径上优化
- ❌ 添加不必要的依赖

### 🔍 性能诊断命令

**检查慢查询**：
```bash
# 启用 JPA SQL 日志（开发环境）
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# 查看执行时间
grep "took" logs/volcano-blog.log | tail -50
```

**压力测试**：
```bash
# 使用 Apache Bench 测试 API
ab -n 1000 -c 10 http://localhost:3001/api/posts

# 使用 wrk 测试
wrk -t12 -c400 -d30s http://localhost:3001/api/posts
```

**分析内存使用**：
```bash
# 生成堆转储
jmap -dump:format=b,file=heap.hprof <pid>

# 分析内存使用
jhat heap.hprof
```

**查看 GC 日志**：
```bash
# 启用 GC 日志
java -XX:+PrintGCDetails -Xloggc:gc.log -jar target/volcano-backend-java-0.0.1-SNAPSHOT.jar
```

### 📊 性能基线

**当前性能指标**：
- API 响应时间（95%）：< 200ms
- 数据库查询时间：< 50ms
- 并发用户数：100
- 吞吐量：500 req/s

**目标改进**：
- API 响应时间减少 20-30%
- 数据库查询次数减少 50%
- 内存使用减少 15-20%

### 🎁 提交优化示例

**PR 模板**：
```markdown
## ⚡ Bolt: 优化文章列表查询性能

### 💡 优化内容
使用 `@EntityGraph` 预加载作者信息，解决 N+1 查询问题。

### 🎯 解决的问题
原代码在查询已发布文章时，对每篇文章都单独查询作者信息，导致 N+1 查询问题：
- 1 次查询获取文章列表（N 条）
- N 次查询获取作者信息

### 📊 预期影响
- 减少数据库查询次数：从 N+1 次到 2 次
- 响应时间提升：约 40-50%
- 性能测试：100 篇文章从 120ms 降至 65ms

### 🔬 验证方法
1. 运行现有测试：`mvn test`
2. 启用 SQL 日志验证查询次数
3. 压力测试：`ab -n 100 -c 10 /api/posts`

### 📝 修改文件
- `PostRepository.java`: 添加 `@EntityGraph`
- `PostService.java`: 优化查询方法
```

### 🏆 性能优化清单

每次优化前检查：
- [ ] 是否真的存在性能问题？
- [ ] 问题是否可测量？
- [ ] 优化方案是否简单？
- [ ] 是否保持代码可读性？
- [ ] 是否编写了测试？
- [ ] 是否验证了性能提升？

**记住**：速度很重要，但没有正确性就毫无意义。测量、优化、验证。如果没有找到明确的性能提升机会，等待明天的机会。

---

**🎉 欢迎使用 AI 助手协作开发 Volcano Blog！**

如有问题，请参考本文档或联系开发团队。

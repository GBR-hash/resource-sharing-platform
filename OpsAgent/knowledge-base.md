# 资料共享平台 - 知识库文档

> 本文档为 AIOps Agent（LangGraph SRE Incident Responder）提供系统业务知识，用于故障诊断和自动修复。

---

## 1. 系统概述

**资料共享平台**是一个基于 Spring Boot 3.2.0 + Vue 3 的单体全栈应用，用于竞赛项目资料的上传、审核、分享与下载。

### 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Element Plus + Vite |
| 后端 | Spring Boot 3.2.0 + Spring Security + JWT |
| 数据库 | MySQL 8.0 + Spring Data JPA (HikariCP) |
| 文件存储 | 本地文件系统 (`./uploads`) |
| 构建工具 | Maven |

### 部署架构

```
浏览器 (Vue 3 SPA)
    │
    │ HTTP/REST (http://localhost:8080/api/*)
    ▼
Spring Boot 应用 (端口 8080)
    ├── Spring Security + JWT 认证
    ├── Spring Data JPA (HikariCP 连接池)
    ├── 文件上传/下载服务
    └── Actuator + Prometheus 监控
            │
            ▼
        MySQL 8.0 (example_db)
        本地文件系统 (./uploads)
```

---

## 2. 核心业务流程

### 2.1 用户注册与登录

```
用户注册 → 密码 BCrypt 加密 → 存入 users 表 (role=0 普通用户)
用户登录 → 验证用户名密码 → 生成 JWT Token → 返回 token + 用户信息
```

- 注册接口：`POST /api/auth/register`
- 登录接口：`POST /api/auth/login`
- JWT 有效期：24 小时（86400000ms）
- Token 前缀：`Bearer `

### 2.2 资料上传与审核

```
普通用户上传资料 → 状态设为"审核中"(status=0) → 文件保存到 ./uploads/{yyyy}/{MM}/{dd}/
    → 管理员审核 → 通过(status=1) / 拒绝(status=2)
    → 通过的资料对所有用户可见并可下载
```

- 上传接口：`POST /api/resources/upload`（需登录）
- 审核通过：`PUT /api/admin/resources/{id}/approve`（需 ADMIN 角色）
- 审核拒绝：`PUT /api/admin/resources/{id}/reject`（需 ADMIN 角色）

### 2.3 资料浏览与下载

```
首页展示 → 已发布资料(status=1) + 当前用户上传的资料
    → 支持搜索、分类筛选、竞赛类型筛选、时间筛选
    → 支持下载量Top/收藏量Top排序
    → 下载时自动增加下载计数
    → 预览不增加下载计数（独立接口）
```

- 资料列表：`GET /api/resources`（无需登录）
- 资料详情：`GET /api/resources/{id}`（无需登录）
- 下载资料：`GET /api/resources/download/{id}`（无需登录，增加下载计数）
- 预览资料：`GET /api/resources/preview/{id}`（无需登录，不增加下载计数）

### 2.4 收藏功能

```
登录用户点击收藏 → 在 favorites 表插入记录 → 更新资源的 favoriteCount
    → 再次点击 → 删除收藏记录 → 更新 favoriteCount
```

- 切换收藏：`POST /api/favorites/toggle/{resourceId}`（需登录）
- 我的收藏：`GET /api/favorites/my`（需登录）

### 2.5 权限控制

| 角色 | role 值 | 权限 |
|------|---------|------|
| 普通用户 | 0 | 浏览、下载、上传、收藏 |
| 管理员 | 1 | 以上所有 + 审核资料、管理用户、删除资料 |

- 删除资料接口 `DELETE /api/resources/{id}` 仅限 ADMIN 角色
- 管理员接口 `/api/admin/**` 仅限 ADMIN 角色

---

## 3. 数据库表结构

### 3.1 users（用户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK, 自增) | 用户ID |
| username | VARCHAR(50, 唯一) | 用户名 |
| password | VARCHAR(255) | BCrypt 加密密码 |
| email | VARCHAR(100, 唯一) | 邮箱 |
| phone | VARCHAR(20) | 手机号 |
| real_name | VARCHAR(100) | 真实姓名 |
| role | INT (默认0) | 0=普通用户, 1=管理员 |
| status | INT (默认1) | 0=禁用, 1=启用 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 3.2 resources（资料表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK, 自增) | 资料ID |
| title | VARCHAR(200) | 标题 |
| description | VARCHAR(500) | 描述 |
| remark | VARCHAR(500) | 备注 |
| file_name | VARCHAR(200) | 原始文件名 |
| file_path | VARCHAR(500) | 存储路径（相对路径） |
| file_size | BIGINT | 文件大小（字节） |
| file_type | VARCHAR(50) | 文件类型标识：image/document/video |
| category_id | BIGINT (FK) | 资料分类ID |
| competition_type_id | BIGINT (FK) | 竞赛类型ID |
| uploader_id | BIGINT (FK) | 上传者ID |
| status | INT (默认0) | 0=审核中, 1=已发布, 2=已拒绝 |
| download_count | INT (默认0) | 下载次数 |
| view_count | INT (默认0) | 浏览次数 |
| favorite_count | INT (默认0) | 收藏次数 |
| created_at | DATETIME | 上传时间 |
| updated_at | DATETIME | 更新时间 |
| approved_at | DATETIME | 审核时间 |
| approved_by | BIGINT | 审核人ID |

### 3.3 categories（资料分类表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK, 自增) | 分类ID |
| name | VARCHAR(50) | 分类名称 |
| description | VARCHAR(255) | 描述 |
| parent_id | BIGINT | 父分类ID（支持多级分类） |
| sort_order | INT (默认0) | 排序序号 |
| status | INT (默认1) | 0=禁用, 1=启用 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 3.4 competition_types（竞赛类型表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK, 自增) | 竞赛类型ID |
| name | VARCHAR(50, 唯一) | 竞赛类型名称 |
| description | VARCHAR(255) | 描述 |
| sort_order | INT (默认0) | 排序序号 |
| status | INT (默认1) | 0=禁用, 1=启用 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 3.5 favorites（收藏表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK, 自增) | 收藏ID |
| user_id | BIGINT (FK) | 用户ID |
| resource_id | BIGINT (FK) | 资料ID |
| created_at | DATETIME | 收藏时间 |

- 唯一约束：(user_id, resource_id)，同一用户不能重复收藏同一资料

### 3.6 表关联关系

```
users (1) ────< (N) resources        (uploader_id → users.id)
users (1) ────< (N) favorites        (user_id → users.id)
categories (1) ────< (N) resources   (category_id → categories.id)
competition_types (1) ────< (N) resources (competition_type_id → competition_types.id)
resources (1) ────< (N) favorites    (resource_id → resources.id)
```

---

## 4. API 接口清单

### 4.1 认证接口（/api/auth）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /api/auth/register | 否 | 用户注册 |
| POST | /api/auth/login | 否 | 用户登录，返回 JWT |
| GET | /api/auth/me | 是 | 获取当前用户信息 |

### 4.2 资料接口（/api/resources）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /api/resources/upload | 是 | 上传资料（需登录） |
| GET | /api/resources | 否 | 资料列表（支持分页、筛选、排序） |
| GET | /api/resources/{id} | 否 | 资料详情 |
| GET | /api/resources/download/{id} | 否 | 下载资料（增加下载计数） |
| GET | /api/resources/preview/{id} | 否 | 预览资料（不增加下载计数） |
| DELETE | /api/resources/{id} | ADMIN | 删除资料 |

**资料列表查询参数：**
- `keyword`: 关键词搜索（标题/描述）
- `categoryId`: 资料分类ID
- `competitionTypeId`: 竞赛类型ID
- `status`: 资料状态（0/1/2）
- `timeRange`: 时间范围（today/week/month/year）
- `customDate`: 自定义日期
- `topDownloads`: 下载量Top N
- `topFavorites`: 收藏量Top N
- `pageNumber`: 页码（默认0）
- `pageSize`: 每页条数（默认10）

### 4.3 收藏接口（/api/favorites）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /api/favorites/toggle/{resourceId} | 是 | 切换收藏状态 |
| GET | /api/favorites/status/{resourceId} | 是 | 查询收藏状态 |
| GET | /api/favorites/my | 是 | 获取我的收藏列表 |

### 4.4 管理员接口（/api/admin）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | /api/admin/users | ADMIN | 用户列表 |
| PUT | /api/admin/users/{id}/status | ADMIN | 修改用户状态 |
| PUT | /api/admin/users/{id}/role | ADMIN | 修改用户角色 |
| DELETE | /api/admin/users/{id} | ADMIN | 删除用户 |
| GET | /api/admin/resources | ADMIN | 资料列表（含审核中） |
| PUT | /api/admin/resources/{id}/approve | ADMIN | 审核通过 |
| PUT | /api/admin/resources/{id}/reject | ADMIN | 审核拒绝 |
| DELETE | /api/admin/resources/{id} | ADMIN | 删除资料 |
| GET | /api/admin/categories | ADMIN | 分类列表 |
| POST | /api/admin/categories | ADMIN | 创建分类 |
| PUT | /api/admin/categories/{id} | ADMIN | 更新分类 |
| DELETE | /api/admin/categories/{id} | ADMIN | 删除分类 |
| GET | /api/admin/competition-types | ADMIN | 竞赛类型列表 |
| POST | /api/admin/competition-types | ADMIN | 创建竞赛类型 |
| PUT | /api/admin/competition-types/{id} | ADMIN | 更新竞赛类型 |
| DELETE | /api/admin/competition-types/{id} | ADMIN | 删除竞赛类型 |
| GET | /api/admin/statistics | ADMIN | 统计数据 |

### 4.5 运维接口（/api/admin/ops）— AIOps 专用

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | /api/admin/ops/health | ADMIN + Token | 系统综合健康检查 |
| GET | /api/admin/ops/connection-pool | ADMIN + Token | HikariCP 连接池状态 |
| GET | /api/admin/ops/jvm | ADMIN + Token | JVM 运行时信息 |
| GET | /api/admin/ops/disk | ADMIN + Token | 磁盘使用情况 |
| POST | /api/admin/ops/cache/evict | ADMIN + Token | 清理应用缓存 |
| POST | /api/admin/ops/service/reload | ADMIN + Token | 服务重载（模拟） |
| GET | /api/admin/ops/logs | ADMIN + Token | 查询最近日志 |

**运维接口需要双重校验：**
1. JWT Token（ADMIN 角色）
2. `X-Ops-Token` 请求头（值：`ops-secret-token-2024`）

### 4.6 故障注入接口（/api/admin/chaos）— AIOps 专用

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /api/admin/chaos/trigger?type={type}&durationSeconds={n} | ADMIN + Token | 触发故障 |
| POST | /api/admin/chaos/reset | ADMIN + Token | 清除所有故障 |
| GET | /api/admin/chaos/status | ADMIN + Token | 查看故障状态 |

**故障类型：**
- `latency`: 业务接口随机延迟 3-10 秒
- `exception`: 业务接口随机抛出 RuntimeException
- `cpu_high`: 启动死循环线程占用 CPU

### 4.7 Actuator 监控端点（/actuator）

| 端点 | 说明 |
|------|------|
| /actuator/health | 健康检查（含数据库状态） |
| /actuator/metrics | 应用指标 |
| /actuator/prometheus | Prometheus 格式指标 |
| /actuator/env | 环境变量 |
| /actuator/info | 应用信息 |
| /actuator/threaddump | 线程dump |
| /actuator/heapdump | 堆dump |
| /actuator/loggers | 日志级别管理 |

---

## 5. 错误码字典

### 5.1 HTTP 状态码

| 状态码 | 含义 | 触发场景 |
|--------|------|---------|
| 200 | 成功 | 正常请求 |
| 400 | 请求参数错误 | 参数验证失败、文件大小超限 |
| 401 | 未认证 | JWT 无效/过期、用户名密码错误 |
| 403 | 权限不足 | 非 ADMIN 角色访问管理员接口 |
| 404 | 资源不存在 | 资料/用户不存在 |
| 500 | 系统异常 | 未预期的服务器错误 |

### 5.2 业务异常（BusinessException）

| 异常 | 错误码 | 说明 |
|------|--------|------|
| `BusinessException("消息")` | 500 | 通用业务异常 |
| `BusinessException(code, "消息")` | 自定义 | 带自定义错误码的业务异常 |

### 5.3 常见错误消息

| 错误消息 | 原因 | 排查方向 |
|---------|------|---------|
| "用户名或密码错误" | 登录凭证错误 | 检查 users 表中的用户名和 BCrypt 密码 |
| "权限不足" | 非管理员访问 ADMIN 接口 | 检查用户的 role 字段是否为 1 |
| "文件大小超过限制（最大50MB）" | 上传文件过大 | 检查 `spring.servlet.multipart.max-file-size` 配置 |
| "参数验证失败" | 请求参数不符合校验规则 | 检查 @Valid 注解的 DTO 字段约束 |
| "系统异常，请稍后重试" | 未捕获的异常 | 查看日志文件 `./logs/app-text.log` |
| "文件读取失败" | 文件不存在或权限问题 | 检查 `./uploads` 目录和文件路径 |
| "运维 Token 校验失败" | X-Ops-Token 缺失或错误 | 请求头需包含 `X-Ops-Token: ops-secret-token-2024` |

### 5.4 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

---

## 6. 关键配置

### 6.1 应用配置（application.yml）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | 8080 | 服务端口 |
| `spring.datasource.url` | jdbc:mysql://localhost:3306/example_db | 数据库连接 |
| `spring.datasource.username` | root | 数据库用户名 |
| `spring.datasource.password` | 1234 | 数据库密码 |
| `spring.jpa.hibernate.ddl-auto` | update | 自动更新表结构 |
| `spring.servlet.multipart.max-file-size` | 50MB | 上传文件大小限制 |
| `file.upload-dir` | ./uploads | 文件上传目录 |
| `jwt.secret` | resource-sharing-platform-secret-key-2024 | JWT 签名密钥 |
| `jwt.expiration` | 86400000 | JWT 有效期（毫秒） |
| `ops.token` | ops-secret-token-2024 | 运维接口 Token |

### 6.2 日志配置

- 日志框架：Logback（通过 `logback-spring.xml` 配置）
- JSON 日志：`./logs/app.log`（供 Agent 读取）
- 文本日志：`./logs/app-text.log`（人类可读）
- 日志滚动：单文件最大 10MB，保留 30 天
- traceId：每个请求自动生成 12 位唯一标识，注入 MDC

### 6.3 默认数据（DataInitializer 自动创建）

- 管理员账号：`admin` / `admin123`（role=1）
- 5 个资料分类：图片资料、文档资料、视频资料、代码资料、其他资料
- 5 个竞赛类型：程序设计竞赛、数学建模竞赛、电子设计竞赛、创新创业竞赛、其他竞赛

---

## 7. 文件存储结构

```
./uploads/
├── 2026/
│   ├── 05/
│   │   ── 21/
│   │       └── {UUID}_{原始文件名}
│   └── 06/
│       └── 17/
│           └── {UUID}_{原始文件名}
```

- 文件按日期分目录存储：`./uploads/{yyyy}/{MM}/{dd}/`
- 文件名格式：`{UUID}_{原始文件名}`（防止重名）
- 数据库中 `file_path` 存储相对路径（如 `2026/06/17/abc123_图片.jpg`）

---

## 8. 安全机制

### 8.1 认证流程

```
请求 → JwtAuthenticationFilter → 解析 Authorization: Bearer {token}
    → 验证 JWT 签名和有效期
    → 从数据库加载用户信息
    → 设置 SecurityContext
    → 进入 Controller
```

### 8.2 授权规则

| 路径模式 | 权限要求 |
|---------|---------|
| `/api/auth/**` | 无需认证 |
| `/api/public/**` | 无需认证 |
| `/api/resources` (GET) | 无需认证 |
| `/api/resources/{id}` (GET) | 无需认证 |
| `/api/resources/download/**` (GET) | 无需认证 |
| `/api/resources/preview/**` (GET) | 无需认证 |
| `/api/resources/upload` (POST) | 需登录 |
| `/api/resources` (DELETE) | 需 ADMIN 角色 |
| `/api/favorites/**` | 需登录 |
| `/api/admin/**` | 需 ADMIN 角色 |
| `/actuator/**` | 无需认证（监控端点） |

### 8.3 CORS 配置

- 允许所有来源（`*`）
- 允许方法：GET, POST, PUT, DELETE, OPTIONS
- 暴露响应头：Authorization

---

## 9. 故障排查指南（供 Agent 参考）

### 9.1 常见问题与排查路径

| 问题 | 排查步骤 |
|------|---------|
| 用户无法登录 | 1. 检查 MySQL 是否运行 2. 查看 `./logs/app-text.log` 中的错误 3. 检查 users 表中用户状态 |
| 资料上传失败 | 1. 检查 `./uploads` 目录权限 2. 检查文件大小是否超限 3. 查看磁盘空间 |
| 下载/预览报 500 | 1. 检查文件是否存在于 `./uploads` 2. 检查 file_path 字段是否正确 3. 查看日志中的堆栈信息 |
| 系统响应慢 | 1. 调用 `/api/admin/ops/jvm` 检查内存和 GC 2. 调用 `/api/admin/ops/connection-pool` 检查连接池 3. 检查是否有故障注入 |
| 数据库连接失败 | 1. 调用 `/actuator/health` 检查数据库状态 2. 检查 MySQL 服务 3. 检查 application.yml 配置 |
| 收藏功能异常 | 1. 检查 favorites 表唯一约束 2. 确认 @Transactional 注解存在 3. 检查用户登录状态 |

### 9.2 可用修复操作

| 操作 | API | 说明 |
|------|-----|------|
| 清理缓存 | `POST /api/admin/ops/cache/evict` | 清理 EntityManager 缓存 |
| 查看日志 | `GET /api/admin/ops/logs?level=ERROR&limit=50` | 读取最近错误日志 |
| 检查健康 | `GET /api/admin/ops/health` | 综合健康检查 |
| 检查连接池 | `GET /api/admin/ops/connection-pool` | HikariCP 状态 |
| 检查 JVM | `GET /api/admin/ops/jvm` | JVM 内存/GC/线程 |
| 检查磁盘 | `GET /api/admin/ops/disk` | 磁盘使用情况 |

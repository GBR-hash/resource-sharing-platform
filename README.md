# 资料共享平台 - 部署说明

## 项目简介

资料共享平台是一个基于 Spring Boot + Vue 3 的竞赛项目资料共享与成果收集系统，支持资料上传、审核、下载、收藏等功能。

## 环境要求

- **Java 17** 或更高版本
- **MySQL 8.0** 数据库

## 部署文件

```
部署目录/
├── resource-sharing-platform-1.0.0.jar   # 前后端合一的 JAR 包
├── application.yml                       # 配置文件（需修改数据库信息）
└── uploads/                              # 用户上传的文件存储目录
```

## 部署步骤

### 1. 创建数据库

登录 MySQL，执行以下 SQL 创建空数据库：

```sql
CREATE DATABASE example_db CHARACTER SET utf8mb4;
```

### 2. 修改配置文件

编辑 `application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/example_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root        # 改为你的 MySQL 用户名
    password: 1234        # 改为你的 MySQL 密码
```

### 3. 启动应用

确保 `application.yml` 和 `uploads/` 文件夹与 JAR 包在同一目录下，然后在当前目录下cmd窗口运行：

```
java -jar resource-sharing-platform-1.0.0.jar
```

### 4. 访问系统

浏览器打开：

```
http://localhost:8080
```

## 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |

## 功能说明

- **资料浏览**：首页展示所有已发布的资料，支持搜索、分类筛选、时间筛选、下载量/收藏量排序
- **资料上传**：登录后上传资料，需管理员审核通过后才能被其他用户下载
- **资料收藏**：登录后可以收藏感兴趣的资料
- **资料下载**：已发布的资料可免费下载，下载量自动统计
- **管理员后台**：资料审核、用户管理、数据统计

## 注意事项

1. `uploads/` 文件夹必须与 JAR 包放在同一目录，否则上传的文件无法访问
2. 首次启动会自动创建数据库表结构和初始数据（管理员账号、分类、竞赛类型）
3. 如果端口 8080 被占用，可在 `application.yml` 中修改 `server.port`

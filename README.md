# 云享智维 - 资料共享平台

一站式资料管理与共享平台，支持文件上传、预览、下载，集成 OpsAgent 运维助手。

## 公网地址

🔗 http://101.37.118.247/resource/

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java Spring Boot 2.x (:8080) + Python FastAPI OpsAgent (:8002) |
| 前端 | Vue 3 + Element Plus + Vite |
| 数据库 | MySQL (example_db) |
| 存储 | 阿里云 OSS |

## 本地部署

### 环境要求

- JDK 17+
- Python 3.12+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

### 1. 克隆项目

```bash
git clone https://github.com/GBR-hash/resource-sharing-platform.git
cd resource-sharing-platform
```

### 2. 数据库初始化

```sql
CREATE DATABASE IF NOT EXISTS example_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Java 后端

```bash
# 修改 src/main/resources/application.yml 中的数据库配置
mvn clean package -DskipTests
java -jar target/resource-sharing-platform-1.0.0.jar
```

### 4. OpsAgent 后端

```bash
# 使用共享虚拟环境
python -m venv venv
source venv/bin/activate
pip install -r OpsAgent/requirements.txt

# 配置 OpsAgent/.env
cd OpsAgent
uvicorn app.main:app --host 0.0.0.0 --port 8002
```

### 5. 前端

```bash
cd fronted/fronted
npm install
npm run dev
```

访问 http://localhost:5173

## 生产部署

```bash
# 前端构建
cd fronted/fronted && npm run build

# 服务管理 (systemd)
sudo systemctl start resource-java  # Java 后端 :8080
sudo systemctl start ops-agent      # OpsAgent :8002

# Nginx 配置示例
# /resource/ → 前端静态文件
# /api/      → proxy_pass :8080
# /ops-api/  → proxy_pass :8002
```

## 项目结构

```
resource-sharing-platform/
├── src/                    # Spring Boot 源码
├── OpsAgent/               # Python 运维助手
│   └── app/
│       └── main.py
├── fronted/fronted/        # Vue3 前端
│   └── src/
│       ├── views/          # 页面组件
│       ├── router/         # 路由配置
│       └── utils/          # 工具函数
└── uploads/                # 本地文件存储
```


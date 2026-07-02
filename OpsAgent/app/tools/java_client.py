import os
import httpx
from dotenv import load_dotenv
from typing import Optional, Dict, Any

# 1. 加载环境变量 (自动寻找 .env 文件)
load_dotenv()


class JavaClient:
    def __init__(self):
        # 2. 从环境变量读取配置，如果没读到则使用默认值或抛出异常
        self.base_url = os.getenv("JAVA_BASE_URL", "http://localhost:8080")
        self.ops_token = os.getenv("OPS_TOKEN", "")
        self.jwt_token = os.getenv("JWT_TOKEN", "")

        if not self.ops_token:
            print("[WARNING] 未找到 OPS_TOKEN 环境变量，鉴权请求可能会失败！")

        if not self.jwt_token:
            print("[WARNING] 未找到 JWT_TOKEN 环境变量，将尝试自动登录获取...")

        # 3. 创建异步客户端
        # 注意：这里不设置全局 timeout，根据具体接口需求动态调整
        self.client = httpx.AsyncClient(
            base_url=self.base_url.rstrip('/'),
            headers={
                "Content-Type": "application/json"
            }
        )

    async def close(self):
        """关闭客户端连接"""
        await self.client.aclose()

    async def _ensure_jwt_token(self):
        """确保 JWT Token 存在，如果不存在则自动登录获取"""
        if not self.jwt_token:
            await self.login()

    async def login(self, username: str = None, password: str = None) -> str:
        """
        登录获取 JWT Token
        :param username: 用户名，默认从环境变量 JAVA_USERNAME 读取
        :param password: 密码，默认从环境变量 JAVA_PASSWORD 读取
        :return: JWT Token
        """
        username = username or os.getenv("JAVA_USERNAME", "admin")
        password = password or os.getenv("JAVA_PASSWORD", "admin123")
        resp = await self.client.post(
            "/api/auth/login",
            json={"username": username, "password": password}
        )
        data = resp.json()
        # 返回格式: {"code": 200, "message": "登录成功", "data": {"token": "xxx", "user": {...}}}
        token = None
        if isinstance(data, dict):
            # 优先从嵌套的 data 中取
            inner = data.get("data", {})
            if isinstance(inner, dict):
                token = inner.get("token")
            # 兜底：顶层直接有 token
            if not token:
                token = data.get("token")
        if token:
            self.jwt_token = token
            print("[INFO] 登录成功，获取到 JWT Token")
            return self.jwt_token
        else:
            raise Exception(f"登录失败: {data}")

    def _get_auth_headers(self) -> Dict[str, str]:
        """获取包含 JWT 和 OPS Token 的认证头"""
        headers = {}
        if self.jwt_token:
            headers["Authorization"] = f"Bearer {self.jwt_token}"
        if self.ops_token:
            headers["X-Ops-Token"] = self.ops_token
        return headers

    # ================= 运维控制接口 (需 Token) =================

    async def get_health(self) -> Dict:
        """获取系统健康状态"""
        await self._ensure_jwt_token()
        resp = await self.client.get("/api/admin/ops/health", headers=self._get_auth_headers())
        return resp.json()

    async def get_connection_pool(self) -> Dict:
        """获取数据库连接池状态"""
        await self._ensure_jwt_token()
        resp = await self.client.get("/api/admin/ops/connection-pool", headers=self._get_auth_headers())
        return resp.json()

    async def get_jvm_info(self) -> Dict:
        """获取 JVM 信息"""
        await self._ensure_jwt_token()
        resp = await self.client.get("/api/admin/ops/jvm", headers=self._get_auth_headers())
        return resp.json()

    async def get_disk_usage(self) -> Dict:
        """获取磁盘使用情况"""
        await self._ensure_jwt_token()
        resp = await self.client.get("/api/admin/ops/disk", headers=self._get_auth_headers())
        return resp.json()

    async def evict_cache(self) -> Dict:
        """清理缓存"""
        await self._ensure_jwt_token()
        resp = await self.client.post("/api/admin/ops/cache/evict", headers=self._get_auth_headers())
        return resp.json()

    async def reload_service(self) -> Dict:
        """服务重载"""
        await self._ensure_jwt_token()
        resp = await self.client.post("/api/admin/ops/service/reload", headers=self._get_auth_headers())
        return resp.json()

    async def get_logs(self, level: str = "ERROR", lines: int = 50) -> str:
        """
        查询日志
        :param level: 日志级别 (INFO, WARN, ERROR)
        :param lines: 获取行数
        """
        await self._ensure_jwt_token()
        resp = await self.client.get(
            "/api/admin/ops/logs",
            params={"level": level, "lines": lines},
            headers=self._get_auth_headers()
        )
        # 假设日志接口返回的是纯文本或大段 JSON，这里直接返回文本以便分析
        return resp.text

    # ================= 故障注入接口 (需 Token) =================

    async def trigger_chaos(self, chaos_type: str) -> Dict:
        """
        触发故障
        :param chaos_type: latency, exception, cpu_high
        """
        await self._ensure_jwt_token()
        resp = await self.client.post(
            "/api/admin/chaos/trigger",
            params={"type": chaos_type},
            headers=self._get_auth_headers()
        )
        return resp.json()

    async def reset_chaos(self) -> Dict:
        """清除所有故障"""
        await self._ensure_jwt_token()
        resp = await self.client.post("/api/admin/chaos/reset", headers=self._get_auth_headers())
        return resp.json()

    async def get_chaos_status(self) -> Dict:
        """查看当前故障状态"""
        await self._ensure_jwt_token()
        resp = await self.client.get("/api/admin/chaos/status", headers=self._get_auth_headers())
        return resp.json()

    # ================= Actuator 监控接口 (无需 Token) =================
    # 注意：Actuator 通常不需要鉴权，所以我们需要绕过默认的 Header
    # 或者创建一个不带 Auth Header 的临时请求

    async def get_actuator_prometheus(self) -> str:
        """获取 Prometheus 格式指标 (用于绘图/趋势分析)"""
        # 显式移除 Token，防止被后端拦截（虽然通常 Actuator 是放行的，但为了保险）
        resp = await self.client.get("/actuator/prometheus", headers={"X-Ops-Token": ""})
        return resp.text

    async def get_actuator_health(self) -> Dict:
        """Actuator 基础健康检查"""
        resp = await self.client.get("/actuator/health")
        return resp.json()

    async def get_actuator_metrics(self) -> Dict:
        """获取可用指标列表"""
        resp = await self.client.get("/actuator/metrics")
        return resp.json()

    async def get_thread_dump(self) -> str:
        """获取线程转储"""
        resp = await self.client.get("/actuator/threaddump")
        return resp.text

    async def get_heap_dump(self) -> bytes:
        """获取堆转储 (二进制流)"""
        resp = await self.client.get("/actuator/heapdump")
        return resp.content

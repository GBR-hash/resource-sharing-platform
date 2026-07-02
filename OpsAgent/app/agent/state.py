from typing import TypedDict, Annotated, List, Optional, Literal
from langgraph.graph.message import add_messages


class OpsAgentState(TypedDict):
    # ── 消息历史 ─────────────────────────────────────────────
    messages: Annotated[List, add_messages]

    # ── 用户意图 ─────────────────────────────────────────────
    user_query: str                     # 用户的原始问题
    intent: Optional[str]               # 意图分类: diagnose / action / query / chat
    intent_confidence: Optional[float]  # 意图置信度

    # ── 工作流控制 ───────────────────────────────────────────
    current_phase: Optional[str]        # 当前阶段: routing / rag / data_collect / diagnosis / action / verify / human_approval / fallback
    retry_count: int                    # 当前循环重试次数 (防止无限循环)
    max_retries: int                    # 最大重试次数
    needs_more_data: bool               # 是否需要更多数据 (触发反思循环)
    fix_verified: Optional[bool]        # 修复是否验证通过

    # ── 观测数据 (来自 Java Client) ──────────────────────────
    jvm_data: Optional[dict]            # JVM 监控数据
    connection_pool_data: Optional[dict]# 连接池数据
    disk_data: Optional[dict]           # 磁盘数据
    health_data: Optional[dict]         # 健康检查数据
    logs_data: Optional[str]            # 日志内容
    actuator_metrics: Optional[str]     # Actuator 指标
    chaos_status: Optional[str]         # 混沌工程状态

    # ── 知识增强 (来自 RAG) ──────────────────────────────────
    rag_context: Optional[str]          # 检索到的知识库内容
    rag_relevant: bool                  # RAG 是否返回了相关内容
    rag_llm_prompt: Optional[str]       # 若需要 RAG，则给出LLM认为需要检索的问题提示，用于检索知识库

    # ── 修复动作 ─────────────────────────────────────────────
    proposed_action: Optional[str]      # 建议的修复动作
    action_approved: Optional[bool]     # 人工是否批准
    action_result: Optional[str]        # 动作执行结果

    # ── 错误处理 ─────────────────────────────────────────────
    last_error: Optional[str]           # 最后一次错误信息
    fallback_triggered: bool            # 是否触发了降级处理

    # ─ 最终输出 ─────────────────────────────────────────────
    final_response: str                 # 给用户的最终回复

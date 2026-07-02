import os
import json
import asyncio
import logging
from typing import Optional, Dict, Any, AsyncGenerator, List
from dotenv import load_dotenv
from pydantic import BaseModel, Field


from langchain_core.messages import HumanMessage, SystemMessage, AIMessage

from langchain_core.tools import tool
from langchain_deepseek import ChatDeepSeek
from langgraph.graph import StateGraph, START, END
from langgraph.types import interrupt, Command
from langgraph.checkpoint.memory import MemorySaver
from langgraph.errors import GraphInterrupt
from app.tools.java_client import JavaClient
from app.tools.rag_retriever import KnowledgeRetriever
from app.agent.state import OpsAgentState

load_dotenv()

# 配置日志 - 实时输出
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    datefmt='%H:%M:%S',
    force=True
)
logger = logging.getLogger(__name__)

# ── 全局实例 ──────────────────────────────────────────────
java_client = JavaClient()
retriever = KnowledgeRetriever()

# ── 基础 LLM ──────────────────────────────────────────────
base_llm = ChatDeepSeek(
    model="deepseek-chat",
    api_key=os.getenv("DEEPSEEK_API_KEY"),
    temperature=0.1,
)


# ═══════════════════════════════════════════════════════════
# Pydantic 输出模型
# ═══════════════════════════════════════════════════════════

class RouterOutput(BaseModel):
    """Router 节点的结构化输出"""
    intent: str = Field(description="意图分类：chat, diagnose, query, action")
    needs_rag: bool = Field(description="是否需要检索知识库")
    query_piece: Optional[str] = Field(default=None, description="如果需要 RAG，给出LLM认为需要检索的问题提示，用于检索知识库")
    proposed_action: Optional[str] = Field(default=None, description="如果需要修复，建议的修复动作名称")

class ToolCall(BaseModel):
    """工具调用信息"""
    name: str = Field(description="工具名称")
    args: Dict[str, Any] = Field(default_factory=dict, description="工具参数")


class CheckOutput(BaseModel):
    """SystemCheck 节点的结构化输出"""
    tools_to_call: List[ToolCall] = Field(description="需要调用的工具列表")
    reason: str = Field(description="选择这些工具的原因")

# ═══════════════════════════════════════════════════════════
# 工具定义（使用 @tool 装饰器）
# ═══════════════════════════════════════════════════════════

@tool
async def get_health() -> dict:
    """获取系统健康状态"""
    return await java_client.get_health()


@tool
async def get_jvm_info() -> dict:
    """获取 JVM 内存、GC、线程信息"""
    return await java_client.get_jvm_info()


@tool
async def get_connection_pool() -> dict:
    """获取数据库连接池状态"""
    return await java_client.get_connection_pool()


@tool
async def get_disk_usage() -> dict:
    """获取磁盘使用情况"""
    return await java_client.get_disk_usage()


@tool
async def get_logs(level: str = "ERROR", lines: int = 50) -> str:
    """获取日志内容

    Args:
        level: 日志级别 (ERROR, WARN, INFO, DEBUG)
        lines: 返回行数
    """
    return await java_client.get_logs(level=level, lines=lines)


@tool
async def get_actuator_health() -> dict:
    """获取 Actuator 健康检查"""
    return await java_client.get_actuator_health()


@tool
async def get_actuator_metrics() -> str:
    """获取 Actuator 指标列表"""
    return await java_client.get_actuator_metrics()


@tool
async def get_chaos_status() -> dict:
    """获取混沌工程故障状态"""
    return await java_client.get_chaos_status()


@tool
async def evict_cache() -> dict:
    """清理缓存"""
    return await java_client.evict_cache()


@tool
async def reload_service() -> dict:
    """重载服务"""
    return await java_client.reload_service()


@tool
async def trigger_chaos(chaos_type: str = "latency") -> dict:
    """触发混沌工程故障

    Args:
        chaos_type: 故障类型 (latency, exception, cpu)
    """
    return await java_client.trigger_chaos(chaos_type=chaos_type)


@tool
async def reset_chaos() -> dict:
    """清除所有混沌工程故障"""
    return await java_client.reset_chaos()


# 工具列表
ALL_TOOLS = [
    get_health,
    get_jvm_info,
    get_connection_pool,
    get_disk_usage,
    get_logs,
    get_actuator_health,
    get_actuator_metrics,
    get_chaos_status,
    evict_cache,
    reload_service,
    trigger_chaos,
    reset_chaos,
]

# 工具名称到函数的映射
TOOL_MAP = {t.name: t for t in ALL_TOOLS}


# ═══════════════════════════════════════════════════════════
# Prompts
# ═══════════════════════════════════════════════════════════

ROUTER_PROMPT = """分析用户问题，分类意图。
        - chat: 闲聊、问候
        - diagnose: 诊断、排查、分析故障（如"系统为什么慢"、"查看错误日志"）
        - query: 查询系统状态（如"系统健康吗"、"连接池状态"、“JVM 内存、GC、线程信息”）
        - action: 执行运维操作（如"清理缓存"、"重启服务"、"注入故障"、"清除故障"）
                """

CHECK_PROMPT = """你是运维数据采集器。根据用户问题和已有数据，决定需要采集哪些系统指标。

        ## 可用工具
        - get_health: 系统健康状态
        - get_jvm_info: JVM 内存、GC、线程
        - get_connection_pool: 数据库连接池
        - get_disk_usage: 磁盘使用
        - get_logs: 错误日志（参数：level, lines）
        - get_actuator_health: Actuator 健康检查
        - get_actuator_metrics: Actuator 指标列表
        - get_chaos_status: 混沌工程状态

        只返回需要调用的工具列表和原因。
                """

RESPONSE_PROMPT = """你是 OpsAgent 智能运维助手。根据分析结果给用户一个清晰的中文回复。
        - 先给结论，再给分析过程
        - 使用 Markdown 格式
        - 简洁专业"""


# ═══════════════════════════════════════════════════════════
# 节点 A: RouterNode — 意图识别
# ══════════════════════════════════════════════════════════

async def router_node(state: OpsAgentState) -> dict:
    """意图识别，决定后续走哪条路径"""
    logger.info("[A-Router] 识别意图...")

    msgs = [
        SystemMessage(content=ROUTER_PROMPT),
        HumanMessage(content=state["user_query"]),
    ]
    result: RouterOutput = await base_llm.bind_tools(ALL_TOOLS).with_structured_output(RouterOutput).ainvoke(msgs)

    logger.info(f"[A-Router] intent={result.intent}, needs_rag={result.needs_rag}, proposed_action={result.proposed_action}")

    return {
        "intent": result.intent,
        "rag_relevant": result.needs_rag,
        "rag_llm_prompt": result.query_piece,
        "proposed_action": result.proposed_action,
        "current_phase": "routing",
    }


def route_after_router(state: OpsAgentState) -> str:
    # 路由到不同的节点
    # chat: 闲聊、问候
    # diagnose: 诊断、排查、分析故障(需要 RAG)
    # query: 查询系统状态
    # action: 执行运维操作
    intent = state.get("intent", "chat")
    if intent == "chat":
        return "respond"
    elif intent == "diagnose":
        return "rag"
    elif intent == "query":
        return "system_check"
    elif intent == "action":
        return "action"


# ═══════════════════════════════════════════════════════════
# 节点 B: RAGNode — 按需知识检索
# ══════════════════════════════════════════════════════════

async def rag_node(state: OpsAgentState) -> dict:
    """按需检索知识库，只在需要时调用"""
    needs_rag = state.get("rag_relevant", False)
    if not needs_rag:
        logger.info("[B-RAG] 跳过（不需要知识库）")
        return {"rag_context": None, "rag_relevant": False, "current_phase": "rag_skip"}

    logger.info("[B-RAG] 检索知识库...")
    # 调用检索器的时候，问题不应该只有用户输入，还要输入LLM认为需要检索的问题
    context = retriever.search(state["rag_llm_prompt"])

    rag_relevant = bool(
        context and "No relevant" not in context and "No knowledge" not in context
    )
    if rag_relevant:
        logger.info(f"[B-RAG] 找到相关知识 ({len(context)} 字符)")
    else:
        logger.info("[B-RAG] 未找到相关知识")
        context = None

    return {
        "rag_context": context,
        "rag_relevant": rag_relevant,
        "current_phase": "rag",
    }

# ══════════════════════════════════════════════════════════
# 节点 C: SystemCheckNode — 系统状态检查 + 诊断
# ══════════════════════════════════════════════════════════

async def system_check_node(state: OpsAgentState) -> dict:
    """
    1. LLM 决定调用哪些 tool 采集数据
    2. 并行执行 tool 调用
    3. 将采集结果写入 state，然后直接返回给 respond 节点
    """
    intent = state.get("intent", "query")
    logger.info(f"[C-SystemCheck] 意图={intent}")

    # ─ 第一步：LLM 决定采集哪些数据 ──
    check_prompt_parts = [f"用户问题：{state['user_query']}"]
    if state.get("rag_context"):
        check_prompt_parts.append(f"知识库参考：{state['rag_context'][:500]}")

    # 告知已有数据，避免重复采集
    existing = []
    if state.get("health_data"):
        existing.append("health_data")
    if state.get("jvm_data"):
        existing.append("jvm_data")
    if state.get("logs_data"):
        existing.append("logs_data")
    if existing:
        check_prompt_parts.append(f"已有数据：{', '.join(existing)}")

    check_msgs = [
        SystemMessage(content=CHECK_PROMPT),
        HumanMessage(content="\n".join(check_prompt_parts)),
    ]
    # 使用 bind_tools 让 LLM 知道可用工具，然后结构化输出工具列表
    check_result: CheckOutput = await base_llm.bind_tools(ALL_TOOLS).with_structured_output(CheckOutput).ainvoke(check_msgs)

    tools_to_call = check_result.tools_to_call
    logger.info(f"[C-SystemCheck] 计划调用 {len(tools_to_call)} 个工具")

    # ── 第二步：并行执行 tool 调用 ──
    results = {}
    if tools_to_call:
        tasks = []
        for tool_call in tools_to_call:
            name = tool_call.name
            args = tool_call.args
            tool_func = TOOL_MAP.get(name)
            if tool_func:
                tasks.append((name, tool_func.ainvoke(args) if args else tool_func.ainvoke({})))
            else:
                logger.warning(f"[C-SystemCheck] 未知工具：{name}")

        if tasks:
            task_results = await asyncio.gather(
                *[t[1] for t in tasks], return_exceptions=True
            )
            for (name, _), result in zip(tasks, task_results):
                if isinstance(result, Exception):
                    logger.error(f"[C-SystemCheck] {name} 失败：{result}")
                    results[name] = f"ERROR: {result}"
                else:
                    results[name] = (
                        json.dumps(result, ensure_ascii=False, indent=2)[:1000]
                        if isinstance(result, (dict, list))
                        else str(result)[:1000]
                    )
                    logger.info(f"[C-SystemCheck] {name} 成功")

    # 将采集结果写入 state
    state_updates = {}
    for name, value in results.items():
        key = name if name.endswith("_data") else f"{name}_result"
        state_updates[key] = value

    # 直接返回结果，由 respond 节点统一生成回复
    logger.info(f"[C-SystemCheck] 数据采集完成，共 {len(results)} 个结果")
    return {
        **state_updates,
        "current_phase": "check_done",
    }


# ═══════════════════════════════════════════════════════════
# 节点 D: ActionNode — 自修复（interrupt 人工审批）
# ══════════════════════════════════════════════════════════

async def action_node(state: OpsAgentState) -> dict:
    """
    判断是否需要执行运维操作。
    如果需要，路由到 execute_action 节点。
    """
    proposed_action = state.get("proposed_action", "")
    logger.info(f"[Action] 待执行操作：{proposed_action}")

    if not proposed_action:
        logger.info("[Action] 无操作需要执行")
        return {"action_result": "无需执行操作", "current_phase": "fix_skip"}

    # 返回操作信息，由条件边路由到 execute_action
    return {
        "proposed_action": proposed_action,
        "action_approved": None,  # None 表示待审批
        "current_phase": "action_pending",
    }


def route_after_action(state: OpsAgentState) -> str:
    """根据是否需要执行操作，路由到不同节点"""
    proposed_action = state.get("proposed_action", "")
    if proposed_action:
        return "execute_action"
    else:
        return "respond"


async def execute_action_node(state: OpsAgentState) -> dict:
    """
    执行修复操作（使用原生 interrupt 机制）。
    - 首次执行：调用 interrupt() 暂停图，等待人工审批
    - 恢复后：interrupt() 返回审批结果，继续执行操作
    """
    proposed_action = state.get("proposed_action", "")
    logger.info(f"[ExecuteAction] 操作：{proposed_action}")

    # 检查是否已经审批过（恢复时 state 会保留之前的审批状态）
    if state.get("action_approved") is not None:
        approved = state["action_approved"]
        logger.info(f"[ExecuteAction] 已恢复执行，审批状态：{approved}")
    else:
        # 首次执行：调用 interrupt() 暂停图
        logger.info(f"[ExecuteAction] 调用 interrupt()，等待人工审批...")
        approved: bool = interrupt({
            "action": proposed_action,
            "message": f"Agent 建议执行操作：{proposed_action}，是否批准？",
        })
        logger.info(f"[ExecuteAction] interrupt 返回，审批结果：{approved}")

    if not approved:
        return {
            "action_result": f"操作 {proposed_action} 被用户拒绝",
            "action_approved": False,
            "current_phase": "fix_rejected",
        }

    action_mapping = {
        "清除": "reset_chaos",
        "清除故障": "reset_chaos",
        "清除所有故障": "reset_chaos",
        "重置": "reset_chaos",
        "清理缓存": "evict_cache",
        "清理": "evict_cache",
        "缓存": "evict_cache",
        "重启": "reload_service",
        "重载": "reload_service",
        "注入延迟": "trigger_chaos",
        "注入故障": "trigger_chaos",
        "延迟": "trigger_chaos",
    }
    
    tool_func = TOOL_MAP.get(proposed_action)
    
    if not tool_func:
        for keyword, tool_name in action_mapping.items():
            if keyword in proposed_action:
                tool_func = TOOL_MAP.get(tool_name)
                if tool_func:
                    logger.info(f"[ExecuteAction] 匹配到工具：{tool_name} (关键词：{keyword})")
                    break
    
    if not tool_func:
        return {
            "action_result": f"未知操作：{proposed_action}，可用操作：{list(TOOL_MAP.keys())}",
            "action_approved": True,
            "current_phase": "fix_failed",
        }

    try:
        result = await tool_func.ainvoke({})
        result_str = (
            json.dumps(result, ensure_ascii=False, indent=2)
            if isinstance(result, (dict, list))
            else str(result)
        )
        logger.info(f"[ExecuteAction] 执行成功：{proposed_action}")
        return {
            "action_result": result_str,
            "action_approved": True,
            "current_phase": "fix_done",
        }
    except Exception as e:
        logger.error(f"[ExecuteAction] 执行失败：{e}")
        return {
            "action_result": f"执行失败：{e}",
            "action_approved": True,
            "last_error": str(e),
            "current_phase": "fix_failed",
        }


# ═══════════════════════════════════════════════════════════
# ResponseNode — 生成最终回复
# ═════════════════════════════════════════════════════════

async def respond_node(state: OpsAgentState) -> dict:
    """综合所有信息，生成最终回复"""
    logger.info("[Respond] 生成回复...")

    parts = [f"用户问题：{state['user_query']}"]

    if state.get("rag_context"):
        parts.append(f"知识库：{state['rag_context'][:500]}")
    if state.get("action_result"):
        parts.append(f"操作结果：{state['action_result']}")

    # 收集所有观测数据
    observations = []
    for key in state:
        if key.endswith("_data") or key.endswith("_status") or key.endswith("_metrics"):
            val = state[key]
            if val:
                observations.append(f"{key}: {str(val)[:300]}")
    if observations:
        parts.append("观测数据:\n" + "\n".join(observations))

    msgs = [
        SystemMessage(content=RESPONSE_PROMPT),
        HumanMessage(content="\n\n".join(parts)),
    ]
    resp = await base_llm.ainvoke(msgs)

    return {"final_response": resp.content, "current_phase": "respond"}


# ═══════════════════════════════════════════════════════════
# 构建图
# ═══════════════════════════════════════════════════════════

# 内存检查点存储（用于 interrupt/resume）
memory = MemorySaver()


def build_graph() -> StateGraph:
    graph = StateGraph(OpsAgentState)

    # ── 节点 ──
    graph.add_node("router", router_node)
    graph.add_node("rag", rag_node)
    graph.add_node("system_check", system_check_node)
    graph.add_node("action", action_node)
    graph.add_node("execute_action", execute_action_node)
    graph.add_node("respond", respond_node)

    # ── 边 ──
    graph.add_edge(START, "router")

    # Router 之后
    graph.add_conditional_edges(
        "router",
        route_after_router,
        {
            "rag": "rag",
            "action": "action",
            "respond": "respond",
            "system_check": "system_check",
        },
    )

    # RAG 之后 → SystemCheck
    graph.add_edge("rag", "system_check")

    # SystemCheck 之后直接回复
    graph.add_edge("system_check", "respond")

    # Action 之后 → 条件边路由
    graph.add_conditional_edges(
        "action",
        route_after_action,
        {
            "execute_action": "execute_action",
            "respond": "respond",
        },
    )

    # ExecuteAction 之后 → 回复
    graph.add_edge("execute_action", "respond")

    # Respond → END
    graph.add_edge("respond", END)

    return graph.compile(checkpointer=memory)


agent_graph = build_graph()


# ═══════════════════════════════════════════════════════════
# 入口函数
# ══════════════════════════════════════════════════════════

def create_initial_state(user_message: str) -> OpsAgentState:
    return {
        "messages": [HumanMessage(content=user_message)],
        "user_query": user_message,
        "intent": None,
        "intent_confidence": None,
        "current_phase": None,
        "retry_count": 0,
        "max_retries": 3,
        "needs_more_data": False,
        "fix_verified": None,
        "jvm_data": None,
        "connection_pool_data": None,
        "disk_data": None,
        "health_data": None,
        "logs_data": None,
        "actuator_metrics": None,
        "chaos_status": None,
        "rag_context": None,
        "rag_relevant": False,
        "proposed_action": None,
        "action_approved": None,
        "action_result": None,
        "last_error": None,
        "fallback_triggered": False,
        "final_response": "",
    }


async def _yield_phase_event(event: dict) -> AsyncGenerator[dict, None]:
    """根据 current_phase 输出阶段事件（公共函数）"""
    current_phase = event.get("current_phase")
    if current_phase == "routing":
        yield {
            "type": "phase",
            "phase": "routing",
            "data": {
                "intent": event.get("intent"),
                "needs_rag": event.get("rag_relevant", False),
            },
        }
    elif current_phase == "rag":
        yield {
            "type": "phase",
            "phase": "rag",
            "data": {
                "found": event.get("rag_relevant", False),
                "length": len(event.get("rag_context", "") or ""),
            },
        }
    elif current_phase == "check_done":
        yield {
            "type": "phase",
            "phase": "diagnosis",
            "data": {"conclusion": "数据采集完成"},
        }
    elif current_phase == "fix_done":
        yield {
            "type": "phase",
            "phase": "fix",
            "data": {
                "action": event.get("proposed_action", ""),
                "result": event.get("action_result", ""),
                "approved": True,
            },
        }
    elif current_phase == "fix_rejected":
        yield {
            "type": "phase",
            "phase": "fix",
            "data": {
                "action": event.get("proposed_action", ""),
                "result": "用户拒绝执行",
                "approved": False,
            },
        }


async def _yield_final_response(final_state: dict) -> AsyncGenerator[dict, None]:
    """将 final_response 逐字符流式输出（公共函数）"""
    if final_state:
        final_response = final_state.get("final_response", "")
        if final_response:
            for char in final_response:
                yield {"type": "token", "data": char}
            yield {"type": "response_done", "data": final_response}


async def run_agent_stream(user_message: str, thread_id: str) -> AsyncGenerator[dict, None]:
    """
    流式运行 Agent，逐步产出事件（使用原生 interrupt 机制）。
    yield 格式：{"type": "...", "data": ...}

    流程:
    1. 用 astream() 执行图
    2. 实时推送阶段事件
    3. astream 结束后，检查是否被 interrupt 中断
    4. 如果中断，yield approval_needed 并停止
    5. 图执行完成后，流式生成最终回复
    """
    initial_state = create_initial_state(user_message)
    config = {"configurable": {"thread_id": thread_id}}

    try:
        final_state = None
        
        async for event in agent_graph.astream(initial_state, config=config, stream_mode="values"):
            final_state = event
            async for phase_event in _yield_phase_event(event):
                yield phase_event

        snapshot = await agent_graph.aget_state(config)
        
        if snapshot.next:
            logger.info(f"[run_agent_stream] snapshot.next: {snapshot.next}, values: {snapshot.values.get('proposed_action', 'N/A')}")
            
            if 'execute_action' in str(snapshot.next):
                proposed_action = snapshot.values.get("proposed_action", "")
                if proposed_action:
                    yield {
                        "type": "approval_needed",
                        "data": {
                            "thread_id": thread_id,
                            "action": proposed_action,
                            "message": f"Agent 建议执行操作：{proposed_action}，是否批准？",
                        },
                    }
                    return

        async for response_event in _yield_final_response(final_state):
            yield response_event

    except Exception as e:
        logger.error(f"[run_agent_stream] 异常：{e}")
        yield {"type": "error", "data": str(e)}


async def run_agent_resume(thread_id: str, approved: bool) -> AsyncGenerator[dict, None]:
    """
    使用原生 Command(resume=approved) 恢复被 interrupt 暂停的图执行。
    图会从断点（execute_action_node）继续执行到 END。
    """
    config = {"configurable": {"thread_id": thread_id}}

    try:
        logger.info(f"[run_agent_resume] 恢复执行，approved={approved}")
        
        final_state = None
        
        async for event in agent_graph.astream(
            Command(resume=approved),
            config=config,
            stream_mode="values",
        ):
            final_state = event
            async for phase_event in _yield_phase_event(event):
                yield phase_event
        
        async for response_event in _yield_final_response(final_state):
            yield response_event
        
        logger.info(f"[run_agent_resume] 恢复完成，approved={approved}")
        
    except Exception as e:
        logger.error(f"[run_agent_resume] 异常：{e}")
        yield {"type": "error", "data": str(e)}

import streamlit as st
import asyncio
import httpx
import json
import uuid

st.set_page_config(
    page_title="OpsAgent - 智能运维助手",
    page_icon="🤖",
    layout="wide",
)

API_BASE = "http://localhost:8000"

if "messages" not in st.session_state:
    st.session_state.messages = []
if "thread_id" not in st.session_state:
    st.session_state.thread_id = str(uuid.uuid4())
if "pending_approval" not in st.session_state:
    st.session_state.pending_approval = None


def _phase_label(phase: str, data: dict) -> str:
    if phase == "routing":
        intent = data.get("intent", "?")
        rag = " +RAG" if data.get("needs_rag") else ""
        return f"意图识别：{intent}{rag}"
    elif phase == "rag":
        found = data.get("found", False)
        length = data.get("length", 0)
        return f"知识检索：{'找到' if found else '未找到'} ({length} 字符)"
    elif phase == "diagnosis":
        conclusion = data.get("conclusion", "")
        return f"诊断分析：{conclusion[:60]}..." if conclusion else "诊断分析中..."
    elif phase == "fix":
        action = data.get("action", "")
        approved = data.get("approved")
        if approved is None:
            return f"等待审批：{action}"
        elif approved:
            return f"执行修复：{action} ✓"
        else:
            return f"修复被拒绝：{action} ✗"
    return phase


async def _process_stream(user_input: str, response_container, phases_container):
    phases_log = []
    result_holder = {"full_response": ""}
    approval_holder = {"needed": False, "data": None}

    async with httpx.AsyncClient(timeout=300.0) as client:
        async with client.stream(
            "POST",
            f"{API_BASE}/chat/stream",
            json={"message": user_input, "thread_id": st.session_state.thread_id},
        ) as resp:
            async for line in resp.aiter_lines():
                if line.startswith("data: "):
                    data_str = line[6:]
                    if data_str.strip():
                        event = json.loads(data_str)
                        etype = event.get("type", "")

                        if etype == "thread_id":
                            st.session_state.thread_id = event.get("data", "")

                        elif etype == "phase":
                            phase = event.get("phase", "")
                            data = event.get("data", {})
                            label = _phase_label(phase, data)
                            phases_log.append(label)
                            with phases_container:
                                for p in phases_log:
                                    st.caption(f"▸ {p}")

                        elif etype == "token":
                            result_holder["full_response"] += event.get("data", "")
                            with response_container:
                                st.markdown(result_holder["full_response"])

                        elif etype == "response_done":
                            result_holder["full_response"] = event.get("data", "")

                        elif etype == "approval_needed":
                            approval_holder["needed"] = True
                            approval_holder["data"] = event.get("data", {})

                        elif etype == "error":
                            error_msg = event.get("data", "未知错误")
                            result_holder["full_response"] = f"**错误**: {error_msg}"
                            with response_container:
                                st.error(error_msg)

    return result_holder["full_response"], phases_log, approval_holder


async def _resume_approval(approved: bool, response_container, phases_container):
    """调用 /chat/resume 接口（流式）"""
    approval_data = st.session_state.pending_approval
    if not approval_data:
        return None

    thread_id = approval_data.get("thread_id", "")
    full_response = ""
    phases_log = []

    async with httpx.AsyncClient(timeout=300.0) as client:
        async with client.stream(
            "POST",
            f"{API_BASE}/chat/resume",
            json={"thread_id": thread_id, "approved": approved},
        ) as resp:
            async for line in resp.aiter_lines():
                if line.startswith("data: "):
                    data_str = line[6:]
                    if data_str.strip():
                        event = json.loads(data_str)
                        etype = event.get("type", "")

                        if etype == "phase":
                            phase = event.get("phase", "")
                            data = event.get("data", {})
                            label = _phase_label(phase, data)
                            phases_log.append(label)
                            with phases_container:
                                for p in phases_log:
                                    st.caption(f"▸ {p}")

                        elif etype == "token":
                            full_response += event.get("data", "")
                            with response_container:
                                st.markdown(full_response)

                        elif etype == "response_done":
                            full_response = event.get("data", "")

                        elif etype == "error":
                            full_response = f"**错误**: {event.get('data', '未知错误')}"
                            with response_container:
                                st.error(event.get("data", "未知错误"))

    return {"response": full_response, "phases": phases_log}


# ── 侧边栏 ──────────────────────────────────────────────
with st.sidebar:
    st.title("OpsAgent")
    st.caption("基于 LangGraph 的智能运维助手")
    st.divider()

    st.subheader("快捷操作")
    quick_actions = {
        "系统全面体检": "请对系统进行一次全面健康检查，包括 JVM、连接池、磁盘和 Actuator 状态",
        "查看错误日志": "请查看最近的 ERROR 级别日志，分析是否有异常",
        "注入延迟故障": "请触发一个 latency 类型的故障，然后观察系统状态变化",
        "清除所有故障": "请清除所有混沌工程故障，恢复系统正常状态",
        "JVM 内存分析": "请获取 JVM 信息，分析当前内存使用情况是否正常",
    }
    for label, query in quick_actions.items():
        if st.button(label, use_container_width=True):
            st.session_state.pending_message = query
            st.rerun()

    st.divider()
    if st.button("清空对话", use_container_width=True):
        st.session_state.messages = []
        st.session_state.pending_approval = None
        st.rerun()


# ── 主区域 ────────────────────────────────────────────────
st.title("OpsAgent 智能运维助手")

for msg in st.session_state.messages:
    with st.chat_message(msg["role"]):
        st.markdown(msg["content"])
        if msg["role"] == "assistant" and "phases" in msg:
            for phase in msg["phases"]:
                st.caption(f"▸ {phase}")

# ── 审批按钮区域 ──────────────────────────────────────────
if st.session_state.pending_approval:
    approval_data = st.session_state.pending_approval
    action = approval_data.get("action", "")
    message = approval_data.get("message", "需要人工审批")

    st.warning(f"**{message}**", icon="⚠️")

    col1, col2 = st.columns(2)
    with col1:
        if st.button("✅ 同意执行", use_container_width=True, key="approve_btn"):
            with st.chat_message("assistant"):
                response_container = st.empty()
                phases_container = st.empty()
                with st.spinner("执行中..."):
                    loop = asyncio.new_event_loop()
                    result = loop.run_until_complete(
                        _resume_approval(True, response_container, phases_container)
                    )
                    loop.close()

                if result:
                    st.session_state.messages.append({
                        "role": "assistant",
                        "content": f"✅ 已批准执行 **{action}**\n\n{result.get('response', '')}",
                        "phases": [f"人工审批：同意执行 {action}"] + result.get("phases", []),
                    })
                st.session_state.pending_approval = None
                st.rerun()

    with col2:
        if st.button("❌ 拒绝", use_container_width=True, key="reject_btn"):
            with st.chat_message("assistant"):
                response_container = st.empty()
                phases_container = st.empty()
                with st.spinner("处理中..."):
                    loop = asyncio.new_event_loop()
                    result = loop.run_until_complete(
                        _resume_approval(False, response_container, phases_container)
                    )
                    loop.close()

                if result:
                    st.session_state.messages.append({
                        "role": "assistant",
                        "content": f"❌ 已拒绝执行 **{action}**\n\n{result.get('response', '')}",
                        "phases": [f"人工审批：拒绝执行 {action}"] + result.get("phases", []),
                    })
                st.session_state.pending_approval = None
                st.rerun()

    st.stop()  # 暂停渲染，等待用户操作


# ── 输入框 ────────────────────────────────────────────────
pending = st.session_state.pop("pending_message", None)
user_input = st.chat_input("输入你的运维问题...") or pending

if user_input:
    st.session_state.messages.append({"role": "user", "content": user_input})

    with st.chat_message("user"):
        st.markdown(user_input)

    with st.chat_message("assistant"):
        st.info(" Agent 正在生成回复，请稍候...", icon="⏳")
        response_container = st.empty()
        phases_container = st.empty()

        try:
            loop = asyncio.new_event_loop()
            full_response, phases_log, approval_holder = loop.run_until_complete(
                _process_stream(user_input, response_container, phases_container)
            )
            loop.close()

            # 检查是否需要审批
            if approval_holder.get("needed"):
                st.session_state.pending_approval = approval_holder.get("data")
                st.session_state.messages.append({
                    "role": "assistant",
                    "content": f"⚠️ 需要人工审批：{approval_holder['data'].get('message', '')}",
                    "phases": phases_log + [f"等待审批：{approval_holder['data'].get('action', '')}"],
                })
            elif full_response:
                st.session_state.messages.append({
                    "role": "assistant",
                    "content": full_response,
                    "phases": phases_log,
                })
            else:
                with response_container:
                    st.warning("Agent 未返回有效回复")

        except httpx.ConnectError:
            error_msg = "无法连接到 OpsAgent 后端服务，请确保 FastAPI 已启动 (localhost:8000)"
            with response_container:
                st.error(error_msg)
            st.session_state.messages.append({
                "role": "assistant",
                "content": error_msg,
                "phases": [],
            })
        except Exception as e:
            error_msg = f"请求失败：{str(e)}"
            with response_container:
                st.error(error_msg)
            st.session_state.messages.append({
                "role": "assistant",
                "content": error_msg,
                "phases": [],
            })

    st.rerun()

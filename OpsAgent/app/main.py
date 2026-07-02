import json
import uuid
from fastapi import FastAPI
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from typing import Optional
from app.agent.graph import run_agent_stream, run_agent_resume

app = FastAPI(title="OpsAgent API", description="智能运维助手 API")


class ChatRequest(BaseModel):
    message: str
    thread_id: Optional[str] = None


class ResumeRequest(BaseModel):
    thread_id: str
    approved: bool


@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "OpsAgent"}


@app.post("/chat/stream")
async def chat_stream(request: ChatRequest):
    """SSE 流式对话接口"""
    thread_id = request.thread_id or str(uuid.uuid4())

    async def event_generator():
        # 先发送 thread_id
        yield f"data: {json.dumps({'type': 'thread_id', 'data': thread_id}, ensure_ascii=False)}\n\n"
        async for event in run_agent_stream(request.message, thread_id):
            yield f"data: {json.dumps(event, ensure_ascii=False)}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


@app.post("/chat/resume")
async def chat_resume(request: ResumeRequest):
    """恢复被 interrupt 暂停的工作流（人工审批）—— 流式输出"""

    async def event_generator():
        async for event in run_agent_resume(request.thread_id, request.approved):
            yield f"data: {json.dumps(event, ensure_ascii=False)}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )

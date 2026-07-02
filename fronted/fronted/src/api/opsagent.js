/**
 * OpsAgent SSE Stream API
 * 封装与 FastAPI 后端的 SSE 流式通信
 */

const API_BASE = "/ops-api";

/**
 * SSE 流式聊天 — async generator
 * @param {string} message 用户消息
 * @param {string|null} threadId 会话 ID
 * @yields {Object} SSE event { type, phase?, data, ... }
 */
export async function* streamChat(message, threadId) {
  const body = { message };
  if (threadId) body.thread_id = threadId;

  const response = await fetch(`${API_BASE}/chat/stream`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    throw new Error(`OpsAgent 请求失败 (HTTP ${response.status})`);
  }

  yield* readSSEStream(response);
}

/**
 * 恢复被中断的工作流（人工审批后继续）
 * @param {string} threadId 会话 ID
 * @param {boolean} approved 是否批准
 * @yields {Object} SSE event
 */
export async function* resumeApproval(threadId, approved) {
  const response = await fetch(`${API_BASE}/chat/resume`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ thread_id: threadId, approved }),
  });

  if (!response.ok) {
    throw new Error(`OpsAgent 恢复请求失败 (HTTP ${response.status})`);
  }

  yield* readSSEStream(response);
}

/**
 * 读取 SSE ReadableStream，逐事件 yield
 */
async function* readSSEStream(response) {
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split("\n");
      buffer = lines.pop() || "";

      for (const line of lines) {
        const event = parseSSELine(line);
        if (event) yield event;
      }
    }

    // 处理残留 buffer
    const event = parseSSELine(buffer);
    if (event) yield event;
  } finally {
    reader.releaseLock();
  }
}

/**
 * 解析单行 SSE data
 */
function parseSSELine(line) {
  if (!line.startsWith("data: ")) return null;
  const dataStr = line.slice(6).trim();
  if (!dataStr) return null;
  try {
    return JSON.parse(dataStr);
  } catch {
    return null;
  }
}

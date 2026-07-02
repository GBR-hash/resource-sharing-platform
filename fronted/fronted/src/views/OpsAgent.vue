<template>
  <div class="ops-agent">
    <!-- ===== 左侧边栏 ===== -->
    <aside class="ops-sidebar">
      <div class="sidebar-header">
        <div class="sidebar-logo">
          <el-icon :size="22"><Cpu /></el-icon>
        </div>
        <h2>OpsAgent</h2>
        <p>基于 LangGraph 的智能运维助手</p>
      </div>

      <div class="sidebar-section">
        <h3>快捷操作</h3>
        <div class="quick-actions">
          <button
            v-for="action in quickActions"
            :key="action.label"
            class="quick-action-btn"
            :disabled="isStreaming || !!pendingApproval"
            @click="sendQuickAction(action.query)"
          >
            <el-icon :size="15"><component :is="action.icon" /></el-icon>
            <span>{{ action.label }}</span>
          </button>
        </div>
      </div>

      <div class="sidebar-footer">
        <button class="clear-btn" @click="clearChat" :disabled="isStreaming">
          <el-icon><Delete /></el-icon>
          <span>清空对话</span>
        </button>
      </div>
    </aside>

    <!-- ===== 右侧聊天区 ===== -->
    <main class="ops-main">
      <div class="chat-messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="chat-empty">
          <div class="empty-icon">
            <el-icon :size="48"><Cpu /></el-icon>
          </div>
          <h3>OpsAgent 智能运维助手</h3>
          <p>输入运维问题，或点击左侧快捷操作开始对话</p>
        </div>

        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          class="message"
          :class="'message--' + msg.role"
        >
          <div class="message-avatar">
            <el-icon v-if="msg.role === 'assistant'" :size="18"><Cpu /></el-icon>
            <el-icon v-else :size="18"><User /></el-icon>
          </div>
          <div class="message-body">
            <div v-if="msg.phases && msg.phases.length" class="phase-tags">
              <span
                v-for="(p, pi) in msg.phases"
                :key="pi"
                class="phase-tag"
                :class="'phase-tag--' + p.type"
              >{{ p.label }}</span>
            </div>

            <div
              class="message-content"
              v-html="renderMarkdown(msg.content)"
            ></div>

            <div
              v-if="msg.approval && idx === pendingApprovalIndex"
              class="approval-card"
            >
              <div class="approval-alert">
                <el-icon :size="18"><WarningFilled /></el-icon>
                <span>{{ msg.approval.message || '需要人工审批' }}</span>
              </div>
              <div class="approval-actions">
                <button class="approval-btn approval-btn--accept" @click="handleApprove(true)">
                  <el-icon><Check /></el-icon> 同意执行
                </button>
                <button class="approval-btn approval-btn--reject" @click="handleApprove(false)">
                  <el-icon><Close /></el-icon> 拒绝
                </button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="isStreaming && streamingBuffer === ''" class="message message--assistant">
          <div class="message-avatar"><el-icon :size="18"><Cpu /></el-icon></div>
          <div class="message-body">
            <div class="typing-indicator"><span></span><span></span><span></span></div>
          </div>
        </div>

        <div v-if="connectionError" class="chat-error">
          <el-alert
            :title="connectionError"
            type="error"
            show-icon
            :closable="false"
          />
        </div>
      </div>

      <div class="chat-input">
        <div class="input-wrapper">
          <textarea
            ref="inputRef"
            v-model="inputText"
            class="input-textarea"
            placeholder="输入你的运维问题..."
            :disabled="isStreaming || !!pendingApproval"
            @keydown.enter.exact.prevent="sendMessage"
            rows="1"
          ></textarea>
          <button
            class="send-btn"
            :disabled="!inputText.trim() || isStreaming || !!pendingApproval"
            @click="sendMessage"
          >
            <el-icon :size="18"><Promotion /></el-icon>
          </button>
        </div>
        <p class="input-hint" v-if="pendingApproval">等待审批中，请先处理上方的审批请求</p>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, nextTick, watch, onMounted } from "vue";
import { marked } from "marked";
import {
  Cpu,
  User,
  Promotion,
  Delete,
  WarningFilled,
  Check,
  Close,
  Search,
  Document,
  VideoPlay,
  RefreshRight,
  TrendCharts,
} from "@element-plus/icons-vue";
import { streamChat, resumeApproval } from "@/api/opsagent";

marked.setOptions({ breaks: true, gfm: true });

const quickActions = [
  { label: "系统全面体检", query: "请对系统进行一次全面健康检查，包括 JVM、连接池、磁盘和 Actuator 状态", icon: "Search" },
  { label: "查看错误日志", query: "请查看最近的 ERROR 级别日志，分析是否有异常", icon: "Document" },
  { label: "注入延迟故障", query: "请触发一个 latency 类型的故障，然后观察系统状态变化", icon: "VideoPlay" },
  { label: "清除所有故障", query: "请清除所有混沌工程故障，恢复系统正常状态", icon: "RefreshRight" },
  { label: "JVM 内存分析", query: "请获取 JVM 信息，分析当前内存使用情况是否正常", icon: "TrendCharts" },
];

const messages = ref([]);
const inputText = ref("");
const isStreaming = ref(false);
const streamingBuffer = ref("");
const threadId = ref(null);
const pendingApproval = ref(null);
const pendingApprovalIndex = ref(-1);
const connectionError = ref("");
const messagesRef = ref(null);
const inputRef = ref(null);

function renderMarkdown(text) {
  if (!text) return "";
  return marked.parse(text);
}

function formatPhase(phase, data) {
  const map = {
    routing: { type: "routing", label: `意图识别：${data?.intent || "?"}${data?.needs_rag ? " +RAG" : ""}` },
    rag: { type: "rag", label: `知识检索：${data?.found ? "找到" : "未找到"} (${data?.length || 0} 字符)` },
    diagnosis: { type: "diagnosis", label: `诊断分析：${data?.conclusion || "数据采集完成"}` },
  };

  if (phase === "fix") {
    if (data?.approved === true) {
      return { type: "fix-ok", label: `执行修复：${data?.action || ""} ?` };
    } else if (data?.approved === false) {
      return { type: "fix-reject", label: `修复被拒绝：${data?.action || ""} ?` };
    }
  }

  return map[phase] || { type: "default", label: phase };
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
    }
  });
}

async function processStream(generator, targetIndex) {
  isStreaming.value = true;
  streamingBuffer.value = "";
  connectionError.value = "";

  let msg = messages.value[targetIndex];
  if (!msg) {
    msg = { role: "assistant", content: "", phases: [], approval: null };
    messages.value.push(msg);
    targetIndex = messages.value.length - 1;
  }

  try {
    for await (const event of generator) {
      switch (event.type) {
        case "thread_id":
          threadId.value = event.data;
          break;

        case "phase":
          msg.phases.push(formatPhase(event.phase, event.data));
          scrollToBottom();
          break;

        case "token":
          msg.content += event.data;
          streamingBuffer.value = msg.content;
          await new Promise(r => setTimeout(r, 10));
          break;

        case "response_done":
          msg.content = event.data;
          streamingBuffer.value = "";
          break;

        case "approval_needed":
          msg.approval = event.data;
          pendingApproval.value = event.data;
          pendingApprovalIndex.value = targetIndex;
          isStreaming.value = false;
          streamingBuffer.value = "";
          scrollToBottom();
          return;

        case "error":
          msg.content = `**错误**: ${event.data}`;
          connectionError.value = event.data;
          break;
      }
    }
  } catch (err) {
    console.error("SSE stream error:", err);
    if (err.name !== "AbortError") {
      msg.content = `**连接失败**: ${err.message || "无法连接到 OpsAgent 后端服务，请确保 FastAPI 已启动 (服务器后端)"}`;
      connectionError.value = msg.content;
    }
  } finally {
    isStreaming.value = false;
    streamingBuffer.value = "";
    scrollToBottom();
  }
}

async function sendMessage() {
  const text = inputText.value.trim();
  if (!text || isStreaming.value || pendingApproval.value) return;

  inputText.value = "";
  connectionError.value = "";

  messages.value.push({ role: "user", content: text, phases: [], approval: null });
  const aiIdx = messages.value.length;

  try {
    const gen = streamChat(text, threadId.value);
    await processStream(gen, aiIdx);
  } catch (err) {
    console.error("sendMessage error:", err);
  }
}

function sendQuickAction(query) {
  inputText.value = query;
  sendMessage();
}

async function handleApprove(approved) {
  if (!pendingApproval.value) return;

  const data = pendingApproval.value;
  const tid = data.thread_id || threadId.value;

  pendingApproval.value = null;
  const targetIdx = pendingApprovalIndex.value;
  pendingApprovalIndex.value = -1;

  if (targetIdx >= 0 && messages.value[targetIdx]) {
    messages.value[targetIdx].approval = null;
  }

  const actionLabel = data.action || "操作";
  const approvalPhase = {
    type: approved ? "fix-ok" : "fix-reject",
    label: `人工审批：${approved ? "同意执行" : "拒绝执行"} ${actionLabel}`,
  };

  const resultMsg = {
    role: "assistant",
    content: "",
    phases: [approvalPhase],
    approval: null,
  };
  messages.value.push(resultMsg);
  const resumeIdx = messages.value.length - 1;

  try {
    const gen = resumeApproval(tid, approved);
    await processStream(gen, resumeIdx);
  } catch (err) {
    console.error("resumeApproval error:", err);
  }
}

function clearChat() {
  messages.value = [];
  threadId.value = null;
  pendingApproval.value = null;
  pendingApprovalIndex.value = -1;
  connectionError.value = "";
  streamingBuffer.value = "";
}

watch(messages, () => scrollToBottom(), { deep: true });
watch(streamingBuffer, () => scrollToBottom());

onMounted(() => {
  inputRef.value?.focus();
});
</script>

<style scoped>
.ops-agent {
  display: flex;
  height: calc(100vh - 60px);
  background: #f5f7fa;
}

.ops-sidebar {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #e8eaed;
  padding: 20px 16px;
  gap: 16px;
}

.sidebar-header {
  text-align: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.sidebar-logo {
  width: 44px;
  height: 44px;
  margin: 0 auto 10px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.sidebar-header h2 {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.sidebar-header p {
  font-size: 12px;
  color: #909399;
  margin: 4px 0 0;
}

.sidebar-section h3 {
  font-size: 12px;
  font-weight: 600;
  color: #909399;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin: 0 0 8px 4px;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.quick-action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #e8eaed;
  border-radius: 8px;
  background: #fafbfc;
  font-size: 13px;
  color: #444;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}

.quick-action-btn:hover:not(:disabled) {
  background: #f0f2ff;
  border-color: #667eea;
  color: #667eea;
}

.quick-action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.sidebar-footer {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.clear-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 9px;
  border: 1px solid #f56c6c33;
  border-radius: 8px;
  background: #fef0f0;
  color: #f56c6c;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.clear-btn:hover:not(:disabled) {
  background: #fde2e2;
}

.clear-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ops-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px;
  scroll-behavior: smooth;
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}

.empty-icon {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  background: linear-gradient(135deg, #667eea15, #764ba215);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667eea;
  margin-bottom: 16px;
}

.chat-empty h3 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px;
}

.chat-empty p {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.message {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  max-width: 85%;
}

.message--user {
  margin-left: auto;
  flex-direction: row-reverse;
}

.message-avatar {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message--assistant .message-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
}

.message--user .message-avatar {
  background: linear-gradient(135deg, #4facfe, #00f2fe);
  color: #fff;
}

.message-body {
  min-width: 0;
}

.phase-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 6px;
}

.phase-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
  white-space: nowrap;
}

.phase-tag--routing { background: #ecf5ff; color: #409eff; }
.phase-tag--rag { background: #f4f0ff; color: #7c5cfc; }
.phase-tag--diagnosis { background: #fef5e7; color: #e6a23c; }
.phase-tag--fix-ok { background: #e8f8f0; color: #67c23a; }
.phase-tag--fix-reject { background: #fef0f0; color: #f56c6c; }
.phase-tag--default { background: #f0f2f5; color: #909399; }

.message-content {
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
  padding: 12px 16px;
  border-radius: 12px;
  word-break: break-word;
}

.message--assistant .message-content {
  background: #fff;
  border: 1px solid #e8eaed;
}

.message--user .message-content {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
}

.message-content :deep(h2) { font-size: 16px; font-weight: 700; margin: 8px 0 4px; }
.message-content :deep(h3) { font-size: 14px; font-weight: 600; margin: 6px 0 4px; }
.message-content :deep(p) { margin: 4px 0; }
.message-content :deep(ul), .message-content :deep(ol) { margin: 4px 0; padding-left: 18px; }
.message-content :deep(li) { margin: 2px 0; }
.message-content :deep(code) {
  background: #f0f2f5;
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 12.5px;
  font-family: "SF Mono", "Fira Code", monospace;
}
.message-content :deep(pre) {
  background: #1a1a2e;
  color: #e0e0e0;
  padding: 12px 16px;
  border-radius: 8px;
  overflow-x: auto;
  font-size: 12.5px;
  margin: 6px 0;
}
.message-content :deep(pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}
.message-content :deep(strong) { font-weight: 600; }
.message-content :deep(blockquote) {
  border-left: 3px solid #667eea;
  margin: 6px 0;
  padding: 4px 12px;
  color: #666;
  background: #f8f9ff;
  border-radius: 0 6px 6px 0;
}

.message--user .message-content :deep(code) {
  background: rgba(255,255,255,0.2);
  color: #fff;
}
.message--user .message-content :deep(pre) {
  background: rgba(0,0,0,0.2);
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #909399;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { opacity: 0.3; transform: scale(1); }
  30% { opacity: 1; transform: scale(1.2); }
}

.approval-card {
  margin-top: 10px;
  border: 1px solid #faecd8;
  border-radius: 10px;
  background: #fef9f0;
  overflow: hidden;
}

.approval-alert {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  color: #e6a23c;
  font-size: 13px;
  font-weight: 500;
}

.approval-actions {
  display: flex;
  gap: 8px;
  padding: 0 14px 12px;
}

.approval-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 7px 16px;
  border: none;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.approval-btn--accept {
  background: #67c23a;
  color: #fff;
}

.approval-btn--accept:hover {
  background: #5daf34;
}

.approval-btn--reject {
  background: #f56c6c;
  color: #fff;
}

.approval-btn--reject:hover {
  background: #e06060;
}

.chat-error {
  margin-bottom: 16px;
}

.chat-input {
  padding: 16px 32px 20px;
  background: #fff;
  border-top: 1px solid #e8eaed;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  background: #f5f7fa;
  border: 1px solid #e8eaed;
  border-radius: 12px;
  padding: 8px 8px 8px 14px;
  transition: border-color 0.2s ease;
}

.input-wrapper:focus-within {
  border-color: #667eea;
  background: #fff;
}

.input-textarea {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  max-height: 120px;
  font-family: inherit;
  color: #303133;
  padding: 4px 0;
}

.input-textarea::placeholder {
  color: #c0c4cc;
}

.input-textarea:disabled {
  opacity: 0.5;
}

.send-btn {
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.35);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.input-hint {
  font-size: 12px;
  color: #e6a23c;
  margin: 6px 0 0 2px;
}
</style>



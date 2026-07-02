<template>
  <div class="error-page">
    <div class="error-bg">
      <div class="error-bg__circle error-bg__circle--1"></div>
      <div class="error-bg__circle error-bg__circle--2"></div>
    </div>
    <div class="error-card">
      <div class="error-code">403</div>
      <h1 class="error-title">权限不足</h1>
      <p class="error-subtitle">抱歉，您没有权限访问此页面</p>
      <div class="error-actions">
        <el-button class="error-btn" @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回上一页
        </el-button>
        <el-button type="primary" class="error-btn error-btn--primary" @click="$router.push('/')">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ArrowLeft, HomeFilled } from '@element-plus/icons-vue'
</script>

<style scoped>
.error-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

.error-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  z-index: 0;
}

.error-bg__circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255,255,255,0.08);
}

.error-bg__circle--1 {
  width: 400px;
  height: 400px;
  top: -100px;
  right: -100px;
}

.error-bg__circle--2 {
  width: 300px;
  height: 300px;
  bottom: -80px;
  left: -80px;
}

.error-card {
  position: relative;
  z-index: 1;
  text-align: center;
  background: rgba(255,255,255,0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 48px 56px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}

.error-code {
  font-size: 72px;
  font-weight: 900;
  background: linear-gradient(135deg, #f56c6c, #f093fb);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  margin-bottom: 12px;
}

.error-title {
  font-size: 24px;
  font-weight: 800;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.error-subtitle {
  font-size: 15px;
  color: #909399;
  margin: 0 0 32px;
}

.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.error-btn {
  border-radius: 10px;
  padding: 12px 24px;
  font-weight: 500;
}

.error-btn--primary {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
}

.error-btn--primary:hover {
  box-shadow: 0 6px 20px rgba(102,126,234,0.4);
  transform: translateY(-1px);
}
</style>

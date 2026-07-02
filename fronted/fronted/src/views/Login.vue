<template>
  <div class="auth-page">
    <div class="auth-bg">
      <div class="auth-bg__circle auth-bg__circle--1"></div>
      <div class="auth-bg__circle auth-bg__circle--2"></div>
      <div class="auth-bg__circle auth-bg__circle--3"></div>
    </div>
    <div class="auth-card">
      <div class="auth-card__header">
        <div class="auth-card__logo">
          <el-icon :size="40" color="#667eea"><FolderOpened /></el-icon>
        </div>
        <h1 class="auth-card__title">资料共享平台</h1>
        <p class="auth-card__subtitle">欢迎回来，请登录您的账号</p>
      </div>

      <el-form :model="loginForm" :rules="rules" ref="formRef" class="auth-form">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            class="auth-input"
          >
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            class="auth-input"
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="auth-submit"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        <span>还没有账号？</span>
        <router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { FolderOpened, User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const res = await login(loginForm)
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify(res.data.user))
      ElMessage.success('登录成功')
      const redirect = route.query.redirect || '/'
      router.push(redirect)
    } catch (error) {
      console.error(error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

.auth-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  z-index: 0;
}

.auth-bg__circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255,255,255,0.08);
}

.auth-bg__circle--1 {
  width: 400px;
  height: 400px;
  top: -100px;
  right: -100px;
}

.auth-bg__circle--2 {
  width: 300px;
  height: 300px;
  bottom: -80px;
  left: -80px;
}

.auth-bg__circle--3 {
  width: 200px;
  height: 200px;
  top: 50%;
  left: 60%;
  transform: translate(-50%, -50%);
}

.auth-card {
  position: relative;
  z-index: 1;
  width: 420px;
  background: rgba(255,255,255,0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 40px 36px 32px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15), 0 0 0 1px rgba(255,255,255,0.2);
}

.auth-card__header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-card__logo {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 8px 24px rgba(102,126,234,0.35);
}

.auth-card__logo :deep(.el-icon) {
  color: #fff;
}

.auth-card__title {
  font-size: 24px;
  font-weight: 800;
  color: #1a1a2e;
  margin: 0 0 8px;
  letter-spacing: 1px;
}

.auth-card__subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.auth-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 14px;
  height: 48px;
  box-shadow: 0 0 0 1px #e4e7ed;
  transition: all 0.3s;
}

.auth-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #667eea;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #667eea;
}

.auth-form :deep(.el-input__inner) {
  font-size: 15px;
}

.auth-submit {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  transition: all 0.3s;
}

.auth-submit:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(102,126,234,0.4);
}

.auth-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #909399;
}

.auth-footer a {
  color: #667eea;
  text-decoration: none;
  margin-left: 6px;
  font-weight: 500;
  transition: color 0.3s;
}

.auth-footer a:hover {
  color: #764ba2;
}
</style>

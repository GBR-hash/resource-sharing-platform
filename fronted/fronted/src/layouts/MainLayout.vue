<template>
  <div class="main-layout">
    <el-container>
      <el-header>
        <div class="header-bg">
          <div class="header-bg__orb header-bg__orb--1"></div>
          <div class="header-bg__orb header-bg__orb--2"></div>
        </div>
        <div class="header-content">
          <div class="logo" @click="$router.push('/')">
            <div class="logo-icon">
              <el-icon :size="20"><FolderOpened /></el-icon>
            </div>
            <span class="logo-text">资料共享平台</span>
          </div>

          <div class="nav-menu">
            <div class="nav-pills">
              <div
                class="nav-pill"
                :class="{ 'nav-pill--active': activeMenu === '/resources' }"
                @click="$router.push('/resources')"
              >
                <el-icon><Document /></el-icon>
                <span>资料浏览</span>
              </div>
              <div
                class="nav-pill"
                :class="{ 'nav-pill--active': activeMenu === '/upload' }"
                @click="handleUploadClick"
              >
                <el-icon><Upload /></el-icon>
                <span>上传资料</span>
              </div>
              <div
                class="nav-pill"
                :class="{ 'nav-pill--active': activeMenu === '/ops-agent' }"
                @click="$router.push('/ops-agent')"
              >
                <el-icon><Monitor /></el-icon>
                <span>运维助手</span>
              </div>
              <div
                v-if="isAdmin"
                class="nav-pill"
                :class="{ 'nav-pill--active': activeMenu === '/admin' }"
                @click="$router.push('/admin')"
              >
                <el-icon><Setting /></el-icon>
                <span>管理后台</span>
              </div>
            </div>
          </div>

          <div class="user-info">
            <template v-if="isLoggedIn">
              <el-dropdown @command="handleCommand">
                <span class="user-dropdown">
                  <div class="header-avatar" :style="{ background: getAvatarColor(user.username) }">
                    {{ user.username?.charAt(0)?.toUpperCase() }}
                  </div>
                  <span class="username">{{ user.username }}</span>
                  <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="profile">
                      <el-icon><User /></el-icon>
                      个人中心
                    </el-dropdown-item>
                    <el-dropdown-item divided command="logout">
                      <el-icon><SwitchButton /></el-icon>
                      退出登录
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <template v-else>
              <el-button class="btn-login" @click="$router.push('/login')">登录</el-button>
              <el-button class="btn-register" @click="$router.push('/register')">注册</el-button>
            </template>
          </div>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>

    <!-- 未登录上传提示对话框 -->
    <el-dialog
      v-model="authDialogVisible"
      title="上传资料需要登录"
      width="400px"
      :close-on-click-modal="false"
    >
      <p style="text-align: center; font-size: 15px; color: #606266; margin-bottom: 20px;">
        上传资料需要先登录账号，您可以选择以下方式继续：
      </p>
      <div style="display: flex; flex-direction: column; gap: 12px;">
        <el-button type="primary" size="large" @click="goToLogin">
          立即登录
        </el-button>
        <el-button type="success" size="large" @click="goToRegister">
          创建账号
        </el-button>
        <el-button size="large" @click="authDialogVisible = false">
          稍后再说
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  FolderOpened,
  Document,
  Upload,
  Setting,
  User,
  SwitchButton,
  ArrowDown,
  Monitor
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const authDialogVisible = ref(false)
const authState = ref(0)

const getUserFromStorage = () => {
  try {
    const userStr = localStorage.getItem('user')
    if (userStr && userStr !== 'undefined') {
      return JSON.parse(userStr)
    }
  } catch (e) {
    console.error('Failed to parse user:', e)
  }
  return {}
}

const user = computed(() => {
  void authState.value
  return getUserFromStorage()
})
const isLoggedIn = computed(() => {
  void authState.value
  return !!localStorage.getItem('token')
})
const isAdmin = computed(() => user.value.role === 1)

const avatarColors = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)',
  'linear-gradient(135deg, #fccb90 0%, #d57eeb 100%)',
  'linear-gradient(135deg, #e0c3fc 0%, #8ec5fc 100%)',
]

const getAvatarColor = (username) => {
  if (!username) return avatarColors[0]
  const index = username.charCodeAt(0) % avatarColors.length
  return avatarColors[index]
}

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/admin')) return '/admin'
  if (path.startsWith('/upload')) return '/upload'
  if (path.startsWith('/ops-agent')) return '/ops-agent'
  return '/resources'
})

watch(route, () => {
  authState.value++
})

const handleUploadClick = () => {
  if (isLoggedIn.value) {
    router.push('/upload')
  } else {
    authDialogVisible.value = true
  }
}

const goToLogin = () => {
  authDialogVisible.value = false
  router.push({ path: '/login', query: { redirect: '/upload' } })
}

const goToRegister = () => {
  authDialogVisible.value = false
  router.push({ path: '/register', query: { redirect: '/upload' } })
}

const handleCommand = (command) => {
  if (command === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    authState.value++
    window.dispatchEvent(new CustomEvent('auth-change'))
    ElMessage.success('已退出登录')
    router.push('/')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  background-color: #f5f7fa;
}

/* ===== Header ===== */
.el-header {
  position: relative;
  padding: 0;
  height: 60px;
  z-index: 100;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 1px 0 rgba(0,0,0,0.06);
}

.header-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  z-index: 0;
}

.header-bg__orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.3;
}

.header-bg__orb--1 {
  width: 200px;
  height: 200px;
  background: #f093fb;
  top: -80px;
  right: 10%;
}

.header-bg__orb--2 {
  width: 160px;
  height: 160px;
  background: #4facfe;
  bottom: -60px;
  left: 20%;
}

:deep(.el-main) {
  padding: 0;
}

.header-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 32px;
  max-width: 1400px;
  margin: 0 auto;
}

/* ===== Logo ===== */
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  flex-shrink: 0;
}

.logo-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: rgba(255,255,255,0.2);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  transition: all 0.3s ease;
}

.logo:hover .logo-icon {
  background: rgba(255,255,255,0.35);
  transform: rotate(-6deg) scale(1.06);
}

.logo-text {
  font-size: 17px;
  font-weight: 800;
  color: #fff;
  letter-spacing: 0.5px;
  text-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

/* ===== Nav Pills ===== */
.nav-menu {
  flex: 1;
  display: flex;
  justify-content: center;
}

.nav-pills {
  display: flex;
  align-items: center;
  gap: 2px;
  background: rgba(255,255,255,0.12);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 12px;
  padding: 4px;
}

.nav-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  border-radius: 9px;
  font-size: 13.5px;
  font-weight: 500;
  color: rgba(255,255,255,0.75);
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
  user-select: none;
}

.nav-pill:hover {
  color: #fff;
  background: rgba(255,255,255,0.12);
}

.nav-pill--active {
  color: #fff;
  background: rgba(255,255,255,0.25);
  font-weight: 600;
  box-shadow: 0 1px 6px rgba(0,0,0,0.1);
}

.nav-pill .el-icon {
  font-size: 15px;
}

/* ===== User Area ===== */
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px 4px 4px;
  border-radius: 20px;
  background: rgba(255,255,255,0.15);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255,255,255,0.1);
  transition: all 0.25s ease;
}

.user-dropdown:hover {
  background: rgba(255,255,255,0.25);
}

.header-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
  border: 2px solid rgba(255,255,255,0.45);
  transition: transform 0.2s ease;
}

.user-dropdown:hover .header-avatar {
  transform: scale(1.08);
}

.username {
  font-size: 13.5px;
  color: #fff;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0,0,0,0.08);
}

.dropdown-arrow {
  font-size: 11px;
  color: rgba(255,255,255,0.6);
  transition: transform 0.3s ease;
}

.user-dropdown:hover .dropdown-arrow {
  transform: rotate(180deg);
}

/* ===== Login / Register Buttons ===== */
.btn-login {
  border-radius: 8px;
  background: rgba(255,255,255,0.18);
  border: 1px solid rgba(255,255,255,0.25);
  color: #fff;
  font-weight: 500;
  font-size: 13.5px;
  padding: 7px 18px;
  transition: all 0.25s ease;
}

.btn-login:hover {
  background: rgba(255,255,255,0.3);
  border-color: rgba(255,255,255,0.45);
}

.btn-register {
  border-radius: 8px;
  background: #fff;
  color: #667eea;
  font-weight: 600;
  font-size: 13.5px;
  padding: 7px 18px;
  border: none;
  transition: all 0.25s ease;
}

.btn-register:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(0,0,0,0.15);
}
</style>

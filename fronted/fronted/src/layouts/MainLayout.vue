<template>
  <div class="main-layout">
    <el-container>
      <el-header>
        <div class="header-bg">
          <div class="header-bg__orb header-bg__orb--1"></div>
          <div class="header-bg__orb header-bg__orb--2"></div>
        </div>
        <div class="header-content">
          <!-- 汉堡菜单（仅手机端） -->
          <div class="hamburger" @click="drawerVisible = true">
            <el-icon :size="22"><Menu /></el-icon>
          </div>

          <div class="logo" @click="$router.push('/')">
            <div class="logo-icon">
              <el-icon :size="20"><FolderOpened /></el-icon>
            </div>
            <span class="logo-text">资料共享平台</span>
          </div>

          <!-- 桌面端导航 -->
          <div class="nav-menu nav-menu--desktop">
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
                class="nav-pill"
                :class="{ 'nav-pill--active': activeMenu === '/monitor' }"
                @click="$router.push('/monitor')"
              >
                <el-icon><VideoCamera /></el-icon>
                <span>系统监控</span>
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
        <router-view v-slot="{ Component }">
          <keep-alive :include="['Monitor']">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>

    <!-- 手机端侧滑导航抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      direction="ltr"
      size="280px"
      :with-header="false"
      class="mobile-drawer"
    >
      <div class="drawer-content">
        <div class="drawer-user" v-if="isLoggedIn">
          <div class="drawer-avatar" :style="{ background: getAvatarColor(user.username) }">
            {{ user.username?.charAt(0)?.toUpperCase() }}
          </div>
          <div class="drawer-user-info">
            <span class="drawer-username">{{ user.username }}</span>
            <span class="drawer-role">{{ isAdmin ? '管理员' : '普通用户' }}</span>
          </div>
        </div>
        <div class="drawer-user drawer-user--guest" v-else>
          <div class="drawer-avatar drawer-avatar--guest">
            <el-icon :size="22"><User /></el-icon>
          </div>
          <span class="drawer-username">未登录</span>
        </div>

        <div class="drawer-divider"></div>

        <div class="drawer-nav">
          <div
            class="drawer-item"
            :class="{ 'drawer-item--active': activeMenu === '/resources' }"
            @click="navigateTo('/resources')"
          >
            <el-icon><Document /></el-icon>
            <span>资料浏览</span>
          </div>
          <div
            class="drawer-item"
            :class="{ 'drawer-item--active': activeMenu === '/upload' }"
            @click="handleUploadClick"
          >
            <el-icon><Upload /></el-icon>
            <span>上传资料</span>
          </div>
          <div
            class="drawer-item"
            :class="{ 'drawer-item--active': activeMenu === '/ops-agent' }"
            @click="navigateTo('/ops-agent')"
          >
            <el-icon><Monitor /></el-icon>
            <span>运维助手</span>
          </div>
          <div
            class="drawer-item"
            :class="{ 'drawer-item--active': activeMenu === '/monitor' }"
            @click="navigateTo('/monitor')"
          >
            <el-icon><VideoCamera /></el-icon>
            <span>系统监控</span>
          </div>
          <div
            v-if="isAdmin"
            class="drawer-item"
            :class="{ 'drawer-item--active': activeMenu === '/admin' }"
            @click="navigateTo('/admin')"
          >
            <el-icon><Setting /></el-icon>
            <span>管理后台</span>
          </div>
        </div>

        <div class="drawer-divider"></div>

        <div class="drawer-nav">
          <div class="drawer-item" @click="navigateTo('/profile')" v-if="isLoggedIn">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </div>
          <div class="drawer-item drawer-item--logout" @click="handleLogout" v-if="isLoggedIn">
            <el-icon><SwitchButton /></el-icon>
            <span>退出登录</span>
          </div>
          <div class="drawer-item" @click="navigateTo('/login')" v-if="!isLoggedIn">
            <el-icon><User /></el-icon>
            <span>登录</span>
          </div>
          <div class="drawer-item" @click="navigateTo('/register')" v-if="!isLoggedIn">
            <el-icon><Plus /></el-icon>
            <span>注册账号</span>
          </div>
        </div>
      </div>
    </el-drawer>

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
  Monitor,
  VideoCamera,
  Menu,
  Plus
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const authDialogVisible = ref(false)
const authState = ref(0)
const drawerVisible = ref(false)

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
  if (path.startsWith('/monitor')) return '/monitor'
  return '/resources'
})

watch(route, () => {
  authState.value++
})

const navigateTo = (path) => {
  drawerVisible.value = false
  if (path === '/upload' && !isLoggedIn.value) {
    authDialogVisible.value = true
    return
  }
  router.push(path)
}

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
    handleLogout()
  } else if (command === 'profile') {
    router.push('/profile')
  }
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  authState.value++
  drawerVisible.value = false
  window.dispatchEvent(new CustomEvent('auth-change'))
  ElMessage.success('已退出登录')
  router.push('/')
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
  gap: 12px;
}

/* ===== Hamburger ===== */
.hamburger {
  display: none;
  width: 36px;
  height: 36px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.2s;
}
.hamburger:hover { background: rgba(255,255,255,0.15); }

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

/* ===== Nav Pills (桌面端) ===== */
.nav-menu--desktop {
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

/* ===== Mobile Drawer ===== */
:deep(.mobile-drawer .el-drawer__body) {
  padding: 0;
}

.drawer-content {
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.drawer-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 20px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.drawer-user--guest {
  background: #161b22;
}

.drawer-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.drawer-avatar--guest {
  background: rgba(255,255,255,0.1);
  color: #8b949e;
}

.drawer-user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
}

.drawer-username {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drawer-role {
  font-size: 12px;
  color: rgba(255,255,255,0.6);
}

.drawer-divider {
  height: 1px;
  background: #e4e7ed;
  margin: 0;
}

.drawer-nav {
  padding: 8px 0;
}

.drawer-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 20px;
  font-size: 14px;
  color: #303133;
  cursor: pointer;
  transition: background 0.15s;
}

.drawer-item:hover {
  background: #f5f7fa;
}

.drawer-item--active {
  color: #667eea;
  background: #f0f2ff;
  font-weight: 600;
}

.drawer-item--active:hover {
  background: #f0f2ff;
}

.drawer-item--logout {
  color: #f56c6c;
}

.drawer-item .el-icon {
  font-size: 18px;
  flex-shrink: 0;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .el-header {
    height: 52px;
  }

  .header-content {
    padding: 0 12px;
  }

  /* 隐藏桌面导航 */
  .nav-menu--desktop {
    display: none;
  }

  /* 显示汉堡图标 */
  .hamburger {
    display: flex;
  }

  /* Logo 缩小 */
  .logo-icon {
    width: 28px;
    height: 28px;
    border-radius: 8px;
  }
  .logo-text {
    font-size: 15px;
  }

  /* 去掉装饰 orb */
  .header-bg__orb {
    display: none;
  }

  /* 用户区精简 */
  .username {
    display: none;
  }

  .user-dropdown {
    padding: 4px;
    background: transparent;
    border: none;
  }

  .user-dropdown:hover {
    background: rgba(255,255,255,0.1);
  }

  .btn-login,
  .btn-register {
    font-size: 12px;
    padding: 5px 12px;
  }

  /* el-dialog 移动端宽度 */
  :deep(.auth-dialog .el-dialog) {
    width: 90% !important;
  }
}
</style>

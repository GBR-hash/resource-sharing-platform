<template>
  <div class="profile-page">
    <!-- 用户信息卡片 -->
    <div class="profile-hero">
      <div class="profile-hero__bg"></div>
      <div class="profile-hero__content">
        <div class="profile-avatar">
          <div class="profile-avatar__circle">
            {{ user.username?.charAt(0)?.toUpperCase() }}
          </div>
        </div>
        <div class="profile-hero__info">
          <h1 class="profile-hero__name">{{ user.username }}</h1>
          <div class="profile-hero__meta">
            <el-tag :type="user.role === 1 ? 'danger' : 'info'" effect="dark" round size="small">
              {{ user.role === 1 ? '管理员' : '普通用户' }}
            </el-tag>
            <span class="profile-hero__email">{{ user.email || '未设置邮箱' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 内容卡片 -->
    <div class="profile-card">
      <el-tabs v-model="activeTab" class="profile-tabs">
        <el-tab-pane name="basic">
          <template #label>
            <span class="tab-label">
              <el-icon><User /></el-icon>
              基本信息
            </span>
          </template>
          <div class="tab-content">
            <el-form :model="userForm" label-width="100px" class="profile-form">
              <el-form-item label="用户名">
                <el-input v-model="userForm.username" disabled>
                  <template #prefix>
                    <el-icon><User /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="真实姓名">
                <el-input v-model="userForm.realName" placeholder="请输入真实姓名">
                  <template #prefix>
                    <el-icon><UserFilled /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="userForm.email" placeholder="请输入邮箱">
                  <template #prefix>
                    <el-icon><Message /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="手机号">
                <el-input v-model="userForm.phone" placeholder="请输入手机号">
                  <template #prefix>
                    <el-icon><Phone /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" class="save-btn" @click="handleUpdate">
                  <el-icon><Check /></el-icon>
                  保存修改
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane name="resources">
          <template #label>
            <span class="tab-label">
              <el-icon><Document /></el-icon>
              我的资料
            </span>
          </template>
          <div class="tab-content">
            <el-table :data="myResources" v-loading="loading" class="styled-table" stripe>
              <el-table-column prop="title" label="资料名称" min-width="200">
                <template #default="{ row }">
                  <el-link type="primary" :underline="false">{{ row.title }}</el-link>
                </template>
              </el-table-column>
              <el-table-column prop="fileType" label="类型" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="getFileTypeTag(row.fileType)" effect="plain" round size="small">
                    {{ row.fileType }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="downloadCount" label="下载量" width="100" align="center">
                <template #default="{ row }">
                  <div class="count-cell">
                    <el-icon color="#409eff"><Download /></el-icon>
                    <span>{{ row.downloadCount }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tag
                    :type="row.status === 1 ? 'success' : 'warning'"
                    effect="dark"
                    round
                    size="small"
                  >
                    {{ row.status === 1 ? '已发布' : '审核中' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150" align="center">
                <template #default="{ row }">
                  <el-button v-if="user.role === 1" type="danger" size="small" round plain @click="handleDelete(row)">
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-button>
                  <span v-else style="color: #c0c4cc;">-</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane name="favorites">
          <template #label>
            <span class="tab-label">
              <el-icon><StarFilled /></el-icon>
              我的收藏
            </span>
          </template>
          <div class="tab-content">
            <el-table :data="myFavorites" v-loading="favoriteLoading" class="styled-table" stripe>
              <el-table-column prop="title" label="资料名称" min-width="200">
                <template #default="{ row }">
                  <el-link type="primary" :underline="false" @click="goToResource(row.id)">
                    {{ row.title }}
                  </el-link>
                </template>
              </el-table-column>
              <el-table-column prop="fileType" label="类型" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="getFileTypeTag(row.fileType)" effect="plain" round size="small">
                    {{ row.fileType }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="downloadCount" label="下载量" width="100" align="center">
                <template #default="{ row }">
                  <div class="count-cell">
                    <el-icon color="#409eff"><Download /></el-icon>
                    <span>{{ row.downloadCount }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="favoriteCount" label="收藏数" width="100" align="center">
                <template #default="{ row }">
                  <div class="count-cell">
                    <el-icon :size="16" color="#f56c6c"><StarFilled /></el-icon>
                    <span>{{ row.favoriteCount || 0 }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150" align="center">
                <template #default="{ row }">
                  <el-button type="warning" size="small" round plain @click="handleUnfavorite(row)">
                    <el-icon><StarFilled /></el-icon>
                    取消收藏
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!favoriteLoading && myFavorites.length === 0" class="empty-state">
              <el-icon :size="48" color="#c0c4cc"><StarFilled /></el-icon>
              <p>暂无收藏资料</p>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, UserFilled, Message, Phone, Check, Document, StarFilled, Download, Delete } from '@element-plus/icons-vue'
import { getResources, deleteResource } from '@/api/resource'
import { getMyFavorites, toggleFavorite } from '@/api/favorite'

const router = useRouter()
const activeTab = ref('basic')
const loading = ref(false)
const favoriteLoading = ref(false)
const myResources = ref([])
const myFavorites = ref([])

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

const user = computed(() => getUserFromStorage())

const userForm = reactive({
  username: user.value.username || '',
  realName: user.value.realName || '',
  email: user.value.email || '',
  phone: user.value.phone || ''
})

const getFileTypeTag = (type) => {
  const typeMap = {
    'pdf': 'danger',
    'doc': 'primary',
    'docx': 'primary',
    'xls': 'success',
    'xlsx': 'success',
    'ppt': 'warning',
    'pptx': 'warning',
    'mp4': 'info',
    'avi': 'info',
    'jpg': 'success',
    'png': 'success'
  }
  return typeMap[type?.toLowerCase()] || 'info'
}

const fetchMyResources = async () => {
  loading.value = true
  try {
    const res = await getResources({ uploaderId: user.value.id, page: 0, size: 100 })
    myResources.value = res.data.content
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleUpdate = () => {
  ElMessage.info('功能开发中...')
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该资料吗？', '提示', {
      type: 'warning'
    })
    await deleteResource(row.id)
    ElMessage.success('删除成功')
    fetchMyResources()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const fetchMyFavorites = async () => {
  favoriteLoading.value = true
  try {
    const res = await getMyFavorites()
    const favoriteIds = res.data
    if (favoriteIds && favoriteIds.length > 0) {
      const resourcesRes = await getResources({ page: 0, size: 100 })
      myFavorites.value = resourcesRes.data.content.filter(r => favoriteIds.includes(r.id))
    } else {
      myFavorites.value = []
    }
  } catch (error) {
    console.error('获取收藏列表失败:', error)
  } finally {
    favoriteLoading.value = false
  }
}

const handleUnfavorite = async (row) => {
  try {
    await toggleFavorite(row.id)
    ElMessage.success('已取消收藏')
    fetchMyFavorites()
  } catch (error) {
    console.error('取消收藏失败:', error)
  }
}

const goToResource = (id) => {
  router.push(`/resources`)
}

onMounted(() => {
  fetchMyResources()
  fetchMyFavorites()
})
</script>

<style scoped>
.profile-page {
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

/* Hero */
.profile-hero {
  position: relative;
  overflow: hidden;
  padding: 40px 40px 36px;
}

.profile-hero__bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  z-index: 0;
}

.profile-hero__bg::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 80%, rgba(255,255,255,0.15) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255,255,255,0.1) 0%, transparent 50%);
}

.profile-hero__bg::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 30px;
  background: linear-gradient(to bottom, transparent, #f5f7fa);
}

.profile-hero__content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 24px;
  max-width: 800px;
  margin: 0 auto;
}

.profile-avatar__circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: rgba(255,255,255,0.25);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  border: 3px solid rgba(255,255,255,0.4);
}

.profile-hero__info {
  flex: 1;
}

.profile-hero__name {
  font-size: 28px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 8px;
  letter-spacing: 1px;
  text-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.profile-hero__meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.profile-hero__email {
  font-size: 14px;
  color: rgba(255,255,255,0.8);
}

/* Card */
.profile-card {
  max-width: 900px;
  margin: -8px auto 32px;
  background: #fff;
  border-radius: 12px;
  padding: 24px 32px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  position: relative;
  z-index: 2;
}

/* Tabs */
.profile-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
  border-bottom: 2px solid #f0f0f0;
}

.profile-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
  padding: 0 20px;
  height: 48px;
  line-height: 48px;
  color: #909399;
  transition: all 0.3s;
}

.profile-tabs :deep(.el-tabs__item.is-active) {
  color: #667eea;
  font-weight: 600;
}

.profile-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 3px;
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab-label .el-icon {
  font-size: 18px;
}

.tab-content {
  padding: 8px 0;
}

/* Form */
.profile-form {
  max-width: 560px;
}

.profile-form :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.save-btn {
  border-radius: 8px;
  font-weight: 600;
  min-width: 140px;
}

/* Table */
.styled-table {
  border-radius: 8px;
  overflow: hidden;
}

.styled-table :deep(.el-table__header th) {
  background: #f5f7fa;
  color: #606266;
  font-weight: 600;
  font-size: 14px;
}

.styled-table :deep(.el-table__row:hover td) {
  background: #f5f7fa;
}

.count-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-weight: 600;
  color: #303133;
}

/* Empty */
.empty-state {
  text-align: center;
  padding: 48px 20px;
  color: #909399;
}

.empty-state p {
  margin-top: 12px;
  font-size: 15px;
}
</style>

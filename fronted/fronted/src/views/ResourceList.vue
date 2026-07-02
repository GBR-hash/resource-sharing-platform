<template>
  <div class="resource-list">
    <!-- 顶部 Hero 区域：左标题 + 右走马灯 -->
    <div class="hero-section">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <!-- 左侧标题区 -->
        <div class="hero-left">
          <h1 class="hero-title">
            <el-icon class="hero-icon"><FolderOpened /></el-icon>
            资料共享平台
          </h1>
          <p class="hero-subtitle">发现、分享、下载优质竞赛资料</p>
          <div class="hero-stats">
            <div class="stat-item">
              <span class="stat-num">{{ pagination.total }}</span>
              <span class="stat-label">份资料</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ totalDownloads }}</span>
              <span class="stat-label">次下载</span>
            </div>
          </div>
        </div>

        <!-- 右侧走马灯 -->
        <div class="hero-right">
          <div class="carousel-label">
            <el-icon><TrendCharts /></el-icon>
            <span>热门资料 Top 5</span>
          </div>
          <el-carousel
            v-if="topResources.length"
            type="card"
            height="260px"
            :interval="4000"
            indicator-position="none"
            class="hot-carousel"
          >
            <el-carousel-item v-for="item in topResources" :key="item.id">
              <div class="hot-card" @click="handleCardClick(item)">
                <!-- 预览区 -->
                <div class="hot-card__preview">
                  <img
                    v-if="item.fileType === 'image'"
                    :src="getImagePreviewUrl(item.id)"
                    :alt="item.title"
                    class="hot-card__img"
                  />
                  <div v-else class="hot-card__placeholder">
                    <el-icon v-if="item.fileType === 'video'" :size="48" color="#409eff"><VideoPlay /></el-icon>
                    <el-icon v-else :size="48" color="#67c23a"><Document /></el-icon>
                    <span class="hot-card__filetype">{{ item.fileType?.toUpperCase() }}</span>
                  </div>
                  <div class="hot-card__badge">
                    <el-icon><Download /></el-icon>
                    {{ item.downloadCount }}
                  </div>
                </div>
                <!-- 信息区 -->
                <div class="hot-card__info">
                  <h3 class="hot-card__title">{{ item.title }}</h3>
                  <div class="hot-card__meta">
                    <el-tag v-if="item.categoryName" size="small" effect="plain" round>
                      {{ item.categoryName }}
                    </el-tag>
                    <el-tag v-if="item.competitionTypeName" size="small" effect="plain" round type="warning">
                      {{ item.competitionTypeName }}
                    </el-tag>
                  </div>
                  <p class="hot-card__desc">{{ item.description || '暂无描述' }}</p>
                </div>
              </div>
            </el-carousel-item>
          </el-carousel>
          <div v-else class="carousel-empty">
            <el-icon :size="40" color="rgba(255,255,255,0.5)"><FolderOpened /></el-icon>
            <p>暂无热门资料</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选工具栏（含搜索框） -->
    <div class="filter-toolbar">
      <!-- 第一行：搜索 + 排序 -->
      <div class="filter-row filter-row--top">
        <div class="search-wrapper">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索资料名称或描述..."
            clearable
            class="search-input"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" class="search-btn" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </div>
        <div class="filter-group filter-group--right">
          <el-select
            v-model="searchForm.topDownloads"
            placeholder="下载量Top"
            clearable
            class="filter-select filter-select--sm"
          >
            <el-option label="Top 5" :value="5" />
            <el-option label="Top 10" :value="10" />
            <el-option label="Top 20" :value="20" />
            <el-option label="Top 50" :value="50" />
          </el-select>
          <el-select
            v-model="searchForm.topFavorites"
            placeholder="收藏量Top"
            clearable
            class="filter-select filter-select--sm"
          >
            <el-option label="Top 5" :value="5" />
            <el-option label="Top 10" :value="10" />
            <el-option label="Top 20" :value="20" />
            <el-option label="Top 50" :value="50" />
          </el-select>
          <el-button text class="reset-btn" @click="resetSearch">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </div>
      </div>
      <!-- 第二行：分类筛选 -->
      <div class="filter-row filter-row--bottom">
        <div class="filter-group">
          <el-select
            v-model="searchForm.categoryId"
            placeholder="资料分类"
            clearable
            class="filter-select"
          >
            <el-option
              v-for="item in categories"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <el-select
            v-model="searchForm.competitionTypeId"
            placeholder="竞赛类型"
            clearable
            class="filter-select"
          >
            <el-option
              v-for="item in competitionTypes"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <el-select
            v-model="searchForm.timeRange"
            placeholder="发布时间"
            clearable
            class="filter-select"
          >
            <el-option label="今天" value="today" />
            <el-option label="昨天" value="yesterday" />
            <el-option label="一周内" value="week" />
            <el-option label="近一个月" value="month" />
            <el-option label="近三个月" value="threeMonths" />
            <el-option label="近一年" value="year" />
            <el-option label="所有" value="all" />
          </el-select>
          <el-date-picker
            v-model="searchForm.customDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            clearable
            class="filter-date"
          />
        </div>
      </div>
    </div>

    <!-- 资料列表卡片 -->
    <div class="list-card">
      <div class="list-header">
        <div class="list-header__left">
          <h2 class="list-header__title">资料列表</h2>
          <span class="list-header__count">共 {{ pagination.total }} 条资料</span>
        </div>
        <el-button type="primary" class="upload-btn" @click="handleUploadClick">
          <el-icon><Upload /></el-icon>
          上传资料
        </el-button>
      </div>

      <el-table :data="resourceList" v-loading="loading" class="styled-table" stripe>
        <el-table-column prop="title" label="资料名称" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" @click="viewDetail(row.id)" :underline="false">
              {{ row.title }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.remark || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="预览" width="80" align="center">
          <template #default="{ row }">
            <div v-if="row.fileType === 'image'" class="preview-thumb">
              <img
                :src="getImagePreviewUrl(row.id)"
                :alt="row.fileName"
                class="thumb-img"
                @click="handlePreview(row)"
              />
            </div>
            <div v-else class="preview-icon" @click="handlePreview(row)">
              <el-icon v-if="row.fileType === 'video'" :size="28" color="#409eff"><VideoPlay /></el-icon>
              <el-icon v-else :size="28" color="#67c23a"><Document /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getFileTypeTag(row.fileType)" effect="plain" round size="small">
              {{ row.fileType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="100" align="center">
          <template #default="{ row }">
            <span class="file-size-text">{{ formatFileSize(row.fileSize) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="downloadCount" label="下载量" width="90" align="center">
          <template #default="{ row }">
            <div class="count-cell">
              <el-icon color="#409eff"><Download /></el-icon>
              <span>{{ row.downloadCount }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="favoriteCount" label="收藏数" width="90" align="center">
          <template #default="{ row }">
            <div class="count-cell">
              <el-icon :size="16" color="#f56c6c"><StarFilled v-if="row.favoriteCount > 0" /></el-icon>
              <span>{{ row.favoriteCount || 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="发布时间" width="160" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : row.status === 0 ? 'warning' : 'danger'"
              effect="dark"
              round
              size="small"
            >
              {{ row.status === 1 ? '已发布' : row.status === 0 ? '审核中' : '已拒绝' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <template v-if="row.status === 2">
              <el-text type="danger" size="small">已拒绝</el-text>
              <el-divider v-if="isAdmin()" direction="vertical" />
              <el-button
                v-if="isAdmin()"
                link
                type="danger"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </template>
            <template v-else>
              <el-button link type="primary" @click="handlePreview(row)">
                预览
              </el-button>
              <el-divider direction="vertical" />
              <el-button link type="primary" @click="handleDownload(row)">
                下载
              </el-button>
              <el-divider direction="vertical" />
              <el-button
                :type="row.isFavorite ? 'warning' : 'info'"
                link
                @click="handleFavorite(row)"
              >
                <el-icon><Star v-if="!row.isFavorite" /><StarFilled v-else /></el-icon>
                {{ row.isFavorite ? '已收藏' : '收藏' }}
              </el-button>
              <el-divider v-if="isAdmin()" direction="vertical" />
              <el-button
                v-if="isAdmin()"
                link
                type="danger"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 文件预览对话框 -->
      <el-dialog
        v-model="previewVisible"
        :title="previewResource?.fileName || '文件预览'"
        width="700px"
        :close-on-click-modal="true"
      >
        <div v-if="previewResource" class="preview-content">
          <div v-if="previewResource.fileType === 'image'" class="preview-image">
            <img :src="getImagePreviewUrl(previewResource.id)" :alt="previewResource.fileName" />
          </div>
          <div v-else-if="previewResource.fileType === 'video'" class="preview-video">
            <video
              :src="getPreviewUrl(previewResource.id)"
              controls
              style="width: 100%; max-height: 500px"
            />
          </div>
          <div v-else class="preview-doc">
            <el-result icon="info" title="文档预览">
              <template #sub-title>
                <p>文件名：{{ previewResource.fileName }}</p>
                <p>大小：{{ formatFileSize(previewResource.fileSize) }}</p>
                <p>类型：{{ previewResource.fileType }}</p>
                <p v-if="previewResource.remark">备注：{{ previewResource.remark }}</p>
              </template>
              <template #extra>
                <el-button type="primary" @click="handleDownload(previewResource)">
                  下载查看
                </el-button>
              </template>
            </el-result>
          </div>
        </div>
      </el-dialog>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="fetchResourceList"
          @current-change="fetchResourceList"
        />
      </div>
    </div>

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
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Upload, VideoPlay, Document, Star, StarFilled, FolderOpened, Download, Refresh, TrendCharts } from '@element-plus/icons-vue'
import { getResources, deleteResource, downloadResource } from '@/api/resource'
import { getCategories, getCompetitionTypes } from '@/api/resource'
import { toggleFavorite } from '@/api/favorite'

const router = useRouter()
const loading = ref(false)
const authDialogVisible = ref(false)
const previewVisible = ref(false)
const previewResource = ref(null)
const categories = ref([])
const competitionTypes = ref([])
const resourceList = ref([])
const topResources = ref([])

const searchForm = reactive({
  keyword: '',
  categoryId: null,
  competitionTypeId: null,
  timeRange: null,
  customDate: null,
  topDownloads: null,
  topFavorites: null
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

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

const totalDownloads = computed(() => {
  return resourceList.value.reduce((sum, r) => sum + (r.downloadCount || 0), 0)
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

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

const isAdmin = () => {
  return user.value.role === 1
}

const fetchResourceList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size,
      ...searchForm
    }
    const res = await getResources(params)
    resourceList.value = res.data.content
    pagination.total = res.data.totalElements
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const fetchTopResources = async () => {
  try {
    const res = await getResources({ page: 0, size: 5, topDownloads: 5 })
    topResources.value = res.data.content || []
  } catch (error) {
    console.error('获取热门资料失败:', error)
  }
}

const fetchCategories = async () => {
  try {
    const res = await getCategories()
    categories.value = res.data
  } catch (error) {
    console.error(error)
  }
}

const fetchCompetitionTypes = async () => {
  try {
    const res = await getCompetitionTypes()
    competitionTypes.value = res.data
  } catch (error) {
    console.error(error)
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchResourceList()
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.categoryId = null
  searchForm.competitionTypeId = null
  searchForm.timeRange = null
  searchForm.customDate = null
  searchForm.topDownloads = null
  searchForm.topFavorites = null
  pagination.page = 1
  fetchResourceList()
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

const handleFavorite = async (row) => {
  if (!localStorage.getItem('token')) {
    authDialogVisible.value = true
    return
  }
  try {
    const res = await toggleFavorite(row.id)
    row.isFavorite = res.data.isFavorite
    row.favoriteCount = res.data.count
    ElMessage.success(row.isFavorite ? '收藏成功' : '取消收藏')
  } catch (error) {
    console.error('收藏失败:', error)
  }
}

const viewDetail = (id) => {
  router.push(`/resource/${id}`)
}

const handleDownload = async (row) => {
  try {
    const response = await downloadResource(row.id)
    const blob = new Blob([response.data])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = row.fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch (error) {
    console.error('下载失败:', error)
  }
}

const handleUploadClick = () => {
  if (localStorage.getItem('token')) {
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

const API_BASE = '/api'

const getImagePreviewUrl = (id) => {
  return `${API_BASE}/resources/preview/${id}`
}

const getPreviewUrl = (id) => {
  return `${API_BASE}/resources/preview/${id}`
}

const handlePreview = (row) => {
  previewResource.value = row
  previewVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该资料吗？', '提示', {
      type: 'warning'
    })
    await deleteResource(row.id)
    ElMessage.success('删除成功')
    fetchResourceList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

// 点击走马灯卡片 → 跳转到详情页
const handleCardClick = (item) => {
  router.push(`/resource/${item.id}`)
}

onMounted(() => {
  fetchResourceList()
  fetchCategories()
  fetchCompetitionTypes()
  fetchTopResources()

  // 监听登录状态变化事件，退出登录时刷新数据清除收藏状态
  window.addEventListener('auth-change', () => {
    fetchResourceList()
    fetchTopResources()
  })
})
</script>

<style scoped>
.resource-list {
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

/* ==================== Hero 区域 ==================== */
.hero-section {
  position: relative;
  overflow: hidden;
  padding: 36px 40px 32px;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  z-index: 0;
}

.hero-bg::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 15% 85%, rgba(255,255,255,0.12) 0%, transparent 50%),
    radial-gradient(circle at 85% 15%, rgba(255,255,255,0.08) 0%, transparent 50%);
}

.hero-bg::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 30px;
  background: linear-gradient(to bottom, transparent, #f5f7fa);
}

.hero-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  max-width: 1200px;
  margin: 0 auto;
}

/* 左侧标题 */
.hero-left {
  flex-shrink: 0;
  max-width: 340px;
}

.hero-title {
  font-size: 32px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  letter-spacing: 1px;
  text-shadow: 0 2px 12px rgba(0,0,0,0.15);
}

.hero-icon {
  font-size: 36px;
}

.hero-subtitle {
  font-size: 15px;
  color: rgba(255,255,255,0.85);
  margin: 0 0 20px;
  letter-spacing: 0.5px;
  line-height: 1.6;
}

.hero-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-num {
  font-size: 28px;
  font-weight: 800;
  color: #fff;
}

.stat-label {
  font-size: 13px;
  color: rgba(255,255,255,0.7);
}

/* 右侧走马灯 */
.hero-right {
  flex: 1;
  min-width: 0;
}

.carousel-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: rgba(255,255,255,0.8);
  margin-bottom: 12px;
  padding-left: 4px;
}

.carousel-label .el-icon {
  font-size: 16px;
}

.hot-carousel {
  --el-carousel-item-padding: 0;
  background: transparent !important;
  box-shadow: none !important;
}

.hot-carousel :deep(.el-carousel__container) {
  height: 260px !important;
  background: transparent !important;
  box-shadow: none !important;
}

.hot-carousel :deep(.el-carousel__item) {
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent !important;
  transition: all 0.3s ease;
  box-shadow: none !important;
}

.hot-carousel :deep(.el-carousel__item.is-active) {
  z-index: 2;
}

.hot-carousel :deep(.el-carousel__item:not(.is-active)) {
  opacity: 0.4;
}

.hot-carousel :deep(.el-carousel__mask) {
  display: none !important;
}

.hot-carousel :deep(.el-carousel__arrow) {
  display: none !important;
}

.hot-carousel :deep(.el-carousel__indicators) {
  display: none !important;
}

/* 热门卡片 */
.hot-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 20px rgba(0,0,0,0.12);
  height: 240px;
  width: 200px;
  display: flex;
  flex-direction: column;
  margin: 0 auto;
}

.hot-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0,0,0,0.2);
}

.hot-card__preview {
  position: relative;
  width: 100%;
  height: 140px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.hot-card__preview .el-icon {
  display: block;
}

.hot-card__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.hot-card__placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 24px;
}

.hot-card__filetype {
  font-size: 12px;
  font-weight: 700;
  color: #909399;
  letter-spacing: 2px;
}

.hot-card__badge {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(102,126,234,0.9);
  backdrop-filter: blur(8px);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 20px;
}

.hot-card__badge .el-icon {
  font-size: 13px;
}

.hot-card__info {
  padding: 12px 14px 14px;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  text-align: center;
}

.hot-card__title {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hot-card__meta {
  display: flex;
  gap: 4px;
  margin-bottom: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.hot-card__desc {
  font-size: 12px;
  color: #909399;
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.carousel-empty {
  text-align: center;
  padding: 40px 20px;
  color: rgba(255,255,255,0.6);
}

.carousel-empty p {
  margin-top: 8px;
  font-size: 14px;
}

/* ==================== 筛选工具栏 ==================== */
.filter-toolbar {
  padding: 0 24px;
  margin-top: -8px;
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 12px;
  padding: 12px 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.filter-row--top {
  gap: 16px;
}

.filter-row--bottom {
  justify-content: flex-start;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-group--right {
  gap: 8px;
  margin-left: auto;
}

.search-wrapper {
  display: flex;
  align-items: center;
  gap: 0;
  flex: 1;
  max-width: 420px;
}

.search-input {
  width: 100%;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 8px 0 0 8px;
  height: 36px;
}

.search-btn {
  border-radius: 0 8px 8px 0;
  height: 36px;
  padding: 0 16px;
  font-size: 13px;
  font-weight: 500;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  flex-shrink: 0;
}

.search-btn:hover {
  opacity: 0.9;
}

.filter-select {
  width: 130px;
}

.filter-select--sm {
  width: 110px;
}

.filter-date {
  width: 140px;
}

.reset-btn {
  color: #909399;
  font-size: 13px;
  transition: color 0.3s;
}

.reset-btn:hover {
  color: #667eea;
}

/* ==================== 资料列表卡片 ==================== */
.list-card {
  margin: 20px 24px 24px;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.list-header__left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.list-header__title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.list-header__count {
  font-size: 13px;
  color: #909399;
}

.upload-btn {
  border-radius: 8px;
  font-weight: 500;
}

/* ==================== 表格样式 ==================== */
.styled-table {
  border-radius: 8px;
  overflow: hidden;
}

.styled-table :deep(.el-table__header th) {
  background: #f5f7fa;
  color: #606266;
  font-weight: 600;
  font-size: 14px;
  border-bottom: 2px solid #ebeef5;
}

.styled-table :deep(.el-table__body td) {
  border-bottom: 1px solid #f0f0f0;
}

.styled-table :deep(.el-table__row:hover td) {
  background: #f5f7fa;
}

.file-size-text {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 13px;
  color: #606266;
}

.count-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-weight: 600;
  color: #303133;
}

/* ==================== 预览缩略图 ==================== */
.preview-thumb {
  display: flex;
  justify-content: center;
  align-items: center;
}

.thumb-img {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  border: 2px solid #f0f0f0;
  transition: all 0.2s;
}

.thumb-img:hover {
  transform: scale(1.1);
  border-color: #667eea;
}

.preview-icon {
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
}

.preview-content {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.preview-image img {
  max-width: 100%;
  max-height: 500px;
  object-fit: contain;
}

.preview-video video {
  max-height: 500px;
}

/* ==================== 分页 ==================== */
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.pagination-wrapper :deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

/* ==================== 响应式 ==================== */
@media (max-width: 1200px) {
  .hero-content {
    flex-direction: column;
    align-items: flex-start;
  }
  .hero-left {
    max-width: 100%;
    margin-bottom: 20px;
  }
  .hero-right {
    width: 100%;
  }
  .filter-row--top {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  .filter-row--top .filter-group--right {
    margin-left: 0;
    justify-content: center;
  }
  .search-wrapper {
    max-width: 100%;
  }
  .filter-row--bottom .filter-group {
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 24px;
  }
  .hero-section {
    padding: 28px 20px 24px;
  }
  .list-card {
    margin: 16px 12px 20px;
    padding: 16px;
  }
}
</style>

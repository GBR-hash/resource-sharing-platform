<template>
  <div class="detail-page">
    <div class="detail-hero" v-if="resource">
      <div class="detail-hero__bg"></div>
      <div class="detail-hero__content">
        <el-button class="back-btn" @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h1 class="detail-hero__title">{{ resource.title }}</h1>
        <div class="detail-hero__meta">
          <span class="meta-item">
            <el-icon><User /></el-icon>
            {{ resource.uploaderName }}
          </span>
          <span class="meta-item">
            <el-icon><Clock /></el-icon>
            {{ formatDateTime(resource.createdAt) }}
          </span>
          <el-tag
            :type="resource.status === 1 ? 'success' : resource.status === 0 ? 'warning' : 'danger'"
            effect="dark"
            round
            size="small"
          >
            {{ resource.status === 1 ? '已发布' : resource.status === 0 ? '审核中' : '已拒绝' }}
          </el-tag>
        </div>
      </div>
    </div>

    <div class="detail-card" v-loading="loading">
      <div class="detail-grid" v-if="resource">
        <div class="detail-section">
          <div class="detail-section__title">
            <el-icon color="#667eea"><InfoFilled /></el-icon>
            <span>基本信息</span>
          </div>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-item__label">资料类型</span>
              <el-tag :type="getFileTypeTag(resource.fileType)" effect="plain" round size="small">
                {{ resource.fileType }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="info-item__label">文件大小</span>
              <span class="info-item__value">{{ formatFileSize(resource.fileSize) }}</span>
            </div>
            <div class="info-item">
              <span class="info-item__label">下载量</span>
              <span class="info-item__value info-item__value--highlight">
                <el-icon color="#409eff"><Download /></el-icon>
                {{ resource.downloadCount }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-item__label">资料分类</span>
              <span class="info-item__value">{{ resource.categoryName || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-item__label">竞赛类型</span>
              <span class="info-item__value">{{ resource.competitionTypeName || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-item__label">备注</span>
              <span class="info-item__value">{{ resource.remark || '-' }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <div class="detail-section__title">
            <el-icon color="#667eea"><Document /></el-icon>
            <span>资料描述</span>
          </div>
          <div class="description-box">
            {{ resource?.description || '暂无描述' }}
          </div>
        </div>

        <div class="detail-actions" v-if="resource.status === 1">
          <el-button type="primary" size="large" class="download-btn" @click="handleDownload">
            <el-icon><Download /></el-icon>
            下载资料
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Download, ArrowLeft, User, Clock, InfoFilled, Document } from '@element-plus/icons-vue'
import { getResourceById, downloadResource } from '@/api/resource'
import { ElMessage } from 'element-plus'

const route = useRoute()
const loading = ref(false)
const resource = ref(null)

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

const fetchResourceDetail = async () => {
  loading.value = true
  try {
    const res = await getResourceById(route.params.id)
    resource.value = res.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleDownload = async () => {
  try {
    const response = await downloadResource(resource.value.id)
    const blob = new Blob([response.data])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = resource.value.fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch (error) {
    console.error('下载失败:', error)
  }
}

onMounted(() => {
  fetchResourceDetail()
})
</script>

<style scoped>
.detail-page {
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

/* Hero */
.detail-hero {
  position: relative;
  overflow: hidden;
  padding: 40px 40px 36px;
}

.detail-hero__bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  z-index: 0;
}

.detail-hero__bg::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 80%, rgba(255,255,255,0.15) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255,255,255,0.1) 0%, transparent 50%);
}

.detail-hero__bg::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 30px;
  background: linear-gradient(to bottom, transparent, #f5f7fa);
}

.detail-hero__content {
  position: relative;
  z-index: 1;
  max-width: 900px;
  margin: 0 auto;
}

.back-btn {
  background: rgba(255,255,255,0.2);
  border: 1px solid rgba(255,255,255,0.3);
  color: #fff;
  border-radius: 8px;
  margin-bottom: 16px;
  backdrop-filter: blur(8px);
}

.back-btn:hover {
  background: rgba(255,255,255,0.3);
}

.detail-hero__title {
  font-size: 28px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 12px;
  letter-spacing: 1px;
  text-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.detail-hero__meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: rgba(255,255,255,0.85);
}

/* Card */
.detail-card {
  max-width: 900px;
  margin: -8px auto 32px;
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  position: relative;
  z-index: 2;
}

/* Sections */
.detail-section {
  margin-bottom: 28px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-section:last-of-type {
  border-bottom: none;
  margin-bottom: 0;
}

.detail-section__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 20px;
}

.detail-section__title .el-icon {
  font-size: 20px;
}

/* Info Grid */
.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px;
  background: #f8f9fa;
  border-radius: 10px;
}

.info-item__label {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.info-item__value {
  font-size: 15px;
  color: #303133;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.info-item__value--highlight {
  color: #409eff;
  font-weight: 700;
  font-size: 18px;
}

/* Description */
.description-box {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 10px;
  line-height: 1.8;
  color: #606266;
  font-size: 15px;
  min-height: 80px;
}

/* Actions */
.detail-actions {
  text-align: center;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.download-btn {
  min-width: 200px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  height: 48px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  transition: all 0.3s;
}

.download-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(102,126,234,0.4);
}

/* Responsive */
@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .detail-hero__title {
    font-size: 22px;
  }
  .detail-card {
    margin: -8px 12px 24px;
    padding: 20px;
  }
}
</style>

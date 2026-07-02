<template>
  <div class="resource-management">
    <!-- 页面标题栏 -->
    <div class="page-header">
      <div class="page-header__left">
        <h2 class="page-header__title">资料审核管理</h2>
        <span class="page-header__subtitle">管理和审核用户上传的资料</span>
      </div>
      <div class="page-header__right">
        <el-select
          v-model="filterStatus"
          placeholder="筛选状态"
          clearable
          class="filter-select"
          @change="fetchResourceList"
        >
          <el-option label="审核中" :value="0" />
          <el-option label="已发布" :value="1" />
          <el-option label="已拒绝" :value="2" />
        </el-select>
      </div>
    </div>

    <!-- 表格卡片 -->
    <div class="table-card">
      <el-table :data="resourceList" v-loading="loading" class="styled-table" stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="title" label="资料名称" min-width="200">
          <template #default="{ row }">
            <div class="resource-title">
              <el-icon class="resource-title__icon" :color="getFileIconColor(row.fileType)">
                <component :is="getFileIcon(row.fileType)" />
              </el-icon>
              <span>{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="uploaderName" label="上传人" width="120" align="center">
          <template #default="{ row }">
            <div class="uploader-cell">
              <div class="uploader-avatar">{{ row.uploaderName?.charAt(0)?.toUpperCase() }}</div>
              <span>{{ row.uploaderName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getFileTypeTag(row.fileType)" effect="plain" round size="small">
              {{ row.fileType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="110" align="center">
          <template #default="{ row }">
            <span class="file-size">{{ formatFileSize(row.fileSize) }}</span>
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
        <el-table-column prop="status" label="状态" width="110" align="center">
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
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                v-if="row.status === 0"
                type="success"
                size="small"
                round
                @click="handleApprove(row)"
              >
                <el-icon><Check /></el-icon> 通过
              </el-button>
              <el-button
                v-if="row.status === 0"
                type="danger"
                size="small"
                round
                @click="handleReject(row)"
              >
                <el-icon><Close /></el-icon> 拒绝
              </el-button>
              <el-button
                type="danger"
                size="small"
                round
                plain
                @click="handleDelete(row)"
              >
                <el-icon><Delete /></el-icon> 删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, Delete, Download, Document, Picture, VideoPlay, Files } from '@element-plus/icons-vue'
import { getResourceList, approveResource, rejectResource, deleteResource } from '@/api/admin'

const loading = ref(false)
const resourceList = ref([])
const filterStatus = ref(null)

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const getFileIcon = (type) => {
  const iconMap = {
    'pdf': Document,
    'doc': Document,
    'docx': Document,
    'xls': Files,
    'xlsx': Files,
    'ppt': Files,
    'pptx': Files,
    'mp4': VideoPlay,
    'avi': VideoPlay,
    'jpg': Picture,
    'png': Picture,
    'image': Picture,
    'video': VideoPlay,
    'document': Document
  }
  return iconMap[type?.toLowerCase()] || Document
}

const getFileIconColor = (type) => {
  const colorMap = {
    'pdf': '#f56c6c',
    'doc': '#409eff',
    'docx': '#409eff',
    'xls': '#67c23a',
    'xlsx': '#67c23a',
    'ppt': '#e6a23c',
    'pptx': '#e6a23c',
    'mp4': '#909399',
    'avi': '#909399',
    'jpg': '#67c23a',
    'png': '#67c23a',
    'image': '#67c23a',
    'video': '#909399',
    'document': '#409eff'
  }
  return colorMap[type?.toLowerCase()] || '#909399'
}

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

const fetchResourceList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size
    }
    if (filterStatus.value !== null && filterStatus.value !== undefined) {
      params.status = filterStatus.value
    }
    const res = await getResourceList(params)
    resourceList.value = res.data.content
    pagination.total = res.data.totalElements
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm('确定要通过该资料吗？', '提示', {
      type: 'warning'
    })
    await approveResource(row.id)
    ElMessage.success('审核通过')
    fetchResourceList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handleReject = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '提示', {
      type: 'warning'
    })
    await rejectResource(row.id, value)
    ElMessage.success('已拒绝')
    fetchResourceList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
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

onMounted(() => {
  fetchResourceList()
})
</script>

<style scoped>
.resource-management {
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

/* 页面标题栏 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header__title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 4px 0;
}

.page-header__subtitle {
  font-size: 14px;
  color: #909399;
}

.filter-select {
  width: 160px;
}

/* 表格卡片 */
.table-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

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

/* 资料名称 */
.resource-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: #303133;
}

.resource-title__icon {
  font-size: 18px;
  flex-shrink: 0;
}

/* 上传人 */
.uploader-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.uploader-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

/* 文件大小 */
.file-size {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 13px;
  color: #606266;
}

/* 下载量 */
.count-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-weight: 600;
  color: #303133;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: center;
}

/* 分页 */
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.pagination-wrapper :deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
</style>

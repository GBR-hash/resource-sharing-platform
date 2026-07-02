<template>
  <div class="user-management">
    <!-- 页面标题栏 -->
    <div class="page-header">
      <div class="page-header__left">
        <h2 class="page-header__title">用户管理</h2>
        <span class="page-header__subtitle">管理系统中的所有用户账号</span>
      </div>
    </div>

    <!-- 表格卡片 -->
    <div class="table-card">
      <el-table :data="userList" v-loading="loading" class="styled-table" stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" width="140">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-avatar" :style="{ background: getAvatarColor(row.username) }">
                {{ row.username?.charAt(0)?.toUpperCase() }}
              </div>
              <span class="user-name">{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="真实姓名" width="130" align="center">
          <template #default="{ row }">
            <span>{{ row.realName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="200">
          <template #default="{ row }">
            <div class="email-cell">
              <el-icon color="#909399"><Message /></el-icon>
              <span>{{ row.email || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140" align="center">
          <template #default="{ row }">
            <span class="phone-text">{{ row.phone || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="110" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.role === 1 ? 'danger' : 'info'"
              effect="dark"
              round
              size="small"
            >
              {{ row.role === 1 ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <div class="status-cell">
              <div class="status-dot" :class="row.status === 1 ? 'status-dot--active' : 'status-dot--disabled'"></div>
              <span>{{ row.status === 1 ? '正常' : '禁用' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                v-if="row.status === 0"
                type="success"
                size="small"
                round
                @click="handleUpdateStatus(row, 1)"
              >
                <el-icon><Unlock /></el-icon> 启用
              </el-button>
              <el-button
                v-else-if="row.status === 1 && row.role !== 1"
                type="warning"
                size="small"
                round
                @click="handleUpdateStatus(row, 0)"
              >
                <el-icon><Lock /></el-icon> 禁用
              </el-button>
              <el-button
                v-if="row.role !== 1"
                type="primary"
                size="small"
                round
                plain
                @click="handleUpdateRole(row, 1)"
              >
                <el-icon><UserFilled /></el-icon> 设为管理员
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
          @size-change="fetchUserList"
          @current-change="fetchUserList"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Message, Unlock, Lock, UserFilled } from '@element-plus/icons-vue'
import { getUserList, updateUserStatus, updateUserRole } from '@/api/admin'

const loading = ref(false)
const userList = ref([])

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

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

const fetchUserList = async () => {
  loading.value = true
  try {
    const res = await getUserList({
      page: pagination.page - 1,
      size: pagination.size
    })
    userList.value = res.data.content
    pagination.total = res.data.totalElements
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleUpdateStatus = async (row, status) => {
  try {
    await ElMessageBox.confirm(
      `确定要${status === 1 ? '启用' : '禁用'}该用户吗？`,
      '提示',
      { type: 'warning' }
    )
    await updateUserStatus(row.id, status)
    ElMessage.success('操作成功')
    fetchUserList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handleUpdateRole = async (row, role) => {
  try {
    await ElMessageBox.confirm('确定要设为管理员吗？', '提示', {
      type: 'warning'
    })
    await updateUserRole(row.id, role)
    ElMessage.success('操作成功')
    fetchUserList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

onMounted(() => {
  fetchUserList()
})
</script>

<style scoped>
.user-management {
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

/* 用户单元格 */
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-name {
  font-weight: 500;
  color: #303133;
}

/* 邮箱 */
.email-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 13px;
}

/* 手机号 */
.phone-text {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 13px;
  color: #606266;
}

/* 状态 */
.status-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 14px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot--active {
  background: #67c23a;
  box-shadow: 0 0 6px rgba(103, 194, 58, 0.4);
}

.status-dot--disabled {
  background: #f56c6c;
  box-shadow: 0 0 6px rgba(245, 108, 108, 0.4);
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

<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-card--blue">
        <div class="stat-card__icon">
          <el-icon :size="32"><User /></el-icon>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ statistics.userCount || 0 }}</div>
          <div class="stat-card__label">用户总数</div>
        </div>
        <div class="stat-card__bg">
          <el-icon :size="80" class="stat-card__bg-icon"><User /></el-icon>
        </div>
      </div>

      <div class="stat-card stat-card--green">
        <div class="stat-card__icon">
          <el-icon :size="32"><Document /></el-icon>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ statistics.resourceCount || 0 }}</div>
          <div class="stat-card__label">资料总数</div>
        </div>
        <div class="stat-card__bg">
          <el-icon :size="80" class="stat-card__bg-icon"><Document /></el-icon>
        </div>
      </div>

      <div class="stat-card stat-card--orange">
        <div class="stat-card__icon">
          <el-icon :size="32"><Download /></el-icon>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ statistics.downloadCount || 0 }}</div>
          <div class="stat-card__label">总下载量</div>
        </div>
        <div class="stat-card__bg">
          <el-icon :size="80" class="stat-card__bg-icon"><Download /></el-icon>
        </div>
      </div>

      <div class="stat-card stat-card--red">
        <div class="stat-card__icon">
          <el-icon :size="32"><Warning /></el-icon>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ statistics.pendingCount || 0 }}</div>
          <div class="stat-card__label">待审核资料</div>
        </div>
        <div class="stat-card__bg">
          <el-icon :size="80" class="stat-card__bg-icon"><Warning /></el-icon>
        </div>
      </div>
    </div>

    <!-- 快捷操作 & 数据概览 -->
    <div class="dashboard-grid">
      <div class="dashboard-card">
        <div class="dashboard-card__header">
          <el-icon class="dashboard-card__header-icon" color="#409eff"><Operation /></el-icon>
          <span>快捷操作</span>
        </div>
        <div class="dashboard-card__body">
          <div class="action-list">
            <div class="action-item" @click="$router.push('/admin/users')">
              <div class="action-item__icon action-item__icon--blue">
                <el-icon :size="24"><UserFilled /></el-icon>
              </div>
              <div class="action-item__text">用户管理</div>
            </div>
            <div class="action-item" @click="$router.push('/admin/resources')">
              <div class="action-item__icon action-item__icon--green">
                <el-icon :size="24"><Files /></el-icon>
              </div>
              <div class="action-item__text">资料审核</div>
            </div>
            <div class="action-item" @click="fetchStatistics">
              <div class="action-item__icon action-item__icon--orange">
                <el-icon :size="24"><Refresh /></el-icon>
              </div>
              <div class="action-item__text">刷新统计</div>
            </div>
          </div>
        </div>
      </div>

      <div class="dashboard-card">
        <div class="dashboard-card__header">
          <el-icon class="dashboard-card__header-icon" color="#67c23a"><DataAnalysis /></el-icon>
          <span>审核概览</span>
        </div>
        <div class="dashboard-card__body">
          <div class="overview-list">
            <div class="overview-item">
              <div class="overview-item__left">
                <div class="overview-dot overview-dot--success"></div>
                <span>已通过</span>
              </div>
              <span class="overview-item__value">{{ statistics.approvedCount || 0 }}</span>
            </div>
            <div class="overview-item">
              <div class="overview-item__left">
                <div class="overview-dot overview-dot--warning"></div>
                <span>待审核</span>
              </div>
              <span class="overview-item__value">{{ statistics.pendingCount || 0 }}</span>
            </div>
            <div class="overview-item">
              <div class="overview-item__left">
                <div class="overview-dot overview-dot--danger"></div>
                <span>已拒绝</span>
              </div>
              <span class="overview-item__value">{{ statistics.rejectedCount || 0 }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { User, Document, Download, Warning, Operation, UserFilled, Files, Refresh, DataAnalysis } from '@element-plus/icons-vue'
import { getStatistics } from '@/api/admin'

const statistics = ref({})

const fetchStatistics = async () => {
  try {
    const res = await getStatistics()
    statistics.value = res.data
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  fetchStatistics()
})
</script>

<style scoped>
.dashboard {
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

/* 统计卡片 */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  position: relative;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  overflow: hidden;
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s, box-shadow 0.3s;
  cursor: default;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.stat-card--blue {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-card--green {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-card--orange {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.stat-card--red {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-card__icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card__content {
  flex: 1;
}

.stat-card__value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-card__label {
  font-size: 14px;
  opacity: 0.9;
  margin-top: 4px;
}

.stat-card__bg {
  position: absolute;
  right: -10px;
  bottom: -10px;
  opacity: 0.15;
}

.stat-card__bg-icon {
  transform: rotate(-15deg);
}

/* 网格布局 */
.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.dashboard-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.dashboard-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 24px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #f0f0f0;
}

.dashboard-card__header-icon {
  font-size: 20px;
}

.dashboard-card__body {
  padding: 24px;
}

/* 快捷操作 */
.action-list {
  display: flex;
  gap: 16px;
}

.action-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px 16px;
  border-radius: 12px;
  background: #f8f9fa;
  cursor: pointer;
  transition: all 0.3s;
}

.action-item:hover {
  background: #ecf5ff;
  transform: translateY(-2px);
}

.action-item__icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.action-item__icon--blue {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.action-item__icon--green {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.action-item__icon--orange {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.action-item__text {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

/* 审核概览 */
.overview-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.overview-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-radius: 10px;
  background: #f8f9fa;
  transition: background 0.3s;
}

.overview-item:hover {
  background: #f0f2f5;
}

.overview-item__left {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  color: #606266;
}

.overview-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.overview-dot--success {
  background: #67c23a;
}

.overview-dot--warning {
  background: #e6a23c;
}

.overview-dot--danger {
  background: #f56c6c;
}

.overview-item__value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stat-cards {
    grid-template-columns: 1fr;
  }
  .action-list {
    flex-direction: column;
  }
}
</style>

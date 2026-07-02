<template>
  <div class="upload-page">
    <div class="upload-hero">
      <div class="upload-hero__bg"></div>
      <div class="upload-hero__content">
        <h1 class="upload-hero__title">
          <el-icon class="upload-hero__icon"><UploadFilled /></el-icon>
          上传资料
        </h1>
        <p class="upload-hero__subtitle">分享您的竞赛成果，让更多人受益</p>
      </div>
    </div>

    <div class="upload-card">
      <el-form
        :model="uploadForm"
        :rules="rules"
        ref="formRef"
        label-width="100px"
        class="upload-form"
      >
        <div class="form-section">
          <div class="form-section__title">
            <el-icon color="#667eea"><Edit /></el-icon>
            <span>基本信息</span>
          </div>
          <el-form-item label="资料标题" prop="title">
            <el-input v-model="uploadForm.title" placeholder="请输入资料标题" />
          </el-form-item>
          <el-form-item label="资料描述" prop="description">
            <el-input
              v-model="uploadForm.description"
              type="textarea"
              :rows="4"
              placeholder="请输入资料描述"
            />
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="uploadForm.remark"
              type="textarea"
              :rows="2"
              placeholder="请输入备注信息（选填）"
            />
          </el-form-item>
        </div>

        <div class="form-section">
          <div class="form-section__title">
            <el-icon color="#667eea"><Folder /></el-icon>
            <span>分类信息</span>
          </div>
          <el-form-item label="资料分类" prop="categoryId">
            <el-select v-model="uploadForm.categoryId" placeholder="请选择资料分类" style="width: 100%">
              <el-option
                v-for="item in categories"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="竞赛类型" prop="competitionTypeId">
            <el-select v-model="uploadForm.competitionTypeId" placeholder="请选择竞赛类型" style="width: 100%">
              <el-option
                v-for="item in competitionTypes"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-section">
          <div class="form-section__title">
            <el-icon color="#667eea"><Paperclip /></el-icon>
            <span>上传文件</span>
          </div>
          <el-form-item label="上传文件" prop="file">
            <div class="file-upload-area" :class="{ 'has-file': uploadForm.file }">
              <el-upload
                ref="uploadRef"
                :auto-upload="false"
                :on-change="handleFileChange"
                :limit="1"
                drag
              >
                <div class="upload-dragger">
                  <el-icon class="upload-dragger__icon"><Upload /></el-icon>
                  <div class="upload-dragger__text">将文件拖到此处，或<em>点击上传</em></div>
                  <div class="upload-dragger__tip">支持格式：doc, docx, pdf, ppt, pptx, xls, xlsx, jpg, png, mp4, avi 等，最大 50MB</div>
                </div>
              </el-upload>
              <div v-if="uploadForm.file" class="file-info">
                <el-icon color="#67c23a"><SuccessFilled /></el-icon>
                <span class="file-info__name">{{ uploadForm.file.name }}</span>
                <span class="file-info__size">{{ formatFileSize(uploadForm.file.size) }}</span>
              </div>
            </div>
          </el-form-item>
        </div>

        <div class="form-actions">
          <el-button type="primary" :loading="loading" size="large" class="submit-btn" @click="handleSubmit">
            <el-icon><Upload /></el-icon>
            立即上传
          </el-button>
          <el-button size="large" @click="$router.back()">取消</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Upload, Edit, Folder, Paperclip, SuccessFilled } from '@element-plus/icons-vue'
import { getCategories, getCompetitionTypes, uploadResource } from '@/api/resource'

const formRef = ref(null)
const uploadRef = ref(null)
const loading = ref(false)
const categories = ref([])
const competitionTypes = ref([])

const uploadForm = reactive({
  title: '',
  description: '',
  remark: '',
  categoryId: null,
  competitionTypeId: null,
  file: null
})

const rules = {
  title: [
    { required: true, message: '请输入资料标题', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择资料分类', trigger: 'change' }
  ],
  competitionTypeId: [
    { required: true, message: '请选择竞赛类型', trigger: 'change' }
  ]
}

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

const handleFileChange = (file) => {
  uploadForm.file = file.raw
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    if (!uploadForm.file) {
      ElMessage.warning('请选择要上传的文件')
      return
    }

    loading.value = true
    try {
      const formData = new FormData()
      formData.append('file', uploadForm.file)
      formData.append('title', uploadForm.title)
      formData.append('description', uploadForm.description)
      formData.append('remark', uploadForm.remark)
      formData.append('categoryId', uploadForm.categoryId)
      formData.append('competitionTypeId', uploadForm.competitionTypeId)

      await uploadResource(formData)
      ElMessage.success('上传成功')
      formRef.value.resetFields()
      uploadRef.value?.clearFiles()
      uploadForm.file = null
    } catch (error) {
      console.error(error)
    } finally {
      loading.value = false
    }
  })
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

onMounted(() => {
  fetchCategories()
  fetchCompetitionTypes()
})
</script>

<style scoped>
.upload-page {
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

/* Hero */
.upload-hero {
  position: relative;
  overflow: hidden;
  padding: 40px 40px 36px;
}

.upload-hero__bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  z-index: 0;
}

.upload-hero__bg::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 80%, rgba(255,255,255,0.15) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255,255,255,0.1) 0%, transparent 50%);
}

.upload-hero__bg::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 30px;
  background: linear-gradient(to bottom, transparent, #f5f7fa);
}

.upload-hero__content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.upload-hero__title {
  font-size: 32px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  letter-spacing: 2px;
  text-shadow: 0 2px 12px rgba(0,0,0,0.15);
}

.upload-hero__icon {
  font-size: 36px;
}

.upload-hero__subtitle {
  font-size: 15px;
  color: rgba(255,255,255,0.85);
  margin: 0;
  letter-spacing: 1px;
}

/* Card */
.upload-card {
  max-width: 800px;
  margin: -8px auto 32px;
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  position: relative;
  z-index: 2;
}

.upload-form {
  max-width: 100%;
}

/* Form Sections */
.form-section {
  margin-bottom: 28px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.form-section:last-of-type {
  border-bottom: none;
  margin-bottom: 0;
}

.form-section__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 20px;
}

.form-section__title .el-icon {
  font-size: 20px;
}

/* File Upload */
.file-upload-area {
  width: 100%;
}

.upload-dragger {
  padding: 32px 24px;
  text-align: center;
  border: 2px dashed #dcdfe6;
  border-radius: 12px;
  background: #fafafa;
  transition: all 0.3s;
  cursor: pointer;
}

.upload-dragger:hover {
  border-color: #667eea;
  background: #f5f3ff;
}

.upload-dragger__icon {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 12px;
}

.upload-dragger__text {
  font-size: 15px;
  color: #606266;
  margin-bottom: 8px;
}

.upload-dragger__text em {
  color: #667eea;
  font-style: normal;
  font-weight: 500;
}

.upload-dragger__tip {
  font-size: 12px;
  color: #909399;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 16px;
  background: #f0f9eb;
  border-radius: 8px;
  font-size: 14px;
}

.file-info__name {
  font-weight: 500;
  color: #303133;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-info__size {
  color: #909399;
  font-size: 13px;
  flex-shrink: 0;
}

/* Actions */
.form-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 8px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.submit-btn {
  min-width: 160px;
  border-radius: 8px;
  font-weight: 600;
}

/* Override el-upload drag */
:deep(.el-upload-dragger) {
  padding: 0;
  border: none;
  background: transparent;
}

:deep(.el-upload-dragger:hover) {
  border: none;
}

:deep(.el-upload__tip) {
  display: none;
}
</style>

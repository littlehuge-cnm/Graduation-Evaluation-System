<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Document, Reading, Medal, InfoFilled, CircleCheckFilled, Clock, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user.js'
import { getStudentById, getStudentDocuments } from '@/api/student.js'
import { getScoreRecordList } from '@/api/scoreRecord.js'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const records = ref([])
const documents = ref([])
const studentInfo = ref(null)

const progressItems = [
  { label: '任务书', itemType: '任务书', type: 'doc' },
  { label: '指导书', itemType: '指导书', type: 'doc' },
  { label: '开题报告', itemType: '开题报告成绩', type: 'score' },
  { label: '外文翻译', itemType: '外文翻译', type: 'score' },
  { label: '中期检查', itemType: '中期检查成绩', type: 'score' },
  { label: '指导评语', itemType: '指导评语', type: 'score' },
  { label: '评阅评分', itemType: '评阅评语', type: 'score' },
  { label: '答辩记录', itemType: '答辩记录', type: 'score' },
  { label: '毕业答辩', itemType: '毕业答辩成绩', type: 'score' },
  { label: '总评成绩', itemType: '委员会评定', type: 'score' }
]

function isCompleted(item) {
  if (item.type === 'doc') {
    return !!documents.value.find(d => d.docType === item.itemType)?.content
  }
  const record = records.value.find(r => r.itemType === item.itemType)
  if (!record) return false
  if (item.itemType === '答辩记录') {
    return !!record.defenseRecord
  }
  return record.score !== null && record.score !== undefined
}

function getStatusType(item) {
  return isCompleted(item) ? 'success' : 'info'
}

const completedCount = computed(() => {
  let count = 0
  progressItems.forEach(item => {
    if (isCompleted(item)) count++
  })
  return count
})

const totalCount = progressItems.length

const progressPercent = computed(() => {
  return Math.round((completedCount.value / totalCount) * 100)
})

const finalScore = computed(() => {
  const committee = records.value.find(r => r.itemType === '委员会评定')
  if (committee?.score !== null && committee?.score !== undefined) {
    return committee.score
  }
  return null
})

const finalGrade = computed(() => {
  const committee = records.value.find(r => r.itemType === '委员会评定')
  return committee?.grade || '未评定'
})

const pendingItems = computed(() => {
  return progressItems.filter(item => !isCompleted(item))
})

function navigateTo(path) {
  router.push(path)
}

async function loadData() {
  loading.value = true
  try {
    const studentNo = userStore.username
    const [infoRes, recordRes, docRes] = await Promise.all([
      getStudentById(studentNo),
      getScoreRecordList(studentNo),
      getStudentDocuments(studentNo)
    ])
    studentInfo.value = infoRes
    records.value = recordRes || []
    documents.value = docRes || []
  } catch (error) {
    console.error('加载数据失败', error)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="dashboard-container" v-loading="loading">
    <el-page-header title="控制台" :icon="null" />

    <el-card class="welcome-card" style="margin-top: 16px;">
      <div class="welcome-content">
        <div class="welcome-avatar">
          <el-icon :size="48">
            <User />
          </el-icon>
        </div>
        <div class="welcome-text">
          <h2>{{ userStore.name }}同学，你好！</h2>
          <p class="welcome-info">学号：{{ userStore.username }} | 专业：{{ studentInfo?.major || '-' }} | 班级：{{
            studentInfo?.className || '-' }}</p>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover" @click="navigateTo('/student/manual')">
          <div class="stat-content">
            <div class="stat-icon progress-icon">
              <el-icon>
                <Reading />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">完成进度</div>
              <div class="stat-value">{{ completedCount }}/{{ totalCount }}</div>
              <el-progress :percentage="progressPercent" :stroke-width="6" :show-text="false"
                style="margin-top: 8px;" />
              <div class="stat-progress-text">{{ progressPercent }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover" @click="navigateTo('/student/manual#section-final')">
          <div class="stat-content">
            <div class="stat-icon score-icon">
              <el-icon>
                <Medal />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">总评成绩</div>
              <div class="stat-value score-value">{{ finalScore !== null ? finalScore : '--' }}</div>
              <el-tag v-if="finalGrade !== '未评定'" size="small" type="success" class="grade-tag">{{ finalGrade
              }}</el-tag>
              <el-tag v-else size="small" type="info" class="grade-tag">{{ finalGrade }}</el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover" @click="navigateTo('/student/profile')">
          <div class="stat-content">
            <div class="stat-icon info-icon">
              <el-icon>
                <InfoFilled />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">待完成事项</div>
              <div class="stat-value">{{ pendingItems.length }}</div>
              <div class="stat-sub">{{ pendingItems.length > 0 ? '还有任务未完成' : '所有任务已完成' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover" @click="navigateTo('/student/manual')">
          <div class="stat-content">
            <div class="stat-icon manual-icon">
              <el-icon>
                <Document />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">评价手册</div>
              <div class="stat-value action-text">查看</div>
              <div class="stat-sub">查看完整评价手册</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :md="16">
        <el-card class="progress-card">
          <template #header>
            <div class="card-header">
              <span>评价任务进度</span>
            </div>
          </template>
          <div class="progress-grid">
            <div v-for="item in progressItems" :key="item.itemType" class="progress-item" :class="getStatusType(item)">
              <div class="progress-status">
                <el-icon v-if="isCompleted(item)" class="check-icon">
                  <CircleCheckFilled />
                </el-icon>
                <el-icon v-else class="pending-icon">
                  <Clock />
                </el-icon>
              </div>
              <div class="progress-name">{{ item.label }}</div>
              <el-tag size="small" :type="getStatusType(item)" effect="plain">
                {{ isCompleted(item) ? '已完成' : '未完成' }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card class="shortcut-card">
          <template #header>
            <div class="card-header">
              <span>快捷导航</span>
            </div>
          </template>
          <div class="shortcut-list">
            <div class="shortcut-item" @click="navigateTo('/student/profile')">
              <div class="shortcut-icon profile-icon">
                <el-icon>
                  <User />
                </el-icon>
              </div>
              <div class="shortcut-info">
                <div class="shortcut-title">个人信息</div>
                <div class="shortcut-desc">查看基本信息和分组</div>
              </div>
              <el-icon class="arrow-icon">
                <ArrowRight />
              </el-icon>
            </div>
            <el-divider />
            <div class="shortcut-item" @click="navigateTo('/student/manual')">
              <div class="shortcut-icon manual-shortcut-icon">
                <el-icon>
                  <Document />
                </el-icon>
              </div>
              <div class="shortcut-info">
                <div class="shortcut-title">评价手册</div>
                <div class="shortcut-desc">查看各环节成绩和评语</div>
              </div>
              <el-icon class="arrow-icon">
                <ArrowRight />
              </el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard-container {
  padding-bottom: 20px;
}

.welcome-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

:deep(.welcome-card .el-card__body) {
  padding: 24px;
}

.welcome-content {
  display: flex;
  align-items: center;
  gap: 20px;
  color: white;
}

.welcome-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  flex-shrink: 0;
}

.welcome-text h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.welcome-info {
  margin: 8px 0 0 0;
  opacity: 0.9;
  font-size: 14px;
}

.stats-row {
  margin: 16px 0;
}

.stat-card {
  height: 100%;
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: white;
  flex-shrink: 0;
}

.progress-icon {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.score-icon {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.info-icon {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #666 !important;
}

.manual-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.score-value {
  color: #f56c6c;
}

.action-text {
  color: #409eff;
  font-size: 24px;
}

.stat-progress-text {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.grade-tag {
  margin-top: 6px;
}

.stat-sub {
  font-size: 13px;
  color: #909399;
  margin-top: 6px;
}

.progress-card,
.shortcut-card {
  margin-bottom: 16px;
}

.card-header {
  font-weight: 600;
  font-size: 16px;
}

.progress-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.progress-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  background-color: #f5f7fa;
  border-radius: 8px;
  border: 2px solid transparent;
  transition: all 0.2s;
}

.progress-item.success {
  background-color: #f0f9eb;
  border-color: #e1f3d8;
}

.progress-item.info {
  background-color: #f4f4f5;
  border-color: #e9e9eb;
}

.progress-status {
  font-size: 28px;
}

.check-icon {
  color: #67c23a;
}

.pending-icon {
  color: #909399;
}

.progress-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  text-align: center;
}

.shortcut-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.shortcut-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 8px;
  cursor: pointer;
  border-radius: 6px;
  transition: background-color 0.2s;
}

.shortcut-item:hover {
  background-color: #f5f7fa;
}

.shortcut-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: white;
  flex-shrink: 0;
}

.profile-icon {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.manual-shortcut-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.shortcut-info {
  flex: 1;
}

.shortcut-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 2px;
}

.shortcut-desc {
  font-size: 12px;
  color: #909399;
}

.arrow-icon {
  color: #c0c4cc;
  font-size: 16px;
}

:deep(.el-divider) {
  margin: 0;
}
</style>

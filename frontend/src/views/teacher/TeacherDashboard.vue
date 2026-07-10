<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { User, Reading, Collection, Check, WarningFilled, DataAnalysis, DocumentCopy, EditPen, Notebook, Medal, OfficeBuilding, Connection, Timer } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user.js'
import { getTeacherStudents } from '@/api/teacher.js'
import { getTeacherGroupList } from '@/api/teacherGroup.js'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const stats = reactive({
  superviseCount: 0,
  reviewCount: 0,
  pendingCount: 0,
  completedCount: 0
})

const identityTags = computed(() => {
  return userStore.identities.map((identity) => {
    const typeMap = {
      '组长': 'danger',
      '秘书': 'warning',
      '指导教师': 'success',
      '评阅教师': 'primary',
      '普通成员': 'info'
    }
    return { name: identity, type: typeMap[identity] || 'info' }
  })
})

const quickLinks = computed(() => {
  const links = []
  if (userStore.identities.includes('指导教师')) {
    links.push({ name: '评价手册查看', icon: DocumentCopy, path: '/teacher/supervise/manual-review', color: '#36cfc9' })
    links.push({ name: '任务书/指导书填写', icon: Notebook, path: '/teacher/supervise/documents', color: '#e6a23c' })
    links.push({ name: '外文翻译评定', icon: EditPen, path: '/teacher/supervise/translation', color: '#909399' })
    links.push({ name: '指导教师评定', icon: Medal, path: '/teacher/supervise/comment', color: '#67c23a' })
  }
  if (userStore.identities.includes('评阅教师')) {
    links.push({ name: '评阅教师评定', icon: Medal, path: '/teacher/review/comment', color: '#f56c6c' })
  }
  if (userStore.identities.includes('组长')) {
    links.push({ name: '开题报告评定', icon: Connection, path: '/teacher/group/opening', color: '#409eff' })
    links.push({ name: '中期检查评定', icon: Timer, path: '/teacher/group/midterm', color: '#b37feb' })
    links.push({ name: '毕业答辩评定', icon: Medal, path: '/teacher/group/defense', color: '#ff7a45' })
  }
  links.unshift({ name: '个人信息', icon: User, path: '/teacher/profile', color: '#909399' })
  return links
})

async function loadStats() {
  loading.value = true
  try {
    const [superviseRes, reviewRes] = await Promise.all([
      getTeacherStudents(userStore.username, '指导'),
      getTeacherStudents(userStore.username, '评阅')
    ])
    stats.superviseCount = (superviseRes || []).length
    stats.reviewCount = (reviewRes || []).length
  } catch (error) {
    console.error('加载统计数据失败', error)
  } finally {
    loading.value = false
  }
}

function goTo(path) {
  router.push(path)
}

onMounted(() => {
  loadStats()
})
</script>

<template>
  <div class="dashboard-container" v-loading="loading">
    <el-page-header title="控制台" />

    <div class="welcome-section">
      <div class="welcome-text">
        <h2>欢迎回来，{{ userStore.name }}</h2>
        <p>工号：{{ userStore.username }}</p>
        <div class="identity-tags">
          <el-tag v-for="tag in identityTags" :key="tag.name" :type="tag.type" size="small" effect="dark"
            style="margin-right: 8px;">
            {{ tag.name }}
          </el-tag>
        </div>
      </div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="8" :md="6">
        <el-card shadow="hover" class="stat-card" @click="goTo('/teacher/profile')">
          <div class="stat-icon" style="background: #f0f9ff; color: #67c23a;">
            <el-icon size="28">
              <User />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.superviseCount }}</div>
            <div class="stat-label">指导学生</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <el-card shadow="hover" class="stat-card" @click="goTo('/teacher/profile')">
          <div class="stat-icon" style="background: #fef0f0; color: #f56c6c;">
            <el-icon size="28">
              <Reading />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.reviewCount }}</div>
            <div class="stat-label">评阅学生</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-row">
      <el-col :xs="24" :md="14">
        <el-card class="section-card">
          <template #header>
            <div class="card-header">
              <span><el-icon style="margin-right: 8px;">
                  <DataAnalysis />
                </el-icon> 快捷入口</span>
            </div>
          </template>
          <div class="quick-links" v-if="quickLinks.length > 0">
            <div v-for="link in quickLinks" :key="link.path" class="quick-link-item" @click="goTo(link.path)">
              <div class="link-icon" :style="{ background: link.color + '15', color: link.color }">
                <el-icon size="22">
                  <component :is="link.icon" />
                </el-icon>
              </div>
              <div class="link-name">{{ link.name }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无可用功能" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-card class="section-card">
          <template #header>
            <div class="card-header">
              <span><el-icon style="margin-right: 8px;">
                  <DocumentCopy />
                </el-icon> 功能说明</span>
            </div>
          </template>
          <div class="intro-content">
            <p>根据您的身份，可使用以下功能：</p>
            <ul>
              <li v-if="userStore.identities.includes('指导教师')">
                <strong>指导教师：</strong>填写任务书/指导书，评定外文翻译成绩，填写指导评语，查看指导学生评价手册
              </li>
              <li v-if="userStore.identities.includes('评阅教师')"><strong>评阅教师：</strong>填写评阅评语和成绩</li>
              <li v-if="userStore.identities.includes('组长')"><strong>组长：</strong>组织开题、中期、答辩环节，评定小组成绩，确认最终成绩</li>
              <li v-if="userStore.identities.includes('秘书')"><strong>秘书：</strong>录入答辩记录</li>
            </ul>
            <p>请按照环节顺序完成各项评定工作，确保成绩准确无误。</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard-container {
  padding: 0;
}

.welcome-section {
  margin: 20px 0;
  padding: 24px;
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  border-radius: 8px;
  color: white;
}

.welcome-text h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
}

.welcome-text p {
  margin: 0 0 12px 0;
  font-size: 14px;
  opacity: 0.9;
}

.identity-tags {
  margin-top: 8px;
}

.stats-row {
  margin-bottom: 20px;

}

.stat-card {
  cursor: pointer;
  margin-bottom: 16px;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  padding: 4px;
  padding-left: 20px;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.content-row {
  margin-bottom: 20px;
}

.section-card {
  margin-bottom: 16px;
  height: 100%;
}

.card-header {
  font-weight: 600;
  font-size: 16px;
  display: flex;
  align-items: center;
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.quick-link-item {
  text-align: center;
  cursor: pointer;
  padding: 16px 8px;
  border-radius: 8px;
  transition: all 0.2s;
}

.quick-link-item:hover {
  background: #f5f7fa;
}

.link-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 8px;
}

.link-name {
  font-size: 13px;
  color: #606266;
}

.intro-content p {
  margin: 0 0 12px 0;
  line-height: 1.8;
  color: #606266;
  font-size: 14px;
}

.intro-content ul {
  margin: 0 0 12px 0;
  padding-left: 20px;
  line-height: 2;
  color: #606266;
  font-size: 14px;
}

.intro-content li strong {
  color: #303133;
}

@media (max-width: 768px) {
  .quick-links {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>

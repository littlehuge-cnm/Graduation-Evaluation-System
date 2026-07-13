<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { User, Reading, DataAnalysis, DocumentCopy, EditPen, Notebook, Medal, Connection, Timer, UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user.js'
import { getTeacherStudents } from '@/api/teacher.js'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const stats = reactive({
  superviseCount: 0,
  reviewCount: 0
})

const identityTags = computed(() => {
  return userStore.identities.map((identity) => {
    const typeMap = {
      '组长': 'danger',
      '秘书': 'warning',
      '指导教师': 'success',
      '评阅教师': '',
      '普通成员': 'info'
    }
    return { name: identity, type: typeMap[identity] || 'info' }
  })
})

const quickLinks = computed(() => {
  const links = []
  if (userStore.identities.includes('指导教师')) {
    links.push({ name: '评价手册查看', icon: DocumentCopy, path: '/teacher/supervise/manual-review' })
    links.push({ name: '任务书/指导书填写', icon: Notebook, path: '/teacher/supervise/documents' })
    links.push({ name: '外文翻译评定', icon: EditPen, path: '/teacher/supervise/translation' })
    links.push({ name: '指导教师评定', icon: Medal, path: '/teacher/supervise/comment' })
  }
  if (userStore.identities.includes('评阅教师')) {
    links.push({ name: '评阅教师评定', icon: Medal, path: '/teacher/review/comment' })
  }
  if (userStore.identities.includes('组长')) {
    links.push({ name: '开题报告评定', icon: Connection, path: '/teacher/group/opening' })
    links.push({ name: '中期检查评定', icon: Timer, path: '/teacher/group/midterm' })
    links.push({ name: '毕业答辩评定', icon: Medal, path: '/teacher/group/defense' })
  }
  links.unshift({ name: '个人信息', icon: User, path: '/teacher/profile' })
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
    <el-page-header title="控制台" :icon="null" />

    <div class="welcome-section">
      <div class="welcome-content">
        <div class="welcome-avatar">
          <el-icon :size="36">
            <UserFilled />
          </el-icon>
        </div>
        <div class="welcome-text">
          <h2>{{ userStore.name }}老师，欢迎回来</h2>
          <p>工号：{{ userStore.username }}</p>
          <div class="identity-tags">
            <el-tag v-for="tag in identityTags" :key="tag.name" :type="tag.type" size="small" effect="plain"
              style="margin-right: 8px;">
              {{ tag.name }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="8" :md="6">
        <el-card shadow="hover" class="stat-card" @click="goTo('/teacher/profile')">
          <div class="stat-content">
            <div class="stat-icon supervise-icon">
              <el-icon size="24">
                <User />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.superviseCount }}</div>
              <div class="stat-label">指导学生</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <el-card shadow="hover" class="stat-card" @click="goTo('/teacher/profile')">
          <div class="stat-content">
            <div class="stat-icon review-icon">
              <el-icon size="24">
                <Reading />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.reviewCount }}</div>
              <div class="stat-label">评阅学生</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-row">
      <el-col :xs="24" :md="16">
        <el-card class="section-card">
          <template #header>
            <div class="card-header">
              <el-icon style="margin-right: 8px; color: #1e40af;">
                <DataAnalysis />
              </el-icon>
              <span>快捷入口</span>
            </div>
          </template>
          <div class="quick-links" v-if="quickLinks.length > 0">
            <div v-for="link in quickLinks" :key="link.path" class="quick-link-item" @click="goTo(link.path)">
              <div class="link-icon">
                <el-icon size="20">
                  <component :is="link.icon" />
                </el-icon>
              </div>
              <div class="link-name">{{ link.name }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无可用功能" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card class="section-card">
          <template #header>
            <div class="card-header">
              <el-icon style="margin-right: 8px; color: #1e40af;">
                <DocumentCopy />
              </el-icon>
              <span>功能说明</span>
            </div>
          </template>
          <div class="intro-content">
            <p class="intro-text">根据您的身份，可使用相应功能：</p>
            <ul class="intro-list">
              <li v-if="userStore.identities.includes('指导教师')">
                <strong>指导教师：</strong>填写任务书/指导书，评定外文翻译成绩，填写指导评语，查看指导学生评价手册
              </li>
              <li v-if="userStore.identities.includes('评阅教师')"><strong>评阅教师：</strong>填写评阅评语和成绩</li>
              <li v-if="userStore.identities.includes('组长')"><strong>组长：</strong>组织开题、中期、答辩环节，评定小组成绩</li>
              <li v-if="userStore.identities.includes('秘书')"><strong>秘书：</strong>录入答辩记录</li>
            </ul>
            <p class="intro-tip">请按照环节顺序完成各项评定工作，确保成绩准确无误。</p>
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
  margin: 16px 0;
  padding: 24px;
  background: #ffffff;
  border-radius: 8px;
  border-left: 4px solid #1e40af;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06);
}

.welcome-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.welcome-avatar {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  background: linear-gradient(135deg, #1e3a8a 0%, #1e40af 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.welcome-text h2 {
  margin: 0 0 8px 0;
  font-size: 22px;
  font-weight: 600;
  color: #1e293b;
}

.welcome-text p {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #64748b;
}

.identity-tags {
  margin-top: 8px;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  cursor: pointer;
  margin-bottom: 16px;
  transition: all 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.supervise-icon {
  background-color: #dbeafe;
  color: #1e40af;
}

.review-icon {
  background-color: #f3e8ff;
  color: #7c3aed;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
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
  color: #1e293b;
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 12px;
}

.quick-link-item {
  text-align: center;
  cursor: pointer;
  padding: 20px 12px;
  border-radius: 8px;
  transition: all 0.2s ease;
  border: 1px solid #e5e7eb;
}

.quick-link-item:hover {
  background: #f8fafc;
  border-color: #1e40af;
  transform: translateY(-1px);
}

.link-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 10px;
  background-color: #eff6ff;
  color: #1e40af;
}

.link-name {
  font-size: 13px;
  color: #374151;
  font-weight: 500;
}

.intro-content {
  font-size: 14px;
  line-height: 1.7;
}

.intro-text {
  margin: 0 0 12px 0;
  color: #475569;
}

.intro-list {
  margin: 0 0 16px 0;
  padding-left: 20px;
  color: #475569;
}

.intro-list li {
  margin-bottom: 8px;
}

.intro-list li strong {
  color: #1e293b;
}

.intro-tip {
  margin: 0;
  padding: 12px;
  background-color: #f8fafc;
  border-radius: 6px;
  color: #64748b;
  font-size: 13px;
  border-left: 3px solid #1e40af;
}

@media (max-width: 768px) {
  .welcome-content {
    flex-direction: column;
    text-align: center;
  }

  .identity-tags {
    display: flex;
    justify-content: center;
    flex-wrap: wrap;
  }

  .quick-links {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>

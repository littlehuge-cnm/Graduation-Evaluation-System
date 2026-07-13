<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, UserFilled, OfficeBuilding, Collection, Reading, EditPen, Check, WarningFilled, DataAnalysis, DocumentCopy } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user.js'
import { getStudentList } from '@/api/student.js'
import { getTeacherList } from '@/api/teacher.js'
import { getStudentGroupList } from '@/api/studentGroup.js'
import { getTeacherGroupList } from '@/api/teacherGroup.js'
import { getScoreRecordList } from '@/api/scoreRecord.js'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const stats = reactive({
  studentTotal: 0,
  teacherTotal: 0,
  studentGroupTotal: 0,
  teacherGroupTotal: 0,
  pendingReview: 0,
  completedReview: 0
})

const quickLinks = [
  { name: '学生管理', icon: User, path: '/admin/students' },
  { name: '教师管理', icon: UserFilled, path: '/admin/teachers' },
  { name: '学生分组', icon: OfficeBuilding, path: '/admin/student-groups' },
  { name: '教师分组', icon: Collection, path: '/admin/teacher-groups' },
  { name: '教师分配', icon: EditPen, path: '/admin/teacher-student' },
  { name: '环节分配', icon: Reading, path: '/admin/group-mappings' },
  { name: '评价手册查看', icon: DocumentCopy, path: '/admin/manual-review' },
  { name: '答辩委员会评定', icon: Check, path: '/admin/committee-evaluation' }
]

async function loadStats() {
  loading.value = true
  try {
    const [studentsRes, teachersRes, studentGroupsRes, teacherGroupsRes] = await Promise.all([
      getStudentList({ pageNum: 1, pageSize: 1 }),
      getTeacherList({ pageNum: 1, pageSize: 1 }),
      getStudentGroupList(),
      getTeacherGroupList()
    ])

    stats.studentTotal = studentsRes.total || 0
    stats.teacherTotal = teachersRes.total || 0
    stats.studentGroupTotal = (studentGroupsRes || []).length
    stats.teacherGroupTotal = (teacherGroupsRes || []).length

    if (studentsRes.total > 0) {
      const allStudents = await getStudentList({ pageNum: 1, pageSize: 1000 })
      const studentList = allStudents.list || []

      let pendingCount = 0
      let completedCount = 0

      const CONCURRENT_LIMIT = 20
      const studentNos = studentList.map(s => s.studentNo)

      for (let i = 0; i < studentNos.length; i += CONCURRENT_LIMIT) {
        const batch = studentNos.slice(i, i + CONCURRENT_LIMIT)
        const batchResults = await Promise.allSettled(
          batch.map(no => getScoreRecordList(no))
        )

        batchResults.forEach(result => {
          if (result.status === 'fulfilled') {
            const recordList = result.value || []
            const hasAllScores = ['开题报告成绩', '外文翻译', '中期检查成绩', '指导评语', '评阅评语', '毕业答辩成绩'].every(key => {
              const r = recordList.find(r => r.itemType === key)
              return r && r.score !== null && r.score !== undefined
            })
            const hasCommittee = recordList.some(r => r.itemType === '委员会评定' && r.score !== null)

            if (hasCommittee) {
              completedCount++
            } else if (hasAllScores) {
              pendingCount++
            }
          }
        })
      }

      stats.pendingReview = pendingCount
      stats.completedReview = completedCount
    }
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
          <h2>{{ userStore.name }}，欢迎回来</h2>
          <p>工号：{{ userStore.username }} · 超级管理员</p>
        </div>
      </div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="stat-card" @click="goTo('/admin/students')">
          <div class="stat-icon">
            <el-icon size="26">
              <User />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.studentTotal }}</div>
            <div class="stat-label">学生总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="stat-card" @click="goTo('/admin/teachers')">
          <div class="stat-icon">
            <el-icon size="26">
              <UserFilled />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.teacherTotal }}</div>
            <div class="stat-label">教师总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="stat-card" @click="goTo('/admin/student-groups')">
          <div class="stat-icon">
            <el-icon size="26">
              <OfficeBuilding />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.studentGroupTotal }}</div>
            <div class="stat-label">学生分组</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="stat-card" @click="goTo('/admin/teacher-groups')">
          <div class="stat-icon">
            <el-icon size="26">
              <Collection />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.teacherGroupTotal }}</div>
            <div class="stat-label">教师分组</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="stat-card" @click="goTo('/admin/committee-evaluation')">
          <div class="stat-icon pending-icon">
            <el-icon size="26">
              <WarningFilled />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value pending">{{ stats.pendingReview }}</div>
            <div class="stat-label">待评定</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="stat-card" @click="goTo('/admin/committee-evaluation')">
          <div class="stat-icon completed-icon">
            <el-icon size="26">
              <Check />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value completed">{{ stats.completedReview }}</div>
            <div class="stat-label">已评定</div>
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
          <div class="quick-links">
            <div v-for="link in quickLinks" :key="link.path" class="quick-link-item" @click="goTo(link.path)">
              <div class="link-icon">
                <el-icon size="20">
                  <component :is="link.icon" />
                </el-icon>
              </div>
              <div class="link-name">{{ link.name }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-card class="section-card">
          <template #header>
            <div class="card-header">
              <span><el-icon style="margin-right: 8px;">
                  <DocumentCopy />
                </el-icon> 系统说明</span>
            </div>
          </template>
          <div class="intro-content">
            <p>本系统用于毕业设计（论文）成绩评定管理，主要功能包括：</p>
            <ul>
              <li><strong>人员管理：</strong>学生、教师、管理员账号管理</li>
              <li><strong>分组管理：</strong>学生分组、教师分组创建与维护，师生关系分配，环节对应关系配置</li>
              <li><strong>任务管理：</strong>评价手册查看、答辩委员会总评成绩评定、评价手册导出</li>
            </ul>
            <p>请按照开题→中期→答辩的流程顺序进行管理，确保各环节成绩录入完成后再进入下一阶段。</p>
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
  background-color: #ffffff;
  border-radius: 8px;
  border-left: 4px solid #1e40af;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
}

.welcome-content {
  display: flex;
  align-items: center;
}

.welcome-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-right: 16px;
  flex-shrink: 0;
}

.welcome-text h2 {
  margin: 0 0 6px 0;
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
}

.welcome-text p {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  cursor: pointer;
  margin-bottom: 16px;
  transition: all 0.2s ease;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  border-color: #1e40af;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  padding: 20px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  flex-shrink: 0;
  background-color: #dbeafe;
  color: #1e40af;
}

.stat-icon.pending-icon {
  background-color: #fef3c7;
  color: #d97706;
}

.stat-icon.completed-icon {
  background-color: #dcfce7;
  color: #166534;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}

.stat-value.pending {
  color: #d97706;
}

.stat-value.completed {
  color: #166534;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
  font-weight: 500;
}

.content-row {
  margin-bottom: 20px;
}

.section-card {
  margin-bottom: 16px;
  height: 100%;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.section-card :deep(.el-card__header) {
  background-color: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  padding: 16px 20px;
}

.card-header {
  font-weight: 600;
  font-size: 15px;
  display: flex;
  align-items: center;
  color: #1e293b;
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.quick-link-item {
  text-align: center;
  cursor: pointer;
  padding: 16px 8px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  transition: all 0.2s ease;
}

.quick-link-item:hover {
  background-color: #f1f5f9;
  border-color: #1e40af;
  transform: translateY(-1px);
}

.link-icon {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 8px;
  background-color: #eff6ff;
  color: #1e40af;
}

.link-name {
  font-size: 13px;
  color: #475569;
  font-weight: 500;
}

.intro-content {
  padding: 4px 0;
}

.intro-content p {
  margin: 0 0 12px 0;
  line-height: 1.8;
  color: #475569;
  font-size: 14px;
}

.intro-content ul {
  margin: 0 0 12px 0;
  padding-left: 20px;
  line-height: 2;
  color: #475569;
  font-size: 14px;
}

.intro-content li strong {
  color: #1e293b;
  font-weight: 600;
}

@media (max-width: 768px) {
  .quick-links {
    grid-template-columns: repeat(2, 1fr);
  }

  .stat-card :deep(.el-card__body) {
    padding: 16px;
  }

  .stat-icon {
    width: 44px;
    height: 44px;
    margin-right: 12px;
  }

  .stat-value {
    font-size: 22px;
  }
}
</style>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { User, UserFilled, OfficeBuilding, Collection, Reading, EditPen, Check, WarningFilled, DataAnalysis, DocumentCopy } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user.js'
import { getStudentList } from '@/api/student.js'
import { getTeacherList } from '@/api/teacher.js'
import { getStudentGroupList } from '@/api/studentGroup.js'
import { getTeacherGroupList } from '@/api/teacherGroup.js'
import { getScoreRecordList } from '@/api/scoreRecord.js'
import { getGroupMappingList } from '@/api/groupMapping.js'

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

const stageProgress = reactive({
  开题: { notStarted: 0, inProgress: 0, completed: 0 },
  中期: { notStarted: 0, inProgress: 0, completed: 0 },
  答辩: { notStarted: 0, inProgress: 0, completed: 0 }
})

const quickLinks = [
  { name: '学生管理', icon: User, path: '/admin/students', color: '#409eff' },
  { name: '教师管理', icon: UserFilled, path: '/admin/teachers', color: '#67c23a' },
  { name: '学生分组', icon: OfficeBuilding, path: '/admin/student-groups', color: '#e6a23c' },
  { name: '教师分组', icon: Collection, path: '/admin/teacher-groups', color: '#f56c6c' },
  { name: '教师分配', icon: EditPen, path: '/admin/teacher-student', color: '#909399' },
  { name: '环节分配', icon: Reading, path: '/admin/group-mappings', color: '#b37feb' },
  { name: '评价手册查看', icon: DocumentCopy, path: '/admin/manual-review', color: '#36cfc9' },
  { name: '答辩委员会评定', icon: Check, path: '/admin/committee-evaluation', color: '#ff7a45' }
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

      for (const student of studentList) {
        try {
          const recordList = await getScoreRecordList(student.studentNo)
          const hasAllScores = ['开题成绩', '外文翻译', '中期检查', '指导评语', '评阅评语', '答辩成绩'].every(key => {
            const r = recordList.find(r => r.itemType === key)
            return r && r.score !== null && r.score !== undefined
          })
          const hasCommittee = recordList.some(r => r.itemType === '委员会评定' && r.score !== null)

          if (hasCommittee) {
            completedCount++
          } else if (hasAllScores) {
            pendingCount++
          }
        } catch (e) {
          // ignore
        }
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
    <el-page-header title="控制台" />

    <div class="welcome-section">
      <div class="welcome-text">
        <h2>欢迎回来，{{ userStore.name }}</h2>
        <p>您是超级管理员</p>
      </div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card" @click="goTo('/admin/students')">
          <div class="stat-icon" style="background: #ecf5ff; color: #409eff;">
            <el-icon size="28"><User /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.studentTotal }}</div>
            <div class="stat-label">学生总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card" @click="goTo('/admin/teachers')">
          <div class="stat-icon" style="background: #f0f9ff; color: #67c23a;">
            <el-icon size="28"><UserFilled /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.teacherTotal }}</div>
            <div class="stat-label">教师总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card" @click="goTo('/admin/student-groups')">
          <div class="stat-icon" style="background: #fdf6ec; color: #e6a23c;">
            <el-icon size="28"><OfficeBuilding /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.studentGroupTotal }}</div>
            <div class="stat-label">学生分组</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card" @click="goTo('/admin/teacher-groups')">
          <div class="stat-icon" style="background: #fef0f0; color: #f56c6c;">
            <el-icon size="28"><Collection /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.teacherGroupTotal }}</div>
            <div class="stat-label">教师分组</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card" @click="goTo('/admin/committee-evaluation')">
          <div class="stat-icon" style="background: #fff7e6; color: #ff7a45;">
            <el-icon size="28"><WarningFilled /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value pending">{{ stats.pendingReview }}</div>
            <div class="stat-label">待评定</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card" @click="goTo('/admin/committee-evaluation')">
          <div class="stat-icon" style="background: #e6fffb; color: #36cfc9;">
            <el-icon size="28"><Check /></el-icon>
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
              <span><el-icon style="margin-right: 8px;"><DataAnalysis /></el-icon> 快捷入口</span>
            </div>
          </template>
          <div class="quick-links">
            <div v-for="link in quickLinks" :key="link.path" class="quick-link-item" @click="goTo(link.path)">
              <div class="link-icon" :style="{ background: link.color + '15', color: link.color }">
                <el-icon size="22"><component :is="link.icon" /></el-icon>
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
              <span><el-icon style="margin-right: 8px;"><DocumentCopy /></el-icon> 系统说明</span>
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: white;
}

.welcome-text h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
}

.welcome-text p {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
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
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  width: 100%;
  padding-left: 40px;
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

.stat-value.pending {
  color: #ff7a45;
}

.stat-value.completed {
  color: #36cfc9;
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
  grid-template-columns: repeat(4, 1fr);
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

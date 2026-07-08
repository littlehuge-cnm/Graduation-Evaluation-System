<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getStudentList } from '@/api/student.js'
import { getScoreRecordList } from '@/api/scoreRecord.js'

const loading = ref(false)
const students = ref([])
const keyword = ref('')
const selectedStudent = ref(null)
const records = ref([])

const filteredStudents = computed(() => {
  if (!keyword.value) return students.value
  const k = keyword.value.trim()
  return students.value.filter(s =>
    s.studentNo?.includes(k) || s.studentName?.includes(k)
  )
})

async function fetchStudents() {
  loading.value = true
  try {
    const res = await getStudentList({ pageNum: 1, pageSize: 1000 })
    students.value = res.list || []
  } catch (error) {
    ElMessage.error(error.message || '获取学生列表失败')
  } finally {
    loading.value = false
  }
}

async function handleSelectStudent(student) {
  selectedStudent.value = student
  try {
    records.value = await getScoreRecordList(student.studentNo)
  } catch (error) {
    ElMessage.error(error.message || '获取评价记录失败')
    records.value = []
  }
}

function scrollTo(id) {
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

onMounted(fetchStudents)
</script>

<template>
  <div>
    <el-page-header title="评价手册查看" />
    <el-card class="table-card">
      <el-row :gutter="16">
        <el-col :span="6">
          <div class="student-list-header">
            <el-input v-model="keyword" placeholder="搜索学号/姓名" clearable />
          </div>
          <div v-loading="loading" class="student-list">
            <div v-for="student in filteredStudents" :key="student.studentNo" class="student-item"
              :class="{ active: selectedStudent?.studentNo === student.studentNo }"
              @click="handleSelectStudent(student)">
              <div class="student-name">{{ student.studentName }}</div>
              <div class="student-no">{{ student.studentNo }}</div>
            </div>
            <el-empty v-if="!filteredStudents.length" description="暂无学生" />
          </div>
        </el-col>
        <el-col :span="18">
          <div v-if="selectedStudent" v-loading="loading" class="detail-panel">
            <div class="detail-header">
              <h3>{{ selectedStudent.studentName }}（{{ selectedStudent.studentNo }}）</h3>
              <p>学生组：{{ selectedStudent.studentGroupName || '未分组' }}</p>
            </div>
            <el-anchor :offset="80" class="right-nav">
              <el-anchor-link href="#progress" title="评价任务进度" />
              <el-anchor-link href="#manual" title="评价手册项目内容" />
            </el-anchor>
            <div id="progress" class="section">
              <h4>评价任务进度</h4>
              <el-timeline>
                <el-timeline-item v-for="record in records" :key="record.id"
                  :type="record.recordStatus === 2 ? 'success' : 'primary'">
                  <div class="record-title">{{ record.itemType }}</div>
                  <div class="record-info">
                    成绩：{{ record.score ?? '未录入' }} &nbsp;|&nbsp;
                    等级：{{ record.grade || '未评定' }} &nbsp;|&nbsp;
                    状态：{{ record.recordStatus === 2 ? '已确认' : '暂存' }}
                  </div>
                  <div v-if="record.comment" class="record-comment">评语：{{ record.comment }}</div>
                </el-timeline-item>
                <el-empty v-if="!records.length" description="暂无评价记录" />
              </el-timeline>
            </div>
            <div id="manual" class="section">
              <h4>评价手册项目内容</h4>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="学号">{{ selectedStudent.studentNo }}</el-descriptions-item>
                <el-descriptions-item label="姓名">{{ selectedStudent.studentName }}</el-descriptions-item>
                <el-descriptions-item label="班级">{{ selectedStudent.className || '-' }}</el-descriptions-item>
                <el-descriptions-item label="专业">{{ selectedStudent.major || '-' }}</el-descriptions-item>
                <el-descriptions-item label="年级">{{ selectedStudent.grade || '-' }}</el-descriptions-item>
              </el-descriptions>
              <div class="manual-placeholder">
                此处展示评价手册各项目详细内容，后续可根据实际手册结构扩展。
              </div>
            </div>
          </div>
          <el-empty v-else description="请选择左侧学生查看详情" />
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<style scoped>
.table-card {
  margin-top: 16px;
}

.student-list-header {
  margin-bottom: 12px;
}

.student-list {
  max-height: calc(100vh - 220px);
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.student-item {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.2s;
}

.student-item:hover {
  background-color: #f5f7fa;
}

.student-item.active {
  background-color: #ecf5ff;
  border-left: 3px solid #409eff;
}

.student-name {
  font-weight: 500;
  color: #303133;
}

.student-no {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.detail-panel {
  position: relative;
  max-height: calc(100vh - 220px);
  overflow-y: auto;
  padding-right: 120px;
}

.detail-header {
  margin-bottom: 16px;
}

.detail-header h3 {
  margin: 0 0 8px;
}

.detail-header p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.right-nav {
  position: absolute;
  right: 0;
  top: 0;
  width: 110px;
}

.section {
  margin-bottom: 32px;
  padding: 16px;
  background-color: #fff;
  border-radius: 4px;
}

.section h4 {
  margin: 0 0 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e4e7ed;
}

.record-title {
  font-weight: 500;
}

.record-info {
  font-size: 13px;
  color: #606266;
  margin-top: 4px;
}

.record-comment {
  font-size: 13px;
  color: #606266;
  margin-top: 4px;
}

.manual-placeholder {
  margin-top: 16px;
  padding: 24px;
  background-color: #f5f7fa;
  color: #909399;
  text-align: center;
  border-radius: 4px;
}
</style>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user.js'
import { getStudentList } from '@/api/student.js'
import { getScoreRecordList, addScoreRecord, updateScoreRecord } from '@/api/scoreRecord.js'

const userStore = useUserStore()
const loading = ref(false)
const students = ref([])
const keyword = ref('')
const selectedStudent = ref(null)
const records = ref([])

const form = reactive({
  recordId: null,
  score: null,
  grade: '',
  comment: ''
})

const gradeOptions = ['优', '良', '中', '及格', '不及格']

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
  form.recordId = null
  form.score = null
  form.grade = ''
  form.comment = ''
  try {
    records.value = await getScoreRecordList(student.studentNo, '委员会评定')
    const record = records.value.find(r => r.itemType === '委员会评定')
    if (record) {
      form.recordId = record.id
      form.score = record.score
      form.grade = record.grade
      form.comment = record.comment
    }
  } catch (error) {
    ElMessage.error(error.message || '获取委员会评定记录失败')
    records.value = []
  }
}

async function handleSubmit() {
  try {
    const data = {
      studentNo: selectedStudent.value.studentNo,
      itemType: '委员会评定',
      score: form.score,
      grade: form.grade,
      comment: form.comment,
      recordStatus: 1
    }

    if (form.recordId) {
      await updateScoreRecord(form.recordId, data)
      ElMessage.success('修改成功')
    } else {
      await addScoreRecord(data, userStore.username)
      ElMessage.success('录入成功')
    }
    handleSelectStudent(selectedStudent.value)
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

onMounted(fetchStudents)
</script>

<template>
  <div>
    <el-page-header title="答辩委员会评定" />
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
          <div v-if="selectedStudent" class="detail-panel">
            <div class="detail-header">
              <h3>{{ selectedStudent.studentName }}（{{ selectedStudent.studentNo }}）</h3>
            </div>
            <el-form :model="form" label-width="100px" class="evaluate-form">
              <el-form-item label="加权总分">
                <el-input-number v-model="form.score" :min="0" :max="100" :precision="2" />
              </el-form-item>
              <el-form-item label="评定等级">
                <el-select v-model="form.grade" placeholder="请选择等级" style="width: 200px;">
                  <el-option v-for="g in gradeOptions" :key="g" :label="g" :value="g" />
                </el-select>
              </el-form-item>
              <el-form-item label="答辩委员会评语">
                <el-input v-model="form.comment" type="textarea" :rows="6" placeholder="请输入评语" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleSubmit">保存</el-button>
              </el-form-item>
            </el-form>
          </div>
          <el-empty v-else description="请选择左侧学生进行评定" />
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

.student-item:hover:not(.disabled) {
  background-color: #f5f7fa;
}

.student-item.active {
  background-color: #ecf5ff;
  border-left: 3px solid #409eff;
}

.student-item.disabled {
  cursor: not-allowed;
  opacity: 0.6;
  background-color: #f5f7fa;
}

.student-name {
  font-weight: 500;
  color: #303133;
}

.student-no {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  margin-bottom: 4px;
}

.detail-panel {
  max-height: calc(100vh - 220px);
  overflow-y: auto;
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

.evaluate-form {
  margin-top: 16px;
  max-width: 600px;
}
</style>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import { getScoreRecordTodo, addScoreRecord, updateScoreRecord, confirmScoreRecord, unlockScoreRecord, getScoreRecordList } from '@/api/scoreRecord.js'
import { getStudentList } from '@/api/student.js'

const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const students = ref([])
const studentStatusMap = ref({})
const allStudents = ref([])
const keyword = ref('')
const selectedStudent = ref(null)
const records = ref([])

const gradeOptions = ['优', '良', '中', '及格', '不及格']

const form = reactive({
  recordId: null,
  score: null,
  grade: '',
  comment: ''
})

function getItemStatus(record) {
  if (record && record.score !== null && record.score !== undefined) {
    return { status: '已录入', type: 'success', order: 2 }
  }
  return { status: '未录入', type: 'info', order: 1 }
}

const filteredStudents = computed(() => {
  let list = students.value
  if (keyword.value) {
    const k = keyword.value.trim().toLowerCase()
    list = list.filter(s =>
      s.studentNo?.toLowerCase().includes(k) || s.studentName?.toLowerCase().includes(k)
    )
  }
  return [...list].sort((a, b) => {
    const statusA = studentStatusMap.value[a.studentNo]
    const statusB = studentStatusMap.value[b.studentNo]
    const orderA = statusA?.order || 1
    const orderB = statusB?.order || 1
    if (orderA !== orderB) return orderA - orderB
    return a.studentNo?.localeCompare(b.studentNo)
  })
})

async function fetchData() {
  loading.value = true
  try {
    const todoRes = await getScoreRecordTodo(userStore.username, 'admin')
    const todoList = todoRes || []

    const studentMap = new Map()
    todoList.forEach(item => {
      if (!studentMap.has(item.studentNo)) {
        studentMap.set(item.studentNo, {
          studentNo: item.studentNo,
          studentName: item.studentName,
          className: item.className,
          major: item.major
        })
      }
    })
    students.value = Array.from(studentMap.values())

    const statusMap = {}
    await Promise.all(students.value.map(async (student) => {
      try {
        const recordList = await getScoreRecordList(student.studentNo)
        const record = (recordList || []).find(r => r.itemType === '委员会评定')
        statusMap[student.studentNo] = getItemStatus(record)
      } catch (e) {
        statusMap[student.studentNo] = { status: '未录入', type: 'info', order: 1 }
      }
    }))
    studentStatusMap.value = statusMap

    try {
      const studentRes = await getStudentList({ pageNum: 1, pageSize: 1000 })
      allStudents.value = studentRes.list || []
    } catch (e) {
      console.error(e)
    }

    if (route.query.studentNo) {
      const targetStudent = students.value.find(s => s.studentNo === route.query.studentNo)
      if (targetStudent) {
        await nextTick()
        handleSelectStudent(targetStudent)
      }
    }
  } catch (error) {
    ElMessage.error(error.message || '加载数据失败')
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
    const recordRes = await getScoreRecordList(student.studentNo)
    records.value = recordRes || []
    const record = records.value.find(r => r.itemType === '委员会评定')
    if (record) {
      form.recordId = record.id
      form.score = record.score
      form.grade = record.grade || ''
      form.comment = record.comment || ''
    }
  } catch (error) {
    ElMessage.error(error.message || '加载评定信息失败')
  }
}

async function handleSave() {
  if (!selectedStudent.value) {
    ElMessage.warning('请先选择学生')
    return
  }
  if (form.score === null || form.score === undefined) {
    ElMessage.warning('请输入加权总分')
    return
  }
  if (!form.grade) {
    ElMessage.warning('请选择评定等级')
    return
  }
  if (!form.comment) {
    ElMessage.warning('请输入评语')
    return
  }

  try {
    const data = {
      studentNo: selectedStudent.value.studentNo,
      itemType: '委员会评定',
      score: form.score,
      grade: form.grade,
      comment: form.comment
    }

    if (form.recordId) {
      await updateScoreRecord(form.recordId, data)
      ElMessage.success('修改成功')
    } else {
      await addScoreRecord(data, userStore.username)
      ElMessage.success('录入成功')
    }
    studentStatusMap.value[selectedStudent.value.studentNo] = { status: '已录入', type: 'success', order: 2 }
    handleSelectStudent(selectedStudent.value)
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div v-loading="loading">
    <el-page-header title="答辩委员会评定" :icon="null" />
    <el-card class="table-card">
      <el-row :gutter="16">
        <el-col :span="5" class="left-col">
          <div class="student-list-header">
            <el-input v-model="keyword" placeholder="搜索学号/姓名" clearable />
          </div>
          <div class="student-list">
            <div v-for="student in filteredStudents" :key="student.studentNo" class="student-item"
              :class="{ active: selectedStudent?.studentNo === student.studentNo }"
              @click="handleSelectStudent(student)">
              <div class="student-item-header">
                <div class="student-name">{{ student.studentName }}</div>
                <el-tag :type="studentStatusMap[student.studentNo]?.type || 'info'" size="small" effect="light">
                  {{ studentStatusMap[student.studentNo]?.status || '未录入' }}
                </el-tag>
              </div>
              <div class="student-no">{{ student.studentNo }}</div>
            </div>
            <el-empty v-if="!filteredStudents.length" description="暂无待评定学生" />
          </div>
        </el-col>
        <el-col :span="19">
          <div v-if="selectedStudent" class="detail-panel">
            <div class="detail-header">
              <h3>{{ selectedStudent.studentName }}（{{ selectedStudent.studentNo }}）</h3>
              <div class="header-info">
                <span>专业：{{ selectedStudent.major || '-' }}</span>
                <span>班级：{{ selectedStudent.className || '-' }}</span>
              </div>
            </div>

            <div class="form-section">
              <h4>答辩委员会评定</h4>
              <el-form label-width="130px">
                <el-form-item label="加权总分" required>
                  <el-input-number v-model="form.score" :min="0" :max="100" :precision="1" :controls="false"
                    style="width: 200px;" />
                  <span style="margin-left: 12px; color: #909399; font-size: 14px;">满分100分</span>
                </el-form-item>
                <el-form-item label="评定等级" required>
                  <el-select v-model="form.grade" placeholder="请选择等级" style="width: 200px;">
                    <el-option v-for="g in gradeOptions" :key="g" :label="g" :value="g" />
                  </el-select>
                </el-form-item>
                <el-form-item label="委员会评语" required>
                  <el-input v-model="form.comment" type="textarea" :rows="8" placeholder="请输入答辩委员会综合评语" />
                </el-form-item>
                <el-form-item label="状态">
                  <el-tag :type="form.recordId ? 'success' : 'info'">
                    {{ form.recordId ? '已录入' : '未录入' }}
                  </el-tag>
                </el-form-item>
              </el-form>
              <div class="form-actions">
                <el-button type="primary" @click="handleSave">{{ form.recordId ? '修改保存' : '保存' }}</el-button>
              </div>
            </div>
          </div>
          <div v-else class="empty-select">
            <span>请选择左侧学生进行委员会评定</span>
          </div>
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

.left-col {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 220px);
}

.student-list {
  flex: 1;
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

.student-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  height: calc(100vh - 220px);
  overflow-y: auto;
  padding-right: 20px;
}

.detail-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
  padding-left: 12px;
}

.detail-header h3 {
  margin: 0;
  font-size: 20px;
  color: #303133;
  font-weight: 600;
}

.header-info {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  color: #606266;
  font-size: 14px;
  margin-top: 8px;
}

.form-section {
  padding: 20px;
  background-color: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.form-section h4 {
  margin: 0 0 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #409eff;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.form-actions {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}

.empty-select {
  height: calc(100vh - 220px);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 14px;
  background-color: #fafafa;
  border-radius: 4px;
}
</style>

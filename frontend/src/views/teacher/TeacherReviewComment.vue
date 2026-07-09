<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import { getTeacherStudents } from '@/api/teacher.js'
import { getScoreRecordList, addScoreRecord, updateScoreRecord } from '@/api/scoreRecord.js'

const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const students = ref([])
const studentStatusMap = ref({})
const keyword = ref('')
const selectedStudent = ref(null)
const records = ref([])

const ITEM_TYPE = '评阅评语'

const subScores = [
  { label: '选题质量', full: 15 },
  { label: '能力水平', full: 45 },
  { label: '成果质量', full: 40 }
]

const form = reactive({
  recordId: null,
  score: null,
  subScores: [null, null, null],
  comment: ''
})

const recordMap = computed(() => {
  const map = {}
  records.value.forEach(r => {
    map[r.itemType] = r
  })
  return map
})

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
    const orderA = statusA?.order || 2
    const orderB = statusB?.order || 2
    if (orderA !== orderB) return orderA - orderB
    return a.studentNo?.localeCompare(b.studentNo)
  })
})

function parseSubScores(str) {
  if (!str) return [null, null, null]
  const arr = str.split(',').map(s => {
    const num = parseInt(s.trim())
    return isNaN(num) ? null : num
  })
  while (arr.length < 3) arr.push(null)
  return arr
}

function getItemStatus(recordList) {
  const record = recordList.find(r => r.itemType === ITEM_TYPE)
  if (record && record.score !== null && record.score !== undefined) {
    return { status: '已录入', type: 'success', order: 2 }
  }
  return { status: '未录入', type: 'info', order: 1 }
}

async function fetchStudents() {
  loading.value = true
  try {
    const reviewRes = await getTeacherStudents(userStore.username, '评阅')
    students.value = reviewRes
    const statusMap = {}
    await Promise.all(reviewRes.map(async (student) => {
      try {
        const recordList = await getScoreRecordList(student.studentNo)
        statusMap[student.studentNo] = getItemStatus(recordList || [])
      } catch (e) {
        statusMap[student.studentNo] = { status: '未录入', type: 'info', order: 1 }
      }
    }))
    studentStatusMap.value = statusMap
    if (route.query.studentNo) {
      const targetStudent = students.value.find(s => s.studentNo === route.query.studentNo)
      if (targetStudent) {
        await nextTick()
        handleSelectStudent(targetStudent)
      }
    }
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
  form.subScores = [null, null, null]
  form.comment = ''
  try {
    const recordRes = await getScoreRecordList(student.studentNo)
    records.value = recordRes || []
    const record = recordMap.value['评阅评语']
    if (record) {
      form.recordId = record.recordId
      form.score = record.score
      form.subScores = parseSubScores(record.subScores)
      form.comment = record.comment || ''
    }
  } catch (error) {
    ElMessage.error(error.message || '加载成绩失败')
  }
}

function calculateTotal() {
  let total = 0
  let hasValue = false
  for (let i = 0; i < subScores.length; i++) {
    const val = form.subScores[i]
    if (val !== null && val !== undefined && !isNaN(val)) {
      if (val < 0 || val > subScores[i].full) {
        ElMessage.warning(`${subScores[i].label}分数应在0-${subScores[i].full}之间`)
        return
      }
      total += Number(val)
      hasValue = true
    }
  }
  if (hasValue) {
    form.score = total
  } else {
    form.score = null
  }
}

async function handleSave() {
  if (!selectedStudent.value) {
    ElMessage.warning('请先选择学生')
    return
  }
  if (form.subScores.some(s => s === null || s === undefined || isNaN(s))) {
    ElMessage.warning('请填写完整分项成绩')
    return
  }
  if (!form.comment) {
    ElMessage.warning('请填写评语')
    return
  }

  try {
    const data = {
      studentNo: selectedStudent.value.studentNo,
      itemType: '评阅评语',
      score: form.score,
      subScores: form.subScores.join(','),
      comment: form.comment,
      recordStatus: 2
    }

    if (form.recordId) {
      await updateScoreRecord(form.recordId, data)
    } else {
      await addScoreRecord(data, userStore.username)
    }
    studentStatusMap.value[selectedStudent.value.studentNo] = { status: '已录入', type: 'success', order: 2 }
    ElMessage.success('保存成功')
    handleSelectStudent(selectedStudent.value)
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  }
}

onMounted(() => {
  fetchStudents()
})
</script>

<template>
  <div v-loading="loading">
    <el-page-header title="评阅教师评定" />
    <el-card class="table-card">
      <el-row :gutter="16">
        <el-col :span="5" class="left-col">
          <div class="student-list-header">
            <el-input v-model="keyword" placeholder="搜索学号/姓名" clearable @input="() => { }" />
          </div>
          <div class="student-list">
            <div v-for="student in filteredStudents" :key="student.studentNo" class="student-item"
              :class="{ active: selectedStudent?.studentNo === student.studentNo }"
              @click="handleSelectStudent(student)">
              <div class="student-item-header">
                <div class="student-name">{{ student.studentName }}</div>
                <el-tag :type="studentStatusMap[student.studentNo]?.type || 'info'" size="small">
                  {{ studentStatusMap[student.studentNo]?.status || '未录入' }}
                </el-tag>
              </div>
              <div class="student-no">{{ student.studentNo }}</div>
            </div>
            <el-empty v-if="!filteredStudents.length" description="暂无学生" />
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
              <h4>评阅教师成绩评定</h4>
              <el-form label-width="130px">
                <div class="sub-scores">
                  <el-form-item v-for="(sub, idx) in subScores" :key="idx" :label="sub.label">
                    <div class="score-input-group">
                      <el-input-number v-model="form.subScores[idx]" :min="0" :max="sub.full" :controls="false"
                        @change="calculateTotal" style="width: 150px;" />
                      <span class="score-hint">满分 {{ sub.full }} 分</span>
                    </div>
                  </el-form-item>
                </div>
                <el-form-item label="总分">
                  <span class="total-score">{{ form.score ?? '-' }} / 100</span>
                </el-form-item>
                <el-form-item label="评阅教师评语" required>
                  <el-input v-model="form.comment" type="textarea" :rows="8"
                    placeholder="请输入评阅教师评语，对毕业设计（论文）的选题质量、能力水平、成果质量进行综合评价" />
                </el-form-item>
              </el-form>
              <div class="form-actions">
                <el-button type="primary" @click="handleSave">保存</el-button>
              </div>
            </div>
          </div>
          <div v-else class="empty-select">请选择左侧学生进行评定</div>
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
  margin-bottom: 4px;
}

.student-name {
  font-weight: 500;
  color: #303133;
}

.student-no {
  font-size: 12px;
  color: #909399;
  margin-top: 0;
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
  margin-top: 6px;
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

.score-input-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.score-hint {
  color: #909399;
  font-size: 14px;
}

.total-score {
  font-size: 22px;
  font-weight: 700;
  color: #409eff;
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

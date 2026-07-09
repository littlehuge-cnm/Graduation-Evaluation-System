<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user.js'
import { getStudentList, getStudentTeachers } from '@/api/student.js'
import { getScoreRecordList, addScoreRecord, updateScoreRecord } from '@/api/scoreRecord.js'
import { getStudentGroupList } from '@/api/studentGroup.js'

const userStore = useUserStore()
const loading = ref(false)
const students = ref([])
const studentGroups = ref([])
const studentStatusMap = ref({})
const keyword = ref('')
const selectedStudent = ref(null)
const records = ref([])
const teachers = ref(null)

const form = reactive({
  recordId: null,
  score: null,
  grade: '',
  comment: ''
})

const gradeOptions = ['优', '良', '中', '及格', '不及格']

const scoreItems = [
  { key: '开题成绩', label: '开题报告', fullScore: 12, weight: 0.12, weightText: '12%' },
  { key: '外文翻译', label: '外文翻译', fullScore: 3, weight: 0.03, weightText: '3%' },
  { key: '中期检查', label: '中期检查', fullScore: 15, weight: 0.15, weightText: '15%' },
  { key: '指导评语', label: '指导教师评分', fullScore: 15, weight: 0.15, weightText: '15%' },
  { key: '评阅评语', label: '评阅教师评分', fullScore: 15, weight: 0.15, weightText: '15%' },
  { key: '答辩成绩', label: '答辩成绩', fullScore: 40, weight: 0.40, weightText: '40%' }
]

const subScoreConfigs = {
  '开题成绩': [
    { label: '调研资料获取能力', full: 4 },
    { label: '课题方案设计合理性', full: 4 },
    { label: '开题报告规范性与质量', full: 4 }
  ],
  '外文翻译': [
    { label: '外文阅读理解能力', full: 1 },
    { label: '专业词语翻译准确性', full: 1 },
    { label: '译文规范性与质量', full: 1 }
  ],
  '中期检查': [
    { label: '完成进度情况', full: 5 },
    { label: '综合能力', full: 5 },
    { label: '已完成部分质量', full: 5 }
  ],
  '指导评语': [
    { label: '方案设计能力', full: 3 },
    { label: '基本理论应用', full: 3 },
    { label: '分析解决问题能力', full: 3 },
    { label: '科学素养与态度', full: 3 },
    { label: '工作量与规范质量', full: 3 }
  ],
  '评阅评语': [
    { label: '规范性与质量', full: 4 },
    { label: '基本理论运用', full: 4 },
    { label: '研究/设计方案', full: 4 },
    { label: '创新性', full: 3 }
  ],
  '答辩成绩': [
    { label: '陈述情况', full: 10 },
    { label: '论文水平', full: 10 },
    { label: '工作量评价', full: 10 },
    { label: '答辩情况', full: 10 }
  ]
}

const recordMap = computed(() => {
  const map = {}
  records.value.forEach(r => {
    map[r.itemType] = r
  })
  return map
})

const groupNameMap = computed(() => {
  const map = {}
  studentGroups.value.forEach(g => {
    map[g.groupId] = g.groupName
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
    const orderA = studentStatusMap.value[a.studentNo]?.order || 3
    const orderB = studentStatusMap.value[b.studentNo]?.order || 3
    if (orderA !== orderB) return orderA - orderB
    return a.studentNo?.localeCompare(b.studentNo)
  })
})

function getScore(itemType) {
  const r = recordMap.value[itemType]
  return r?.score ?? null
}

function getComment(itemType) {
  const r = recordMap.value[itemType]
  return r?.comment || '暂无评语'
}

function getRecorderName(itemType) {
  const r = recordMap.value[itemType]
  return r?.recorderName || '-'
}

function getRecordTime(itemType) {
  const r = recordMap.value[itemType]
  return r?.recordTime || '-'
}

function parseSubScores(subScores) {
  if (!subScores) return []
  return subScores.split(',').map(s => parseInt(s.trim()))
}

function getSubScoreDisplay(itemType, index) {
  const r = recordMap.value[itemType]
  if (!r?.subScores) return '-'
  const arr = parseSubScores(r.subScores)
  return arr[index] ?? '-'
}

function isItemCompleted(itemKey) {
  const r = recordMap.value[itemKey]
  return !!(r && r.score !== null && r.score !== undefined)
}

const incompleteItems = computed(() => {
  return scoreItems.filter(item => !isItemCompleted(item.key)).map(item => item.label)
})

const canSubmit = computed(() => {
  return incompleteItems.value.length === 0
})

const calculatedTotal = computed(() => {
  if (!canSubmit.value) return null
  let total = 0
  for (const item of scoreItems) {
    const score = getScore(item.key)
    total += score * item.weight
  }
  return Number(total.toFixed(2))
})

function getAutoGrade(score) {
  if (score >= 90) return '优'
  if (score >= 80) return '良'
  if (score >= 70) return '中'
  if (score >= 60) return '及格'
  return '不及格'
}

watch(calculatedTotal, (newVal) => {
  if (newVal !== null) {
    form.score = newVal
    form.grade = getAutoGrade(newVal)
  } else {
    form.score = null
    form.grade = ''
  }
}, { immediate: true })

function getTableData() {
  return scoreItems.map(item => ({
    key: item.key,
    label: item.label,
    weight: item.weightText,
    fullScore: item.fullScore,
    score: getScore(item.key),
    completed: isItemCompleted(item.key),
    metaName: (item.key === '中期检查' ? '录入人（检查组长）' :
      item.key === '指导评语' ? '指导老师' :
        item.key === '评阅评语' ? '评阅老师' :
          item.key === '答辩成绩' ? '录入人（答辩小组组长）' : '录入人'),
    recorder: getRecorderName(item.key),
    recordTime: getRecordTime(item.key),
    comment: getComment(item.key),
    subScores: subScoreConfigs[item.key]
  }))
}

function getStudentStatus(studentNo, recordList) {
  const hasAllScores = scoreItems.every(item => {
    const r = recordList.find(r => r.itemType === item.key)
    return r && r.score !== null && r.score !== undefined
  })
  const hasCommittee = recordList.some(r => r.itemType === '委员会评定' && r.score !== null)

  if (hasCommittee) return { status: '已评定', type: 'success', order: 2 }
  if (hasAllScores) return { status: '待评定', type: 'warning', order: 1 }
  return { status: '未完成', type: 'info', order: 3 }
}

async function fetchStudents() {
  loading.value = true
  try {
    const [res, groupRes] = await Promise.all([
      getStudentList({ pageNum: 1, pageSize: 1000 }),
      getStudentGroupList()
    ])
    const studentList = res.list || []
    studentGroups.value = groupRes || []
    students.value = studentList

    const statusMap = {}
    await Promise.all(studentList.map(async (student) => {
      try {
        const recordList = await getScoreRecordList(student.studentNo)
        statusMap[student.studentNo] = getStudentStatus(student.studentNo, recordList || [])
      } catch (e) {
        statusMap[student.studentNo] = { status: '未完成', type: 'info', order: 3 }
      }
    }))
    studentStatusMap.value = statusMap
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
  loading.value = true
  try {
    const [recordRes, teacherRes] = await Promise.all([
      getScoreRecordList(student.studentNo),
      getStudentTeachers(student.studentNo)
    ])
    records.value = recordRes || []
    teachers.value = teacherRes || null
    const record = recordRes.find(r => r.itemType === '委员会评定')
    if (record) {
      form.recordId = record.id
      form.comment = record.comment || ''
      if (!canSubmit.value) {
        form.score = record.score
        form.grade = record.grade
      }
    }
  } catch (error) {
    ElMessage.error(error.message || '获取评定记录失败')
    records.value = []
    teachers.value = null
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!canSubmit.value) {
    ElMessage.warning('请先完成所有环节成绩评定')
    return
  }
  if (form.score === null || form.score === undefined) {
    ElMessage.warning('总评成绩计算异常')
    return
  }
  if (!form.grade) {
    ElMessage.warning('请选择评定等级')
    return
  }
  try {
    const data = {
      studentNo: selectedStudent.value.studentNo,
      itemType: '委员会评定',
      score: form.score,
      grade: form.grade,
      comment: form.comment,
      recordStatus: 2
    }

    if (form.recordId) {
      await updateScoreRecord(form.recordId, data)
      ElMessage.success('修改成功')
    } else {
      await addScoreRecord(data, userStore.username)
      ElMessage.success('评定成功')
    }
    studentStatusMap.value[selectedStudent.value.studentNo] = { status: '已评定', type: 'success', order: 2 }
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
        <el-col :span="5">
          <div class="student-list-header">
            <el-input v-model="keyword" placeholder="搜索学号/姓名" clearable />
          </div>
          <div v-loading="loading" class="student-list">
            <div v-for="student in filteredStudents" :key="student.studentNo" class="student-item"
              :class="{ active: selectedStudent?.studentNo === student.studentNo }"
              @click="handleSelectStudent(student)">
              <div class="student-info">
                <div class="student-name">{{ student.studentName }}</div>
                <div class="student-no">{{ student.studentNo }}</div>
              </div>
              <el-tag v-if="studentStatusMap[student.studentNo]" :type="studentStatusMap[student.studentNo].type"
                size="small" effect="light">
                {{ studentStatusMap[student.studentNo].status }}
              </el-tag>
            </div>
            <el-empty v-if="!filteredStudents.length" description="暂无学生" />
          </div>
        </el-col>
        <el-col :span="19">
          <div v-if="selectedStudent" v-loading="loading" class="detail-panel">
            <div class="detail-header">
              <h3>{{ selectedStudent.studentName }}（{{ selectedStudent.studentNo }}）</h3>
              <div class="header-info">
                <span>专业：{{ selectedStudent.major || '-' }}</span>
                <span>班级：{{ selectedStudent.className || '-' }}</span>
                <span>年级：{{ selectedStudent.grade || '-' }}</span>
              </div>
              <div class="header-info">
                <span>学生组：{{ groupNameMap[selectedStudent.studentGroupId] || '未分组' }}</span>
                <span v-if="teachers">指导教师：{{ teachers.supervisorName || '-' }}</span>
                <span v-if="teachers">评阅教师：{{ teachers.reviewerName || '-' }}</span>
              </div>
            </div>

            <div class="section">
              <h4>各环节成绩汇总</h4>
              <el-table :data="getTableData()" border size="small">
                <el-table-column prop="label" label="成绩项" width="180" />
                <el-table-column prop="weight" label="权重" width="100" />
                <el-table-column prop="fullScore" label="满分" width="100" />
                <el-table-column label="得分" width="120">
                  <template #default="{ row }">
                    <el-popover v-if="row.completed" placement="left" trigger="click" width="500"
                      popper-class="score-detail-popover">
                      <template #reference>
                        <span class="score-link">{{ row.score }}</span>
                      </template>
                      <div class="popover-detail">
                        <h5>{{ row.label }}成绩详情</h5>
                        <el-table :data="[
                          { name: '得分', ...row.subScores.reduce((acc, sub, idx) => ({ ...acc, [`item${idx}`]: getSubScoreDisplay(row.key, idx) }), {}), total: row.score },
                          { name: '满分', ...row.subScores.reduce((acc, sub, idx) => ({ ...acc, [`item${idx}`]: sub.full }), {}), total: row.fullScore }
                        ]" border size="small">
                          <el-table-column prop="name" label="" width="70" />
                          <el-table-column v-for="(sub, idx) in row.subScores" :key="idx" :prop="`item${idx}`"
                            :label="sub.label" />
                          <el-table-column prop="total" label="总成绩" width="90" />
                        </el-table>
                        <div class="popover-meta">
                          <strong>{{ row.metaName }}：</strong>{{ row.recorder }} &nbsp;|&nbsp; <strong>录入时间：</strong>{{
                            row.recordTime }}
                        </div>
                        <div class="popover-comment">{{ row.comment }}</div>
                      </div>
                    </el-popover>
                    <span v-else class="score-incomplete">未完成</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <div v-if="!canSubmit" class="warning-tip">
              <el-icon>
                <WarningFilled />
              </el-icon>
              <span>以下环节尚未完成评分：{{ incompleteItems.join('、') }}，请等待各环节评分完成后再提交委员会评定</span>
            </div>

            <div class="section form-section">
              <h4>答辩委员会评语及总评成绩</h4>
              <div v-if="canSubmit" class="calc-total">
                各环节已完成评分，系统已自动计算总评成绩：<span class="calc-score">{{ calculatedTotal }}</span> 分，等级：<span
                  class="grade-tag">{{
                    form.grade }}</span>
              </div>
              <el-form :model="form" label-width="120px" class="evaluate-form">
                <el-form-item label="总评成绩">
                  <el-input-number v-model="form.score" :min="0" :max="100" :precision="2" size="large"
                    :disabled="canSubmit" />
                  <span class="form-tip">分（满分100分），所有环节完成后自动计算</span>
                </el-form-item>
                <el-form-item label="评定等级">
                  <el-select v-model="form.grade" placeholder="自动计算" size="large" style="width: 200px;"
                    :disabled="canSubmit">
                    <el-option v-for="g in gradeOptions" :key="g" :label="g" :value="g" />
                  </el-select>
                </el-form-item>
                <el-form-item label="答辩委员会评语">
                  <el-input v-model="form.comment" type="textarea" :rows="6" placeholder="请输入答辩委员会评语"
                    :disabled="!canSubmit" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" size="large" @click="handleSubmit" :disabled="!canSubmit">
                    {{ form.recordId ? '修改评定' : '提交评定' }}
                  </el-button>
                </el-form-item>
              </el-form>
              <div class="note-text">
                备注：（1）等级评定：优，良，中，及格，不及格（2）有不合格二次重做或重答辩的一律为及格（3）二次重做或重答辩还有不合格的一律为不及格
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
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.student-item:hover {
  background-color: #f5f7fa;
}

.student-item.active {
  background-color: #ecf5ff;
  border-left: 3px solid #409eff;
}

.student-info {
  flex: 1;
  min-width: 0;
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
}

.detail-header {
  margin-bottom: 16px;
  margin-left: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.detail-header h3 {
  margin: 0 0 12px;
  font-size: 22px;
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

.section {
  margin-bottom: 20px;
  padding: 20px;
  background-color: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.section h4 {
  margin: 0 0 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #409eff;
  color: #303133;
  font-size: 20px;
  font-weight: 600;
}

.score-link {
  color: #409eff;
  cursor: pointer;
  text-decoration: underline;
}

.score-link:hover {
  color: #66b1ff;
}

.score-incomplete {
  color: #f56c6c;
  font-weight: 500;
}

.warning-tip {
  margin-bottom: 20px;
  padding: 12px 16px;
  background-color: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 6px;
  color: #e6a23c;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.score-detail-popover) {
  padding: 0 !important;
}

.popover-detail h5 {
  margin: 0 0 12px;
  font-size: 16px;
  color: #303133;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.popover-meta {
  margin-top: 12px;
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.popover-comment {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.calc-total {
  margin-bottom: 20px;
  padding: 16px;
  background: linear-gradient(135deg, #ecf5ff 0%, #f0f9eb 100%);
  border-radius: 8px;
  font-size: 15px;
  color: #606266;
}

.calc-score {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin: 0 8px;
}

.grade-tag {
  display: inline-block;
  padding: 4px 12px;
  background-color: #67c23a;
  color: #fff;
  border-radius: 4px;
  font-weight: 600;
  margin-left: 8px;
}

.form-section {
  background-color: #fafcff;
}

.evaluate-form {
  margin-top: 0;
}

.form-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 14px;
}

.note-text {
  margin-top: 14px;
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
}

.empty-select {
  padding: 60px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}
</style>

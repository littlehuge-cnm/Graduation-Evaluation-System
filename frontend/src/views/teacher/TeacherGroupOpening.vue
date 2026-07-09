<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import { getStudentGroupList, getStudentGroupById } from '@/api/studentGroup.js'
import { getTeacherGroupList } from '@/api/teacherGroup.js'
import { getGroupMappingList } from '@/api/groupMapping.js'
import { getScoreRecordList, addScoreRecord, updateScoreRecord } from '@/api/scoreRecord.js'

const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const studentGroups = ref([])
const mappings = ref([])
const myTeacherGroupIds = ref([])
const selectedGroup = ref(null)
const students = ref([])
const studentStatusMap = ref({})
const selectedStudent = ref(null)
const records = ref([])

const STAGE = '开题'
const ITEM_TYPE = '开题成绩'

const subScores = [
  { label: '选题质量', full: 20 },
  { label: '文献调研', full: 20 },
  { label: '开题报告', full: 60 }
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

const myGroups = computed(() => {
  const openingMappings = mappings.value.filter(m =>
    m.stage === STAGE && myTeacherGroupIds.value.includes(m.teacherGroupId)
  )
  return openingMappings.map(m => {
    const studentGroup = studentGroups.value.find(g => g.groupId === m.studentGroupId)
    return studentGroup || {
      groupId: m.studentGroupId,
      groupName: m.studentGroupName || `学生组${m.studentGroupId}`
    }
  }).filter(Boolean)
})

const sortedStudents = computed(() => {
  return [...students.value].sort((a, b) => {
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

async function fetchGroups() {
  loading.value = true
  try {
    const teacherNo = userStore.username
    const [teacherGroupsRes, mappingsRes, studentGroupsRes] = await Promise.all([
      getTeacherGroupList(),
      getGroupMappingList(),
      getStudentGroupList()
    ])
    const teacherGroups = teacherGroupsRes || []
    const myTeacherGroups = teacherGroups.filter(g =>
      g.leaderNo === teacherNo ||
      g.secretaryNo === teacherNo ||
      (g.memberNo && g.memberNo.split(',').includes(teacherNo))
    )
    myTeacherGroupIds.value = myTeacherGroups.map(g => g.groupId)
    mappings.value = mappingsRes || []
    studentGroups.value = studentGroupsRes || []
    await nextTick()
    const targetGroupId = route.query.groupId
    const targetStudentNo = route.query.studentNo
    if (targetGroupId) {
      const targetGroup = myGroups.value.find(g => String(g.groupId) === String(targetGroupId))
      if (targetGroup) {
        await handleSelectGroup(targetGroup)
        await nextTick()
        if (targetStudentNo) {
          const targetStudent = students.value.find(s => s.studentNo === targetStudentNo)
          if (targetStudent) {
            handleSelectStudent(targetStudent)
          }
        }
      }
    }
  } catch (error) {
    ElMessage.error(error.message || '获取分组列表失败')
  } finally {
    loading.value = false
  }
}

async function handleSelectGroup(group) {
  selectedGroup.value = group
  selectedStudent.value = null
  form.recordId = null
  form.score = null
  form.subScores = [null, null, null]
  form.comment = ''
  try {
    const groupDetail = await getStudentGroupById(group.groupId)
    const groupStudents = groupDetail.students || []
    students.value = groupStudents
    const statusMap = {}
    await Promise.all(groupStudents.map(async (student) => {
      try {
        const recordList = await getScoreRecordList(student.studentNo)
        statusMap[student.studentNo] = getItemStatus(recordList || [])
      } catch (e) {
        statusMap[student.studentNo] = { status: '未录入', type: 'info', order: 1 }
      }
    }))
    studentStatusMap.value = statusMap
  } catch (error) {
    ElMessage.error(error.message || '获取组内学生失败')
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
    const record = recordMap.value['开题成绩']
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
      itemType: '开题成绩',
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
  fetchGroups()
})
</script>

<template>
  <div v-loading="loading">
    <el-page-header title="开题答辩评定" />
    <el-card class="table-card">
      <el-row :gutter="16">
        <el-col :span="5">
          <div class="list-header">答辩分组列表</div>
          <div class="group-list">
            <div v-for="group in myGroups" :key="group.groupId" class="group-item"
              :class="{ active: selectedGroup?.groupId === group.groupId }" @click="handleSelectGroup(group)">
              <div class="group-name">{{ group.groupName }}</div>
              <div class="group-info">{{ group.major || '-' }}</div>
            </div>
            <el-empty v-if="!myGroups.length" description="暂无参与的分组" />
          </div>
        </el-col>
        <el-col :span="4" v-if="selectedGroup">
          <div class="list-header">组内学生</div>
          <div class="student-list">
            <div v-for="student in sortedStudents" :key="student.studentNo" class="student-item"
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
            <el-empty v-if="!students.length" description="组内暂无学生" />
          </div>
        </el-col>
        <el-col :span="15" v-if="selectedGroup" class="right-col">
          <div v-if="selectedStudent" class="detail-panel">
            <div class="detail-header">
              <h3>{{ selectedStudent.studentName }}（{{ selectedStudent.studentNo }}）</h3>
              <div class="header-info">
                <span>专业：{{ selectedStudent.major || selectedGroup.major || '-' }}</span>
                <span>班级：{{ selectedStudent.className || '-' }}</span>
                <span>答辩分组：{{ selectedGroup.groupName }}</span>
              </div>
            </div>

            <div class="form-section">
              <h4>开题答辩成绩评定</h4>
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
                <el-form-item label="开题评语" required>
                  <el-input v-model="form.comment" type="textarea" :rows="8"
                    placeholder="请输入开题答辩评语，对选题质量、文献调研、开题报告情况进行综合评价" />
                </el-form-item>
              </el-form>
              <div class="form-actions">
                <el-button type="primary" @click="handleSave">保存</el-button>
              </div>
            </div>
          </div>
          <div v-else class="empty-select">
            <span>请选择学生进行评定</span>
          </div>
        </el-col>
        <el-col :span="19" v-else class="empty-select">
          <span>请选择左侧分组开始评定</span>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<style scoped>
.table-card {
  margin-top: 16px;
}

.list-header {
  font-weight: 600;
  margin-bottom: 12px;
  font-size: 15px;
  color: #303133;
}

.group-list {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  height: calc(100vh - 220px);
  overflow-y: auto;
}

.group-item {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.2s;
}

.group-item:hover {
  background-color: #f5f7fa;
}

.group-item.active {
  background-color: #ecf5ff;
  border-left: 3px solid #409eff;
}

.group-name {
  font-weight: 500;
  color: #303133;
}

.group-info {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.student-list {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  height: calc(100vh - 220px);
  overflow-y: auto;
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

.right-col {
  padding-left: 16px;
}

.detail-panel {
  height: calc(100vh - 220px);
  overflow-y: auto;
  padding-right: 8px;
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

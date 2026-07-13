<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import { getStudentGroupList, getStudentGroupById } from '@/api/studentGroup.js'
import { getTeacherGroupList } from '@/api/teacherGroup.js'
import { getGroupMappingList } from '@/api/groupMapping.js'
import { getScoreRecordList, addScoreRecord, updateScoreRecord } from '@/api/scoreRecord.js'
import { getTeacherById } from '@/api/teacher.js'

const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const teacherGroups = ref([])
const studentGroups = ref([])
const mappings = ref([])
const myTeacherGroupIds = ref([])
const currentTeacherGroup = ref(null)
const myRole = ref('')
const selectedGroup = ref(null)
const allGroupStudents = ref({})
const studentStatusMap = ref({})
const selectedStudent = ref(null)
const records = ref([])
const expandedGroups = ref([])

const STAGE = '答辩'
const ITEM_TYPE = '毕业答辩成绩'
const PAGE_TITLE = '毕业答辩评定'

const subScores = [
  { label: '毕业设计（论文）陈述情况', full: 10, desc: '是否简明扼要、思路清晰；语言表达准确，概念清楚，论点正确，分析归纳合理' },
  { label: '毕业设计（论文）水平', full: 10, desc: '是否有独到的见解，富有新意，或对某些问题有较深刻的分析，有较高的学术水平或较高的实用价值' },
  { label: '毕业设计（论文）工作量评价', full: 10, desc: '工作量饱满程度，独立完成情况' },
  { label: '答辩情况', full: 10, desc: '是否能够准确深入地回答所提出的问题，基本概念清楚，有理有据，语言表达能力、是否得体' }
]

const form = reactive({
  recordId: null,
  defenseRecordId: null,
  score: null,
  subScores: [null, null, null, null],
  comment: '',
  defenseRecord: '',
  defenseRecordTime: null,
  defenseRecorderName: ''
})

const searchKeyword = ref('')

const canEditScore = computed(() => myRole.value === '组长')
const canEditRecord = computed(() => myRole.value === '秘书')
const canEdit = computed(() => canEditScore.value || canEditRecord.value)

const recordMap = computed(() => {
  const map = {}
  records.value.forEach(r => {
    map[r.itemType] = r
  })
  return map
})

const myGroups = computed(() => {
  const stageMappings = mappings.value.filter(m =>
    m.stage === STAGE && myTeacherGroupIds.value.includes(m.teacherGroupId)
  )
  return stageMappings.map(m => {
    const studentGroup = studentGroups.value.find(g => g.groupId === m.studentGroupId)
    return studentGroup || {
      groupId: m.studentGroupId,
      groupName: m.studentGroupName || `学生组${m.studentGroupId}`
    }
  }).filter(Boolean)
})

const totalStudents = computed(() => {
  let count = 0
  myGroups.value.forEach(group => {
    count += (allGroupStudents.value[group.groupId] || []).length
  })
  return count
})

const filteredGroups = computed(() => {
  if (!searchKeyword.value.trim()) {
    return myGroups.value.map(g => ({ ...g, _filteredStudents: null }))
  }
  const keyword = searchKeyword.value.trim().toLowerCase()
  return myGroups.value.map(group => {
    const students = (allGroupStudents.value[group.groupId] || []).filter(s =>
      s.studentName.toLowerCase().includes(keyword) || s.studentNo.includes(keyword)
    )
    if (students.length > 0 || group.groupName.toLowerCase().includes(keyword)) {
      return {
        ...group,
        _filteredStudents: students.length > 0 ? students : (allGroupStudents.value[group.groupId] || [])
      }
    }
    return null
  }).filter(Boolean)
})

function parseSubScores(str) {
  if (!str) return [null, null, null, null]
  const arr = str.split(',').map(s => {
    const num = parseInt(s.trim())
    return isNaN(num) ? null : num
  })
  while (arr.length < 4) arr.push(null)
  return arr
}

function formatDate(dateTime) {
  if (!dateTime) {
    return new Date().toLocaleDateString('zh-CN')
  }
  try {
    const date = new Date(dateTime)
    return date.toLocaleDateString('zh-CN')
  } catch (e) {
    return new Date().toLocaleDateString('zh-CN')
  }
}

function getItemStatus(recordList) {
  const scoreRecord = recordList.find(r => r.itemType === ITEM_TYPE)
  const defenseRecord = recordList.find(r => r.itemType === '答辩记录')
  const hasScore = scoreRecord && scoreRecord.score !== null && scoreRecord.score !== undefined
  const hasRecord = defenseRecord && defenseRecord.defenseRecord
  if (hasScore && hasRecord) {
    return { status: '已录入', type: 'success', order: 2 }
  }
  if (hasScore || hasRecord) {
    return { status: '部分录入', type: 'warning', order: 1 }
  }
  return { status: '未录入', type: 'info', order: 1 }
}

const teacherNameCache = ref({})

async function fetchGroups() {
  loading.value = true
  try {
    const teacherNo = userStore.username
    teacherNameCache.value[teacherNo] = userStore.name
    const [teacherGroupsRes, mappingsRes, studentGroupsRes] = await Promise.all([
      getTeacherGroupList(),
      getGroupMappingList(),
      getStudentGroupList()
    ])
    teacherGroups.value = teacherGroupsRes || []
    const myTeacherGroups = teacherGroups.value.filter(g =>
      g.leaderNo === teacherNo ||
      g.secretaryNo === teacherNo ||
      (g.memberNo && g.memberNo.split(',').includes(teacherNo))
    )
    if (myTeacherGroups.length > 0) {
      currentTeacherGroup.value = myTeacherGroups[0]
      if (currentTeacherGroup.value.leaderNo === teacherNo) {
        myRole.value = '组长'
      } else if (currentTeacherGroup.value.secretaryNo === teacherNo) {
        myRole.value = '秘书'
      } else {
        myRole.value = '普通成员'
      }
    }
    myTeacherGroupIds.value = myTeacherGroups.map(g => g.groupId)
    mappings.value = mappingsRes || []
    studentGroups.value = studentGroupsRes || []

    if (currentTeacherGroup.value) {
      const memberNos = []
      if (currentTeacherGroup.value.leaderNo && !teacherNameCache.value[currentTeacherGroup.value.leaderNo]) {
        memberNos.push(currentTeacherGroup.value.leaderNo)
      }
      if (currentTeacherGroup.value.secretaryNo && !teacherNameCache.value[currentTeacherGroup.value.secretaryNo]) {
        memberNos.push(currentTeacherGroup.value.secretaryNo)
      }
      if (currentTeacherGroup.value.memberNo) {
        currentTeacherGroup.value.memberNo.split(',').filter(Boolean).forEach(no => {
          if (!teacherNameCache.value[no]) memberNos.push(no)
        })
      }
      if (memberNos.length > 0) {
        Promise.all(memberNos.map(no => getTeacherById(no))).then(teachers => {
          teachers.forEach(t => {
            teacherNameCache.value[t.teacherNo] = t.teacherName
          })
        }).catch(e => console.error('获取教师姓名失败', e))
      }
    }

    const allStudentsMap = {}
    const allStudents = []
    await Promise.all(myGroups.value.map(async (group) => {
      try {
        const groupDetail = await getStudentGroupById(group.groupId)
        allStudentsMap[group.groupId] = groupDetail.students || []
        allStudents.push(...(groupDetail.students || []))
      } catch (e) {
        console.error(`获取学生组${group.groupId}失败`, e)
        allStudentsMap[group.groupId] = []
      }
    }))
    allGroupStudents.value = allStudentsMap

    // 批量预加载所有学生的记录状态
    await Promise.all(allStudents.map(async (student) => {
      try {
        const recordRes = await getScoreRecordList(student.studentNo)
        const records = recordRes || []
        studentStatusMap.value[student.studentNo] = getItemStatus(records)
      } catch (e) {
        console.error(`加载学生${student.studentNo}记录状态失败`, e)
      }
    }))

    if (myGroups.value.length > 0) {
      expandedGroups.value.push(myGroups.value[0].groupId)
    }

    await nextTick()
    const targetGroupId = route.query.groupId
    const targetStudentNo = route.query.studentNo
    if (targetGroupId) {
      const targetGroup = myGroups.value.find(g => String(g.groupId) === String(targetGroupId))
      if (targetGroup) {
        selectedGroup.value = targetGroup
        if (!expandedGroups.value.includes(targetGroup.groupId)) {
          expandedGroups.value.push(targetGroup.groupId)
        }
        await nextTick()
        if (targetStudentNo) {
          const students = allGroupStudents.value[targetGroup.groupId] || []
          const targetStudent = students.find(s => s.studentNo === targetStudentNo)
          if (targetStudent) {
            handleSelectStudent(targetStudent, targetGroup)
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

function getTeacherName(teacherNo) {
  return teacherNameCache.value[teacherNo] || teacherNo
}

const groupMembers = computed(() => {
  if (!currentTeacherGroup.value) return []
  const members = []
  if (currentTeacherGroup.value.leaderNo) {
    members.push({ no: currentTeacherGroup.value.leaderNo, name: getTeacherName(currentTeacherGroup.value.leaderNo), role: '组长' })
  }
  if (currentTeacherGroup.value.secretaryNo) {
    members.push({ no: currentTeacherGroup.value.secretaryNo, name: getTeacherName(currentTeacherGroup.value.secretaryNo), role: '秘书' })
  }
  if (currentTeacherGroup.value.memberNo) {
    const memberNos = currentTeacherGroup.value.memberNo.split(',').filter(Boolean)
    memberNos.forEach(no => {
      members.push({ no, name: getTeacherName(no), role: '成员' })
    })
  }
  return members
})

function handleSelectGroup(group) {
  const idx = expandedGroups.value.indexOf(group.groupId)
  if (idx > -1) {
    expandedGroups.value.splice(idx, 1)
  } else {
    expandedGroups.value.push(group.groupId)
  }
}

function handleSelectStudent(student, group = null) {
  selectedStudent.value = student
  selectedGroup.value = group || myGroups.value.find(g => g.groupId === student.groupId)
  form.recordId = null
  form.defenseRecordId = null
  form.score = null
  form.subScores = [null, null, null, null]
  form.comment = ''
  form.defenseRecord = ''
  form.defenseRecordTime = null
  form.defenseRecorderName = ''
  loadStudentRecord(student.studentNo)
}

async function loadStudentRecord(studentNo) {
  try {
    const recordRes = await getScoreRecordList(studentNo)
    records.value = recordRes || []
    studentStatusMap.value[studentNo] = getItemStatus(records.value)
    const scoreRecord = recordMap.value[ITEM_TYPE]
    if (scoreRecord) {
      form.recordId = scoreRecord.id
      form.score = scoreRecord.score
      form.subScores = parseSubScores(scoreRecord.subScores)
      form.comment = scoreRecord.comment || ''
    }
    const defenseRecord = recordMap.value['答辩记录']
    if (defenseRecord) {
      form.defenseRecordId = defenseRecord.id
      form.defenseRecord = defenseRecord.defenseRecord || ''
      form.defenseRecordTime = defenseRecord.recordTime
      form.defenseRecorderName = defenseRecord.recorderName || ''
    } else {
      form.defenseRecordId = null
      form.defenseRecordTime = null
      form.defenseRecorderName = ''
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

async function handleSaveScore() {
  if (!canEditScore.value) {
    ElMessage.warning('您没有编辑成绩权限，仅组长可填写成绩')
    return
  }
  if (!selectedStudent.value) {
    ElMessage.warning('请先选择学生')
    return
  }

  const data = {
    studentNo: selectedStudent.value.studentNo,
    itemType: ITEM_TYPE,
    recordStatus: 2
  }

  if (form.subScores.some(s => s === null || s === undefined || isNaN(s))) {
    ElMessage.warning('请填写完整分项成绩')
    return
  }
  if (!form.comment) {
    ElMessage.warning('请填写答辩评语')
    return
  }
  data.score = form.score
  data.subScores = form.subScores.join(',')
  data.comment = form.comment

  try {
    if (form.recordId) {
      await updateScoreRecord(form.recordId, data)
    } else {
      await addScoreRecord(data, userStore.username)
    }

    const recordList = await getScoreRecordList(selectedStudent.value.studentNo)
    studentStatusMap.value[selectedStudent.value.studentNo] = getItemStatus(recordList || [])

    ElMessage.success('成绩保存成功')
    loadStudentRecord(selectedStudent.value.studentNo)
  } catch (error) {
    ElMessage.error(error.message || '成绩保存失败')
  }
}

async function handleSaveRecord() {
  if (!canEditRecord.value) {
    ElMessage.warning('您没有编辑答辩记录权限，仅秘书可填写记录')
    return
  }
  if (!selectedStudent.value) {
    ElMessage.warning('请先选择学生')
    return
  }

  const recordItemType = '答辩记录'
  const data = {
    studentNo: selectedStudent.value.studentNo,
    itemType: recordItemType,
    recordStatus: 2
  }

  if (!form.defenseRecord) {
    ElMessage.warning('请填写答辩记录')
    return
  }
  data.defenseRecord = form.defenseRecord

  try {
    if (form.defenseRecordId) {
      await updateScoreRecord(form.defenseRecordId, data)
    } else {
      await addScoreRecord(data, userStore.username)
    }

    const recordList = await getScoreRecordList(selectedStudent.value.studentNo)
    studentStatusMap.value[selectedStudent.value.studentNo] = getItemStatus(recordList || [])

    ElMessage.success('答辩记录保存成功')
    loadStudentRecord(selectedStudent.value.studentNo)
  } catch (error) {
    ElMessage.error(error.message || '答辩记录保存失败')
  }
}

onMounted(() => {
  fetchGroups()
})
</script>

<template>
  <div v-loading="loading">
    <div class="page-header-wrapper">
      <el-page-header :title="PAGE_TITLE" :icon="null" />
      <div v-if="currentTeacherGroup" class="group-info-header">
        <span class="group-label">教师组：</span>
        <span class="group-name-text">{{ currentTeacherGroup.groupName }}</span>
        <el-tag size="small" :type="myRole === '组长' ? 'danger' : myRole === '秘书' ? 'warning' : 'info'"
          class="my-role-tag" effect="dark">
          {{ myRole }}
        </el-tag>
        <el-divider direction="vertical" />
        <el-tag v-for="member in groupMembers" :key="member.no" size="small"
          :type="member.role === '组长' ? 'danger' : member.role === '秘书' ? 'warning' : 'info'" class="member-tag">
          {{ member.name }}（{{ member.role }}）
        </el-tag>
      </div>
    </div>

    <el-card v-if="!loading && (myGroups.length === 0 || totalStudents === 0)" class="table-card empty-card">
      <el-empty :description="myGroups.length === 0 ? '您没有分配到任何答辩小组' : '您负责的答辩小组中暂无需要评定的学生'" />
    </el-card>
    <el-card v-else class="table-card">
      <el-row :gutter="16">
        <el-col :span="5" class="left-col">
          <div class="search-wrapper">
            <el-input v-model="searchKeyword" placeholder="搜索组名/学号/姓名" clearable />
          </div>
          <div class="student-group-list">
            <div v-for="group in filteredGroups" :key="group.groupId" class="group-section">
              <div class="group-header" @click="handleSelectGroup(group)">
                <el-icon class="expand-icon"
                  :class="{ expanded: expandedGroups.includes(group.groupId) || (searchKeyword && group._filteredStudents) }">
                  <ArrowRight />
                </el-icon>
                <span class="group-name">{{ group.groupName }}</span>
                <el-tag size="small" type="info" class="student-count">
                  {{ (group._filteredStudents || allGroupStudents[group.groupId] || []).length }}人
                </el-tag>
              </div>
              <div v-show="expandedGroups.includes(group.groupId) || (searchKeyword && group._filteredStudents)"
                class="group-students">
                <div v-for="student in (group._filteredStudents || allGroupStudents[group.groupId] || [])"
                  :key="student.studentNo" class="student-item"
                  :class="{ active: selectedStudent?.studentNo === student.studentNo }"
                  @click="handleSelectStudent(student, group)">
                  <div class="student-item-header">
                    <div class="student-name">{{ student.studentName }}</div>
                    <el-tag :type="studentStatusMap[student.studentNo]?.type || 'info'" size="small">
                      {{ studentStatusMap[student.studentNo]?.status || '未录入' }}
                    </el-tag>
                  </div>
                  <div class="student-no">{{ student.studentNo }}</div>
                </div>
                <div v-if="!(group._filteredStudents || allGroupStudents[group.groupId] || []).length"
                  class="empty-students">
                  组内暂无学生
                </div>
              </div>
            </div>
            <el-empty v-if="!filteredGroups.length" description="暂无匹配的学生" />
          </div>
        </el-col>
        <el-col :span="19" class="right-col">
          <div v-if="selectedStudent" class="detail-panel">
            <div class="detail-header">
              <h3>{{ selectedStudent.studentName }}（{{ selectedStudent.studentNo }}）</h3>
              <div class="header-info">
                <span>专业：{{ selectedStudent.major || selectedGroup?.major || '-' }}</span>
                <span>班级：{{ selectedStudent.className || '-' }}</span>
                <span>分组：{{ selectedGroup?.groupName || '-' }}</span>
              </div>
            </div>

            <template v-if="myRole === '秘书'">
              <div class="form-section record-section">
                <h4>答辩记录</h4>
                <div class="record-meta">
                  <div class="meta-item">
                    <span class="meta-label">答辩日期：</span>
                    <span class="meta-value">{{ formatDate(form.defenseRecordTime) }}</span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-label">学生姓名：</span>
                    <span class="meta-value">{{ selectedStudent?.studentName }}</span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-label">学&emsp;&emsp;号：</span>
                    <span class="meta-value">{{ selectedStudent?.studentNo }}</span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-label">记&emsp;录&emsp;人：</span>
                    <span class="meta-value">{{ form.defenseRecorderName || userStore.user?.name }}</span>
                  </div>
                </div>
                <div class="record-content-block">
                  <el-form label-width="80px">
                    <el-form-item label="记录内容" required>
                      <el-input v-model="form.defenseRecord" type="textarea" :rows="15" :disabled="!canEditRecord"
                        placeholder="请详细记录答辩过程中的提问、学生回答情况、答辩委员会讨论及表决结果等内容" />
                    </el-form-item>
                  </el-form>
                </div>
                <div class="section-actions">
                  <el-button type="primary" @click="handleSaveRecord" :disabled="!canEditRecord">保存答辩记录</el-button>
                </div>
              </div>

              <div class="form-section">
                <h4>毕业答辩成绩评定</h4>
                <el-alert v-if="myRole === '秘书'" title="您当前身份为秘书，不能编辑成绩和评语，仅可查看" type="warning" show-icon
                  :closable="false" class="section-alert" />
                <el-alert v-if="myRole === '普通成员'" title="您当前身份为普通成员，不能编辑成绩和评语，仅可查看" type="warning" show-icon
                  :closable="false" class="section-alert" />
                <div class="sub-scores-block">
                  <el-form label-width="280px">
                    <div class="sub-scores">
                      <el-form-item v-for="(sub, idx) in subScores" :key="idx" :label="sub.label">
                        <div class="score-row">
                          <el-input-number v-model="form.subScores[idx]" :min="0" :max="sub.full" :controls="false"
                            :disabled="!canEditScore" @change="calculateTotal" style="width: 120px;" />
                          <div class="score-right">
                            <span class="score-hint">满分 {{ sub.full }} 分</span>
                            <div v-if="sub.desc" class="score-desc">{{ sub.desc }}</div>
                          </div>
                        </div>
                      </el-form-item>
                    </div>
                  </el-form>
                </div>
                <div class="total-comment-block">
                  <el-form label-width="80px">
                    <el-form-item label="总成绩">
                      <span class="total-score">{{ form.score ?? '-' }}<span class="total-full"> / 40分</span></span>
                    </el-form-item>
                    <el-form-item label="评语" required>
                      <el-input v-model="form.comment" type="textarea" :rows="12" :disabled="!canEditScore"
                        placeholder="请输入毕业答辩评语，对报告内容、答辩过程、答辩小组评价进行综合评价" />
                    </el-form-item>
                  </el-form>
                </div>
                <div class="section-actions">
                  <el-button type="primary" @click="handleSaveScore" :disabled="!canEditScore">保存成绩</el-button>
                </div>
              </div>
            </template>

            <template v-else>
              <div class="form-section">
                <h4>毕业答辩成绩评定</h4>
                <el-alert v-if="myRole === '普通成员'" title="您当前身份为普通成员，不能编辑成绩和评语，仅可查看" type="warning" show-icon
                  :closable="false" class="section-alert" />
                <div class="sub-scores-block">
                  <el-form label-width="280px">
                    <div class="sub-scores">
                      <el-form-item v-for="(sub, idx) in subScores" :key="idx" :label="sub.label">
                        <div class="score-row">
                          <el-input-number v-model="form.subScores[idx]" :min="0" :max="sub.full" :controls="false"
                            :disabled="!canEditScore" @change="calculateTotal" style="width: 120px;" />
                          <div class="score-right">
                            <span class="score-hint">满分 {{ sub.full }} 分</span>
                            <div v-if="sub.desc" class="score-desc">{{ sub.desc }}</div>
                          </div>
                        </div>
                      </el-form-item>
                    </div>
                  </el-form>
                </div>
                <div class="total-comment-block">
                  <el-form label-width="80px">
                    <el-form-item label="总成绩">
                      <span class="total-score">{{ form.score ?? '-' }}<span class="total-full"> / 40分</span></span>
                    </el-form-item>
                    <el-form-item label="评语" required>
                      <el-input v-model="form.comment" type="textarea" :rows="12" :disabled="!canEditScore"
                        placeholder="请输入毕业答辩评语，对报告内容、答辩过程、答辩小组评价进行综合评价" />
                    </el-form-item>
                  </el-form>
                </div>
                <div class="section-actions">
                  <el-button type="primary" @click="handleSaveScore" :disabled="!canEditScore">保存成绩</el-button>
                </div>
              </div>

              <div class="form-section record-section">
                <h4>答辩记录</h4>
                <el-alert v-if="myRole === '组长'" title="您当前身份为组长，不能编辑答辩记录，仅可查看" type="warning" show-icon
                  :closable="false" class="section-alert" />
                <el-alert v-if="myRole === '普通成员'" title="您当前身份为普通成员，不能编辑答辩记录，仅可查看" type="warning" show-icon
                  :closable="false" class="section-alert" />
                <div class="record-meta">
                  <div class="meta-item">
                    <span class="meta-label">答辩日期：</span>
                    <span class="meta-value">{{ formatDate(form.defenseRecordTime) }}</span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-label">学生姓名：</span>
                    <span class="meta-value">{{ selectedStudent?.studentName }}</span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-label">学&emsp;&emsp;号：</span>
                    <span class="meta-value">{{ selectedStudent?.studentNo }}</span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-label">记&emsp;录&emsp;人：</span>
                    <span class="meta-value">{{ form.defenseRecorderName || userStore.user?.name }}</span>
                  </div>
                </div>
                <div class="record-content-block">
                  <el-form label-width="80px">
                    <el-form-item label="记录内容" required>
                      <el-input v-model="form.defenseRecord" type="textarea" :rows="15" :disabled="!canEditRecord"
                        placeholder="请详细记录答辩过程中的提问、学生回答情况、答辩委员会讨论及表决结果等内容" />
                    </el-form-item>
                  </el-form>
                </div>
                <div class="section-actions">
                  <el-button type="primary" @click="handleSaveRecord" :disabled="!canEditRecord">保存答辩记录</el-button>
                </div>
              </div>
            </template>
          </div>
          <div v-else class="empty-select">请选择左侧学生进行评定</div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<style scoped>
.page-header-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.group-info-header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #f5f7fa;
  text-align: right;
}

.group-name-text {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}

.my-role-tag {
  font-size: 12px;
}

.group-label {
  font-size: 13px;
  color: #606266;
}

.member-tag {
  font-size: 12px;
}

.section-alert {
  margin-bottom: 16px;
}

.table-card {
  margin-top: 16px;
}

.empty-card {
  height: calc(100vh - 180px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.left-col {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 220px);
}

.search-wrapper {
  margin-bottom: 12px;
}

.student-group-list {
  flex: 1;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.group-section {
  border-bottom: 1px solid #ebeef5;
}

.group-header {
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  background-color: #fafafa;
}

.group-header:hover {
  background-color: #f5f7fa;
}

.expand-icon {
  transition: transform 0.2s;
  font-size: 12px;
  color: #909399;
}

.expand-icon.expanded {
  transform: rotate(90deg);
}

.group-name {
  flex: 1;
  color: #303133;
}

.student-count {
  flex-shrink: 0;
}

.group-students {
  background-color: #fff;
}

.student-item {
  padding: 10px 16px 10px 36px;
  border-bottom: 1px solid #f0f2f5;
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
  margin-bottom: 2px;
}

.student-name {
  font-weight: 500;
  color: #303133;
}

.student-no {
  font-size: 12px;
  color: #909399;
}

.empty-students {
  padding: 12px 16px 12px 36px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}

.right-col {
  padding-left: 16px;
  height: calc(100vh - 220px);
}

.detail-panel {
  height: 100%;
  overflow-y: auto;
  padding-right: 20px;
}

.empty-select {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 14px;
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

.section-actions {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed #e4e7ed;
  text-align: left;
}

.score-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.score-right {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.score-hint {
  color: #909399;
  font-size: 14px;
}

.score-desc {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
  line-height: 1.4;
  word-break: break-word;
  white-space: normal;
}

.sub-scores-block {
  padding-bottom: 20px;
  border-bottom: 1px solid #e4e7ed;
  margin-bottom: 20px;
}

.total-comment-block {
  margin-top: 10px;
}

.total-score {
  font-size: 22px;
  font-weight: 700;
  color: #409eff;
}

.total-full {
  font-size: 14px;
  font-weight: 400;
  color: #909399;
}

.empty-select {
  height: calc(100vh - 280px);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 14px;
}

.record-section {
  margin-top: 20px;
}

.record-meta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 24px;
  padding: 16px 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.meta-label {
  color: #606266;
  font-weight: 500;
  white-space: nowrap;
}

.meta-value {
  color: #303133;
}

.record-content-block {
  padding: 0;
}
</style>

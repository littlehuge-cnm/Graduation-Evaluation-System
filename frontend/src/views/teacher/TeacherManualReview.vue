<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import { getTeacherStudents } from '@/api/teacher.js'
import { getStudentTeachers, getStudentDocuments } from '@/api/student.js'
import { getScoreRecordList } from '@/api/scoreRecord.js'
import { getStudentGroupList } from '@/api/studentGroup.js'
import { getGroupMappingList } from '@/api/groupMapping.js'
import { getTeacherGroupById } from '@/api/teacherGroup.js'
import { exportManual, exportManualBatch, previewManual } from '@/api/manualExport.js'

const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const students = ref([])
const studentGroups = ref([])
const keyword = ref('')
const selectedStudent = ref(null)
const CONTENT_SPLIT = '\n\n=====基本要求=====\n\n'
const categoryMap = {
  A: 'A.工程设计',
  B: 'B.科学研究',
  C: 'C.技术开发',
  D: 'D.其他'
}
const typeMap = {
  A: 'A.真题',
  B: 'B.模拟题（假题）',
  C: 'C.真题假作'
}
const newOldMap = {
  A: 'A.新题',
  B: 'B.旧题'
}

const records = ref([])
const teachers = ref(null)
const documents = ref([])
const groupMappings = ref([])
const teacherGroups = ref({})
const activeSection = ref('')
const batchDialogVisible = ref(false)
const selectedStudentNos = ref([])
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewData = ref({})

const navItems = [
  { id: 'section-progress', label: '任务进度' },
  { id: 'section-task', label: '任务书' },
  { id: 'section-guide', label: '指导书' },
  { id: 'section-opening', label: '开题报告' },
  { id: 'section-translation', label: '外文翻译' },
  { id: 'section-midterm', label: '中期检查' },
  { id: 'section-supervisor', label: '指导评语' },
  { id: 'section-reviewer', label: '评阅评分' },
  { id: 'section-defense-record', label: '答辩记录' },
  { id: 'section-defense-score', label: '毕业答辩' },
  { id: 'section-final', label: '总评成绩' }
]

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

function parseSubScores(subScores) {
  if (!subScores) return []
  return subScores.split(',').map(s => parseInt(s.trim()))
}

const filteredStudents = computed(() => {
  if (!keyword.value) return students.value
  const k = keyword.value.trim().toLowerCase()
  return students.value.filter(s =>
    s.studentNo?.toLowerCase().includes(k) || s.studentName?.toLowerCase().includes(k)
  )
})

function getSubScoreDisplay(itemType, index) {
  const r = recordMap.value[itemType]
  if (!r?.subScores) return '-'
  const arr = parseSubScores(r.subScores)
  return arr[index] ?? '-'
}

function getScore(itemType) {
  const r = recordMap.value[itemType]
  return r?.score ?? '未录入'
}

function getComment(itemType) {
  const r = recordMap.value[itemType]
  if (itemType === '答辩记录') {
    return r?.defenseRecord || '暂无答辩记录'
  }
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

function getDoc(docType) {
  return documents.value.find(d => d.docType === docType)
}

async function fetchGroupMappings() {
  try {
    const mappings = await getGroupMappingList()
    groupMappings.value = mappings || []

    const groupIds = [...new Set((mappings || []).map(m => m.teacherGroupId))]
    for (const groupId of groupIds) {
      if (!teacherGroups.value[groupId]) {
        try {
          const group = await getTeacherGroupById(groupId)
          if (group) {
            teacherGroups.value[groupId] = group
          }
        } catch (e) {
          // ignore
        }
      }
    }
  } catch (error) {
    console.error('获取分组映射失败', error)
  }
}

function getStageTeacherGroup(stage) {
  if (!selectedStudent.value?.studentGroupId) return null
  const mapping = groupMappings.value.find(m =>
    m.studentGroupId === selectedStudent.value.studentGroupId &&
    m.stage === stage
  )
  return mapping ? teacherGroups.value[mapping.teacherGroupId] : null
}

function getStageMembersText(stage) {
  const group = getStageTeacherGroup(stage)
  if (!group) return '-'
  const members = [
    group.leaderName ? `${group.leaderName}（组长）` : '',
    group.secretaryName ? `${group.secretaryName}（秘书）` : '',
    group.memberName ? `${group.memberName}（组员）` : ''
  ].filter(Boolean)
  return members.join('、') || '-'
}

function getTaskContentParts() {
  const doc = getDoc('任务书')
  if (!doc?.content) return { mainContent: '', basicRequirement: '' }
  const content = doc.content
  const splitIndex = content.indexOf(CONTENT_SPLIT)
  if (splitIndex >= 0) {
    return {
      mainContent: content.substring(0, splitIndex),
      basicRequirement: content.substring(splitIndex + CONTENT_SPLIT.length)
    }
  }
  return { mainContent: content, basicRequirement: '' }
}

function getCategoryLabel(value) {
  return categoryMap[value] || value || '-'
}

function getTypeLabel(value) {
  return typeMap[value] || value || '-'
}

function getNewOldLabel(value) {
  return newOldMap[value] || value || '-'
}

function isCompleted(itemType) {
  if (itemType === '任务书' || itemType === '指导书') {
    return !!getDoc(itemType)?.content
  }
  const record = recordMap.value[itemType]
  if (!record) return false
  if (itemType === '答辩记录') {
    return !!record.defenseRecord
  }
  return !!record.score || !!record.comment
}

function getStatusType(itemType) {
  return isCompleted(itemType) ? 'success' : 'info'
}

function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', filename)
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

async function handlePreview() {
  if (!selectedStudent.value) {
    ElMessage.warning('请先选择学生')
    return
  }
  previewLoading.value = true
  previewVisible.value = true
  try {
    previewData.value = await previewManual(selectedStudent.value.studentNo)
  } catch (error) {
    ElMessage.error(error.message || '获取预览数据失败')
    previewVisible.value = false
  } finally {
    previewLoading.value = false
  }
}

function pv(key) {
  return previewData.value[key] || ''
}

async function handleExportSingle() {
  if (!selectedStudent.value) {
    ElMessage.warning('请先选择学生')
    return
  }
  try {
    const res = await exportManual(selectedStudent.value.studentNo)
    const filename = `${selectedStudent.value.studentName}_${selectedStudent.value.studentNo}_评价手册.docx`
    downloadBlob(res, filename)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error(error.message || '导出失败')
  }
}

function openBatchExportDialog() {
  if (!students.value.length) {
    ElMessage.warning('暂无学生可导出')
    return
  }
  selectedStudentNos.value = []
  batchDialogVisible.value = true
}

function toggleSelectAll(checked) {
  if (checked) {
    selectedStudentNos.value = filteredStudents.value.map(s => s.studentNo)
  } else {
    selectedStudentNos.value = []
  }
}

const isAllSelected = computed(() => {
  return filteredStudents.value.length > 0 && filteredStudents.value.every(s => selectedStudentNos.value.includes(s.studentNo))
})

const isIndeterminate = computed(() => {
  const selectedCount = filteredStudents.value.filter(s => selectedStudentNos.value.includes(s.studentNo)).length
  return selectedCount > 0 && selectedCount < filteredStudents.value.length
})

async function confirmBatchExport() {
  if (selectedStudentNos.value.length === 0) {
    ElMessage.warning('请至少选择一名学生')
    return
  }
  try {
    const res = await exportManualBatch(selectedStudentNos.value)
    downloadBlob(res, '选中学生评价手册导出.zip')
    ElMessage.success(`成功导出 ${selectedStudentNos.value.length} 名学生的评价手册`)
    batchDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error.message || '批量导出失败')
  }
}

function scrollToSection(id) {
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

function handleScroll() {
  const panel = document.querySelector('.detail-panel')
  if (!panel) return
  for (let i = navItems.length - 1; i >= 0; i--) {
    const el = document.getElementById(navItems[i].id)
    if (el) {
      const rect = el.getBoundingClientRect()
      const panelRect = panel.getBoundingClientRect()
      if (rect.top <= panelRect.top + 100) {
        activeSection.value = navItems[i].id
        break
      }
    }
  }
}

async function fetchStudents() {
  loading.value = true
  try {
    const [superviseRes, groupsRes] = await Promise.all([
      getTeacherStudents(userStore.username, '指导'),
      getStudentGroupList()
    ])
    students.value = superviseRes
    studentGroups.value = groupsRes || []
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
  loading.value = true
  try {
    const [recordRes, teacherRes, docRes] = await Promise.all([
      getScoreRecordList(student.studentNo),
      getStudentTeachers(student.studentNo),
      getStudentDocuments(student.studentNo)
    ])
    records.value = recordRes || []
    teachers.value = teacherRes || { supervisor: null, reviewer: null }
    documents.value = docRes || []
    nextTick(() => {
      activeSection.value = 'section-progress'
    })
  } catch (error) {
    ElMessage.error(error.message || '获取评价记录失败')
    records.value = []
    teachers.value = null
    documents.value = []
  } finally {
    loading.value = false
  }
}

const finalTotalScore = computed(() => {
  const r = recordMap.value['委员会评定']
  return r?.score ?? '-'
})

const finalGrade = computed(() => {
  const r = recordMap.value['委员会评定']
  return r?.grade ?? '未评定'
})

const progressItems = [
  { label: '任务书', type: '任务书' },
  { label: '指导书', type: '指导书' },
  { label: '开题报告', type: '开题报告成绩' },
  { label: '外文翻译', type: '外文翻译' },
  { label: '中期检查', type: '中期检查成绩' },
  { label: '指导评语', type: '指导评语' },
  { label: '评阅评分', type: '评阅评语' },
  { label: '答辩记录', type: '答辩记录' },
  { label: '毕业答辩', type: '毕业答辩成绩' },
  { label: '总评成绩', type: '委员会评定' }
]

onMounted(() => {
  fetchStudents()
  fetchGroupMappings()
})
</script>

<template>
  <div v-loading="loading">
    <el-page-header title="评价手册查看" :icon="null" />
    <el-card v-if="!loading && students.length === 0" class="table-card empty-card">
      <el-empty description="您没有需要查看评价手册的学生" />
    </el-card>
    <el-card v-else class="table-card">
      <el-row :gutter="16">
        <el-col :span="5" class="left-col">
          <div class="student-list-header">
            <el-input v-model="keyword" placeholder="搜索学号/姓名" clearable />
          </div>
          <div class="student-list">
            <div v-for="student in filteredStudents" :key="student.studentNo" class="student-item"
              :class="{ active: selectedStudent?.studentNo === student.studentNo }"
              @click="handleSelectStudent(student)">
              <div class="student-name">{{ student.studentName }}</div>
              <div class="student-no">{{ student.studentNo }}</div>
            </div>
            <el-empty v-if="!filteredStudents.length" description="暂无匹配学生" />
          </div>
          <div class="action-bar">
            <el-button type="primary" size="small" @click="handlePreview" :disabled="!selectedStudent"
              class="action-btn">预览手册Word格式</el-button>
            <el-button type="success" size="small" @click="handleExportSingle" :disabled="!selectedStudent"
              class="action-btn">导出手册Word格式</el-button>
            <el-button type="warning" size="small" @click="openBatchExportDialog"
              class="action-btn">批量导出评价手册</el-button>
          </div>
        </el-col>
        <el-col :span="19" class="right-col">
          <div v-if="selectedStudent" class="detail-wrapper">
            <div class="detail-panel" @scroll="handleScroll">
              <div class="detail-header">
                <h3>{{ selectedStudent.studentName }}（{{ selectedStudent.studentNo }}）</h3>
                <div class="header-info">
                  <span>专业：{{ selectedStudent.major || '-' }}</span>
                  <span>班级：{{ selectedStudent.className || '-' }}</span>
                  <span>年级：{{ selectedStudent.grade || '-' }}</span>
                </div>
                <div class="header-info">
                  <span>学生组：{{ groupNameMap[selectedStudent.studentGroupId] || '未分组' }}</span>
                  <span>指导教师：{{ teachers?.supervisor?.teacherName || '-' }}</span>
                  <span>评阅教师：{{ teachers?.reviewer?.teacherName || '-' }}</span>
                </div>
              </div>

              <div id="section-progress" class="section">
                <h4>评价任务进度</h4>
                <div class="progress-horizontal">
                  <div v-for="item in progressItems" :key="item.type" class="progress-item"
                    :class="getStatusType(item.type)">
                    <el-tag :type="getStatusType(item.type)" size="small" class="progress-tag">
                      {{ isCompleted(item.type) ? '已完成' : '未完成' }}
                    </el-tag>
                    <div class="progress-name">{{ item.label }}</div>
                  </div>
                </div>
              </div>

              <div id="section-task" class="section">
                <h4>任务书</h4>
                <div class="doc-info-grid">
                  <div class="doc-info-item full-width">
                    <span class="info-label">毕业设计（论文）题目：</span>
                    <span class="info-value">{{ getDoc('任务书')?.title || '-' }}</span>
                  </div>
                  <div class="doc-info-item">
                    <span class="info-label">课题类别：</span>
                    <span class="info-value">{{ getCategoryLabel(getDoc('任务书')?.subjectCategory) }}</span>
                  </div>
                  <div class="doc-info-item">
                    <span class="info-label">课题类型：</span>
                    <span class="info-value">{{ getTypeLabel(getDoc('任务书')?.subjectType) }}</span>
                  </div>
                  <div class="doc-info-item">
                    <span class="info-label">新旧课题：</span>
                    <span class="info-value">{{ getNewOldLabel(getDoc('任务书')?.subjectNewOld) }}</span>
                  </div>
                </div>
                <div v-if="getDoc('任务书')?.content" class="doc-content">
                  <div class="content-section">
                    <h5 class="content-title">课题研究的主要内容</h5>
                    <div class="content-text">{{ getTaskContentParts().mainContent }}</div>
                  </div>
                  <div v-if="getTaskContentParts().basicRequirement" class="content-section" style="margin-top: 16px;">
                    <h5 class="content-title">基本要求</h5>
                    <div class="content-text">{{ getTaskContentParts().basicRequirement }}</div>
                  </div>
                </div>
                <div v-else class="empty-hint">暂无任务书内容</div>
              </div>

              <div id="section-guide" class="section">
                <h4>指导书</h4>
                <div v-if="getDoc('指导书')?.content" class="doc-content">
                  <div class="content-text">{{ getDoc('指导书').content }}</div>
                </div>
                <div v-else class="empty-hint">暂无指导书内容</div>
              </div>

              <div id="section-opening" class="section">
                <h4>开题报告成绩</h4>
                <div class="meta-info"
                  style="margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px dashed #ebeef5;">
                  <strong>评定教师组：</strong>{{ getStageTeacherGroup('开题')?.groupName || '未分配' }}
                  &nbsp;|&nbsp;
                  <strong>组员：</strong>{{ getStageMembersText('开题') }}
                </div>
                <el-table :data="[{
                  name: '得分',
                  item1: getSubScoreDisplay('开题报告成绩', 0),
                  item2: getSubScoreDisplay('开题报告成绩', 1),
                  item3: getSubScoreDisplay('开题报告成绩', 2),
                  total: getScore('开题报告成绩')
                }, {
                  name: '满分',
                  item1: 4,
                  item2: 4,
                  item3: 4,
                  total: 12
                }]" border>
                  <el-table-column prop="name" label="" width="80" />
                  <el-table-column prop="item1" label="调研资料的获取能力" />
                  <el-table-column prop="item2" label="课题方案设计的合理性" />
                  <el-table-column prop="item3" label="开题报告的规范性与质量" />
                  <el-table-column prop="total" label="总成绩" width="100" />
                </el-table>
                <div class="comment-block">
                  <div class="meta-info"><strong>录入人：</strong>{{ getRecorderName('开题报告成绩') }} &nbsp;|&nbsp;
                    <strong>录入时间：</strong>{{ getRecordTime('开题报告成绩') }}
                  </div>
                  <div class="comment-text">{{ getComment('开题报告成绩') }}</div>
                </div>
              </div>

              <div id="section-translation" class="section">
                <h4>外文翻译成绩</h4>
                <el-table :data="[{
                  name: '得分',
                  item1: getSubScoreDisplay('外文翻译', 0),
                  item2: getSubScoreDisplay('外文翻译', 1),
                  item3: getSubScoreDisplay('外文翻译', 2),
                  total: getScore('外文翻译')
                }, {
                  name: '满分',
                  item1: 1,
                  item2: 1,
                  item3: 1,
                  total: 3
                }]" border>
                  <el-table-column prop="name" label="" width="80" />
                  <el-table-column prop="item1" label="对外文资料的阅读理解能力" />
                  <el-table-column prop="item2" label="专业词语翻译的准确性" />
                  <el-table-column prop="item3" label="译文规范性与质量" />
                  <el-table-column prop="total" label="总成绩" width="100" />
                </el-table>
                <div class="comment-block">
                  <div class="meta-info"><strong>录入人：</strong>{{ getRecorderName('外文翻译') }} &nbsp;|&nbsp;
                    <strong>录入时间：</strong>{{ getRecordTime('外文翻译') }}
                  </div>
                  <div class="comment-text">{{ getComment('外文翻译') }}</div>
                </div>
              </div>

              <div id="section-midterm" class="section">
                <h4>中期检查评语及成绩</h4>
                <div class="meta-info"
                  style="margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px dashed #ebeef5;">
                  <strong>评定教师组：</strong>{{ getStageTeacherGroup('中期')?.groupName || '未分配' }}
                  &nbsp;|&nbsp;
                  <strong>组员：</strong>{{ getStageMembersText('中期') }}
                </div>
                <el-table :data="[{
                  name: '得分',
                  item1: getSubScoreDisplay('中期检查成绩', 0),
                  item2: getSubScoreDisplay('中期检查成绩', 1),
                  item3: getSubScoreDisplay('中期检查成绩', 2),
                  total: getScore('中期检查成绩')
                }, {
                  name: '满分',
                  item1: 5,
                  item2: 5,
                  item3: 5,
                  total: 15
                }]" border>
                  <el-table-column prop="name" label="" width="80" />
                  <el-table-column prop="item1" label="完成毕业设计进度情况" />
                  <el-table-column prop="item2" label="综合能力" />
                  <el-table-column prop="item3" label="已完成的部分毕业设计质量" />
                  <el-table-column prop="total" label="总成绩" width="100" />
                </el-table>
                <div class="comment-block">
                  <div class="meta-info"><strong>录入人：</strong>{{ getRecorderName('中期检查成绩') }} &nbsp;|&nbsp;
                    <strong>录入时间：</strong>{{ getRecordTime('中期检查成绩') }}
                  </div>
                  <div class="comment-text">{{ getComment('中期检查成绩') }}</div>
                </div>
              </div>

              <div id="section-supervisor" class="section">
                <h4>指导老师评语及建议成绩</h4>
                <el-table :data="[{
                  name: '得分',
                  item1: getSubScoreDisplay('指导评语', 0),
                  item2: getSubScoreDisplay('指导评语', 1),
                  item3: getSubScoreDisplay('指导评语', 2),
                  item4: getSubScoreDisplay('指导评语', 3),
                  item5: getSubScoreDisplay('指导评语', 4),
                  total: getScore('指导评语')
                }, {
                  name: '满分',
                  item1: 3,
                  item2: 3,
                  item3: 3,
                  item4: 3,
                  item5: 3,
                  total: 15
                }]" border>
                  <el-table-column prop="name" label="" width="80" />
                  <el-table-column prop="item1" label="设计（实验）方案、研究方案及软硬件方案设计能力" />
                  <el-table-column prop="item2" label="基本概念、基本理论的应用能力" />
                  <el-table-column prop="item3" label="分析问题、解决问题及知识综合运用能力" />
                  <el-table-column prop="item4" label="科学素养、学习态度、纪律表现" />
                  <el-table-column prop="item5" label="工作量及毕业设计（论文）规范与质量" />
                  <el-table-column prop="total" label="总成绩" width="90" />
                </el-table>
                <div class="comment-block">
                  <div class="meta-info"><strong>指导老师：</strong>{{ getRecorderName('指导评语') }} &nbsp;|&nbsp;
                    <strong>录入时间：</strong>{{ getRecordTime('指导评语') }}
                  </div>
                  <div class="comment-text">{{ getComment('指导评语') }}</div>
                </div>
              </div>

              <div id="section-reviewer" class="section">
                <h4>评阅教师评分及评语</h4>
                <el-table :data="[{
                  name: '得分',
                  item1: getSubScoreDisplay('评阅评语', 0),
                  item2: getSubScoreDisplay('评阅评语', 1),
                  item3: getSubScoreDisplay('评阅评语', 2),
                  item4: getSubScoreDisplay('评阅评语', 3),
                  total: getScore('评阅评语')
                }, {
                  name: '满分',
                  item1: 4,
                  item2: 4,
                  item3: 4,
                  item4: 3,
                  total: 15
                }]" border>
                  <el-table-column prop="name" label="" width="80" />
                  <el-table-column prop="item1" label="毕业设计（论文）规范性与质量" />
                  <el-table-column prop="item2" label="基本理论和基本知识运用情况" />
                  <el-table-column prop="item3" label="研究方案及设计方案" />
                  <el-table-column prop="item4" label="毕业设计（论文）创新性" />
                  <el-table-column prop="total" label="总成绩" width="90" />
                </el-table>
                <div class="comment-block">
                  <div class="meta-info"><strong>评阅老师：</strong>{{ getRecorderName('评阅评语') }} &nbsp;|&nbsp;
                    <strong>录入时间：</strong>{{ getRecordTime('评阅评语') }}
                  </div>
                  <div class="comment-text">{{ getComment('评阅评语') }}</div>
                </div>
              </div>

              <div id="section-defense-record" class="section">
                <h4>答辩记录</h4>
                <div class="meta-info"><strong>记录人：</strong>{{ getRecorderName('答辩记录') }} &nbsp;|&nbsp;
                  <strong>答辩日期：</strong>{{ getRecordTime('答辩记录') }}
                </div>
                <div class="comment-block">
                  <div class="comment-text">{{ getComment('答辩记录') }}</div>
                </div>
              </div>

              <div id="section-defense-score" class="section">
                <h4>毕业答辩小组评定成绩</h4>
                <div class="meta-info"
                  style="margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px dashed #ebeef5;">
                  <strong>答辩教师组：</strong>{{ getStageTeacherGroup('答辩')?.groupName || '未分配' }}
                  &nbsp;|&nbsp;
                  <strong>组员：</strong>{{ getStageMembersText('答辩') }}
                </div>
                <el-table :data="[{
                  name: '得分',
                  item1: getSubScoreDisplay('毕业答辩成绩', 0),
                  item2: getSubScoreDisplay('毕业答辩成绩', 1),
                  item3: getSubScoreDisplay('毕业答辩成绩', 2),
                  item4: getSubScoreDisplay('毕业答辩成绩', 3),
                  total: getScore('毕业答辩成绩')
                }, {
                  name: '满分',
                  item1: 10,
                  item2: 10,
                  item3: 10,
                  item4: 10,
                  total: 40
                }]" border>
                  <el-table-column prop="name" label="" width="80" />
                  <el-table-column prop="item1" label="毕业设计（论文）陈述情况" />
                  <el-table-column prop="item2" label="毕业设计（论文）水平" />
                  <el-table-column prop="item3" label="毕业设计（论文）工作量评价" />
                  <el-table-column prop="item4" label="答辩情况" />
                  <el-table-column prop="total" label="总成绩" width="100" />
                </el-table>
                <div class="comment-block">
                  <div class="meta-info"><strong>录入人：</strong>{{ getRecorderName('毕业答辩成绩') }} &nbsp;|&nbsp;
                    <strong>录入时间：</strong>{{ getRecordTime('毕业答辩成绩') }}
                  </div>
                  <div class="comment-text">{{ getComment('毕业答辩成绩') }}</div>
                </div>
              </div>

              <div id="section-final" class="section">
                <h4>答辩委员会评语及总评成绩</h4>
                <el-table :data="[
                  { item: '开题报告', weight: '12%', fullScore: 12, score: getScore('开题报告成绩') },
                  { item: '外文翻译', weight: '3%', fullScore: 3, score: getScore('外文翻译') },
                  { item: '中期检查', weight: '15%', fullScore: 15, score: getScore('中期检查成绩') },
                  { item: '指导教师', weight: '15%', fullScore: 15, score: getScore('指导评语') },
                  { item: '评阅教师', weight: '15%', fullScore: 15, score: getScore('评阅评语') },
                  { item: '毕业答辩', weight: '40%', fullScore: 40, score: getScore('毕业答辩成绩') }
                ]" border>
                  <el-table-column prop="item" label="成绩评定项" width="180" />
                  <el-table-column prop="weight" label="权重" width="100" />
                  <el-table-column prop="fullScore" label="满分" width="100" />
                  <el-table-column prop="score" label="得分" />
                </el-table>
                <div class="total-score-area">
                  <div class="total-score-item">
                    <span class="total-label">总分</span>
                    <span class="total-score">{{ finalTotalScore }}</span>
                  </div>
                  <div class="total-score-item">
                    <span class="total-label">评定等级</span>
                    <span :class="['grade-tag', finalGrade]">{{ finalGrade }}</span>
                  </div>
                </div>
                <div class="comment-block">
                  <div class="meta-info"><strong>录入人：</strong>{{ getRecorderName('委员会评定') }} &nbsp;|&nbsp;
                    <strong>录入时间：</strong>{{ getRecordTime('委员会评定') }}
                  </div>
                  <div class="comment-text">{{ getComment('委员会评定') }}</div>
                </div>
                <div class="note-text">
                  备注：（1）等级评定：优，良，中，及格，不及格（2）有不合格二次重做或重答辩的一律为及格（3）二次重做或重答辩还有不合格的一律为不及格
                </div>
              </div>
            </div>

            <div class="float-nav">
              <div v-for="item in navItems" :key="item.id" class="nav-item"
                :class="{ active: activeSection === item.id }" @click="scrollToSection(item.id)">
                {{ item.label }}
              </div>
            </div>
          </div>
          <div v-else class="empty-select">
            <span>请选择左侧学生查看详情</span>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-dialog v-model="batchDialogVisible" title="选择要导出的学生" width="500px">
      <div style="margin-bottom: 12px;">
        <el-checkbox :model-value="isAllSelected" :indeterminate="isIndeterminate"
          @change="toggleSelectAll">全选当前列表</el-checkbox>
        <span style="margin-left: 16px; color: #909399; font-size: 13px;">已选择 {{ selectedStudentNos.length }} 名学生</span>
      </div>
      <div style="max-height: 400px; overflow-y: auto; border: 1px solid #ebeef5; border-radius: 4px;">
        <el-checkbox-group v-model="selectedStudentNos" style="width: 100%;">
          <div v-for="student in filteredStudents" :key="student.studentNo"
            style="padding: 10px 16px; border-bottom: 1px solid #f0f0f0;">
            <el-checkbox :value="student.studentNo">
              {{ student.studentName }}（{{ student.studentNo }}）- {{ student.className || '-' }}
            </el-checkbox>
          </div>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBatchExport">确认导出</el-button>
      </template>
    </el-dialog>

    <!-- 预览弹窗 -->
    <el-dialog v-model="previewVisible" title="评价手册预览" width="90%" top="3vh" destroy-on-close class="preview-dialog">
      <div v-loading="previewLoading" class="preview-doc">
        <!-- 封面 -->
        <div class="preview-page preview-cover">
          <div class="cover-spacer"></div>
          <p class="cover-school">智能科学与技术学院</p>
          <p class="cover-dept">计算机系</p>
          <div class="cover-spacer"></div>
          <div class="cover-spacer"></div>
          <p class="cover-title">毕业设计评价手册</p>
          <div class="cover-spacer"></div>
          <div class="cover-spacer"></div>
          <div class="cover-fields">
            <p>指导教师 <span class="fill-line">{{ pv('a') }}</span></p>
            <p>　　　 职　称 <span class="fill-line">{{ pv('b') }}</span></p>
            <p>学生姓名 <span class="fill-line">{{ pv('c') }}</span></p>
            <p>专　　业 <span class="fill-line">{{ pv('d') }}</span></p>
            <p>班　　级 <span class="fill-line">{{ pv('e') }}</span></p>
            <p>学　　号 <span class="fill-line">{{ pv('f') }}</span></p>
          </div>
          <div class="cover-spacer"></div>
          <p class="cover-time">毕业设计（论文）时间：自2026年2月 至 2026年6月</p>
        </div>

        <!-- 一、任务书 -->
        <div class="preview-page">
          <p class="section-heading">一、毕业设计（论文）任务书</p>
          <table class="preview-table">
            <tr>
              <td class="col-label">毕业设计（论文）题目</td>
              <td colspan="2">{{ pv('g') }}</td>
            </tr>
            <tr>
              <td class="col-label">课题类别</td>
              <td>A.工程设计B.科学研究C.技术开发D.其他</td>
              <td class="col-select">{{ pv('h') }}</td>
            </tr>
            <tr>
              <td class="col-label">课题类型</td>
              <td>A.真题B.模拟题（假题）C.真题假作</td>
              <td class="col-select">{{ pv('i') }}</td>
            </tr>
            <tr>
              <td class="col-label">新旧课题</td>
              <td>A.新题B.旧题</td>
              <td class="col-select">{{ pv('j') }}</td>
            </tr>
            <tr>
              <td colspan="3" class="content-cell">课题研究的主要内容<br />{{ pv('k') }}</td>
            </tr>
            <tr>
              <td colspan="3" class="content-cell">基本要求<br />{{ pv('l') }}</td>
            </tr>
            <tr>
              <td colspan="3">导师签字：{{ pv('m') }}&emsp;{{ pv('n') }}年&emsp;{{ pv('o') }}月&emsp;{{ pv('p') }}日</td>
            </tr>
            <tr>
              <td colspan="3">系意见：{{ pv('q') }}系主任签字：{{ pv('r') }}&emsp;{{ pv('s') }}年&emsp;{{ pv('t') }}月&emsp;{{
                pv('u')
              }}日</td>
            </tr>
            <tr>
              <td colspan="3">学院意见：{{ pv('v') }} 院长签字:{{ pv('w') }}&emsp;{{ pv('x') }}年&emsp;{{ pv('y') }}月&emsp;{{
                pv('z')
              }}日</td>
            </tr>
          </table>
        </div>

        <!-- 二、指导书 -->
        <div class="preview-page">
          <p class="section-heading">二、毕业设计(论文)指导书</p>
          <table class="preview-table">
            <tr>
              <td class="content-cell">{{ pv('A') }}</td>
            </tr>
          </table>
        </div>

        <!-- 三、开题报告成绩 -->
        <div class="preview-page">
          <p class="section-heading">三、开题报告成绩</p>
          <table class="preview-table">
            <tr>
              <th>检查项目</th>
              <th>分值</th>
            </tr>
            <tr>
              <td>1．调研资料的获取能力（资料的检索、收集、整理、归纳能力；调研的范围、地点、领域内国内外的发展状况、已取得的应用成果、目前存在的问题等）</td>
              <td>4</td>
            </tr>
            <tr>
              <td>2．课题方案设计的合理性（拟采用的设计方案的合理性、有效性与可行性）</td>
              <td>4</td>
            </tr>
            <tr>
              <td>3．开题报告的规范性与质量</td>
              <td>4</td>
            </tr>
            <tr>
              <td>开题报告总成绩（由小组给分）</td>
              <td>{{ pv('H') }}</td>
            </tr>
          </table>
        </div>

        <!-- 四、外文翻译成绩 -->
        <div class="preview-page">
          <p class="section-heading">四、外文翻译成绩</p>
          <table class="preview-table">
            <tr>
              <th>检查项目</th>
              <th>分值</th>
              <th>成绩</th>
            </tr>
            <tr>
              <td>对外文资料的阅读理解能力（深入理解外文资料的全文，译文能够做到兼顾上下文）</td>
              <td>1</td>
              <td>{{ pv('I') }}</td>
            </tr>
            <tr>
              <td>专业词语翻译的准确性（译文中专业术语准确、规范）</td>
              <td>1</td>
              <td>{{ pv('J') }}</td>
            </tr>
            <tr>
              <td>译文规范性与质量（译文的格式符合校论文规范、译文行文流畅）</td>
              <td>1</td>
              <td>{{ pv('K') }}</td>
            </tr>
            <tr>
              <td>外文翻译总成绩（指导教师给分）</td>
              <td colspan="2">{{ pv('L') }}</td>
            </tr>
          </table>
        </div>

        <!-- 五、中期检查评语及成绩 -->
        <div class="preview-page">
          <p class="section-heading">五、中期检查评语及成绩</p>
          <table class="preview-table">
            <tr>
              <th>检查项目</th>
              <th>分值</th>
            </tr>
            <tr>
              <td>1．完成毕业设计进度情况（超前、正常、滞后）</td>
              <td>5</td>
            </tr>
            <tr>
              <td>2．综合能力（独立工作能力、调研能力、对知识的综合运用能力等）</td>
              <td>5</td>
            </tr>
            <tr>
              <td>3．已完成的部分毕业设计质量</td>
              <td>5</td>
            </tr>
            <tr>
              <td>总成绩</td>
              <td>{{ pv('M') }}</td>
            </tr>
            <tr>
              <td colspan="2" class="content-cell">检查小组评语<br />{{ pv('N') }}<br />检查组长签字：{{ pv('O') }}&emsp;{{ pv('P')
              }}年&emsp;{{ pv('Q') }}月&emsp;{{ pv('R') }}日</td>
            </tr>
          </table>
        </div>

        <!-- 六、指导老师评语及建议成绩 -->
        <div class="preview-page">
          <p class="section-heading">六、指导老师评语及建议成绩</p>
          <table class="preview-table">
            <tr>
              <th>考核项目</th>
              <th>分值</th>
              <th>成绩</th>
            </tr>
            <tr>
              <td>1．设计（实验）方案、研究方案及软硬件方案设计能力</td>
              <td>3</td>
              <td>{{ pv('S') }}</td>
            </tr>
            <tr>
              <td>2．基本概念、基本理论的应用能力</td>
              <td>3</td>
              <td>{{ pv('T') }}</td>
            </tr>
            <tr>
              <td>3．分析问题、解决问题及知识综合运用能力</td>
              <td>3</td>
              <td>{{ pv('U') }}</td>
            </tr>
            <tr>
              <td>4．科学素养、学习态度、纪律表现</td>
              <td>3</td>
              <td>{{ pv('V') }}</td>
            </tr>
            <tr>
              <td>5．工作量及毕业设计（论文）规范与质量</td>
              <td>3</td>
              <td>{{ pv('W') }}</td>
            </tr>
            <tr>
              <td>合计</td>
              <td>15</td>
              <td>{{ pv('X') }}</td>
            </tr>
            <tr>
              <td colspan="3" class="content-cell">指导老师评语<br />{{ pv('Y') }}<br />指导老师签字:{{ pv('Z') }}&emsp;{{ pv('aa')
              }}年&emsp;{{ pv('ab') }}月&emsp;{{ pv('ac') }}日</td>
            </tr>
          </table>
        </div>

        <!-- 七、评阅教师评分及评语 -->
        <div class="preview-page">
          <p class="section-heading">七、评阅教师评分及评语</p>
          <table class="preview-table">
            <tr>
              <th>考核项目</th>
              <th>分值</th>
              <th>成绩</th>
            </tr>
            <tr>
              <td>1．毕业设计（论文）规范性与质量（格式的规范性，是否符合要求）</td>
              <td>4</td>
              <td>{{ pv('ad') }}</td>
            </tr>
            <tr>
              <td>2．基本理论和基本知识运用情况（运用与课题相关的基础理论和专业知识分析问题和解决问题的能力；理论、公式的正确性；概念是否清楚，应用是否合理）</td>
              <td>4</td>
              <td>{{ pv('ae') }}</td>
            </tr>
            <tr>
              <td>3．研究方案及设计方案（方案的正确性、逻辑性；论证的严密性，计算是否准确，有无表达错误）</td>
              <td>4</td>
              <td>{{ pv('af') }}</td>
            </tr>
            <tr>
              <td>4．毕业设计（论文）创新性（研究成果是否具有一定水平，有自己的见解，研究成果有一定的学术或应用价值）</td>
              <td>3</td>
              <td>{{ pv('ag') }}</td>
            </tr>
            <tr>
              <td>合计</td>
              <td>15</td>
              <td>{{ pv('ah') }}</td>
            </tr>
            <tr>
              <td colspan="3" class="content-cell">
                评阅老师评语（对学生毕业设计中完成的任务、涉及的知识、成果的数量与质量、体现的工作能力与创新精神、存在的问题等做出综合评价）：<br />{{
                  pv('ai') }}<br />评阅老师签字：{{ pv('aj') }}&emsp;{{ pv('ak') }}年&emsp;{{ pv('al') }}月&emsp;{{ pv('am') }}日
              </td>
            </tr>
          </table>
        </div>

        <!-- 八、答辩记录 -->
        <div class="preview-page">
          <p class="section-heading">八、答辩记录</p>
          <table class="preview-table">
            <tr>
              <td>答辩日期</td>
              <td>{{ pv('an') }}</td>
              <td>学生姓名</td>
              <td>{{ pv('ao') }}</td>
              <td>记录人</td>
              <td>{{ pv('ap') }}</td>
            </tr>
            <tr>
              <td colspan="6" class="content-cell">{{ pv('aq') }}</td>
            </tr>
          </table>
        </div>

        <!-- 九、答辩小组评定成绩 -->
        <div class="preview-page">
          <p class="section-heading">九、答辩小组评定成绩</p>
          <table class="preview-table">
            <tr>
              <th>成绩评定项</th>
              <th>评定项分值</th>
            </tr>
            <tr>
              <td>1．毕业设计（论文）陈述情况（是否简明扼要、思路清晰；语言表达准确，概念清楚，论点正确，分析归纳合理）</td>
              <td>10分</td>
            </tr>
            <tr>
              <td>2．毕业设计（论文）水平（是否有独到的见解，富有新意，或对某些问题有较深刻的分析，有较高的学术水平或较高的实用价值）</td>
              <td>10分</td>
            </tr>
            <tr>
              <td>3．毕业设计（论文）工作量评价（工作量饱满程度，独立完成情况）</td>
              <td>10分</td>
            </tr>
            <tr>
              <td>4．答辩情况（是否能够准确深入地回答所提出的问题，基本概念清楚，有理有据，语言表达能力、是否得体）</td>
              <td>10分</td>
            </tr>
            <tr>
              <td>答辩成绩</td>
              <td>{{ pv('ar') }}</td>
            </tr>
            <tr>
              <td colspan="2" class="content-cell">答辩小组评语<br />{{ pv('as') }}<br />答辩小组组长签字：{{ pv('at')
              }}<br />答辩小组成员签字：{{
                  pv('au') }}<br />{{ pv('av') }}年&emsp;{{ pv('aw') }}月&emsp;{{ pv('ax') }}日</td>
            </tr>
          </table>
        </div>

        <!-- 十、答辩委员会评语及总评成绩 -->
        <div class="preview-page">
          <p class="section-heading">十、答辩委员会评语及总评成绩</p>
          <table class="preview-table">
            <tr>
              <th>成绩评定项</th>
              <th>得分</th>
              <th>备注</th>
            </tr>
            <tr>
              <td>开题报告12%</td>
              <td>{{ pv('ay') }}</td>
              <td>{{ pv('az') }}</td>
            </tr>
            <tr>
              <td>英文翻译3%</td>
              <td>{{ pv('ba') }}</td>
              <td>{{ pv('bb') }}</td>
            </tr>
            <tr>
              <td>中期检查15%</td>
              <td>{{ pv('bc') }}</td>
              <td>{{ pv('bd') }}</td>
            </tr>
            <tr>
              <td>指导教师15%</td>
              <td>{{ pv('be') }}</td>
              <td>{{ pv('bf') }}</td>
            </tr>
            <tr>
              <td>评阅教师15%</td>
              <td>{{ pv('bg') }}</td>
              <td>{{ pv('bh') }}</td>
            </tr>
            <tr>
              <td>答辩40%</td>
              <td>{{ pv('bi') }}</td>
              <td>{{ pv('bj') }}</td>
            </tr>
            <tr>
              <td>总分100%</td>
              <td colspan="2">{{ pv('bk') }}</td>
            </tr>
            <tr>
              <td>评定等级</td>
              <td colspan="2">{{ pv('bl') }}</td>
            </tr>
            <tr>
              <td colspan="3" class="content-cell">答辩委员会评语<br />{{ pv('bm') }}<br />答辩委员会主任签字（盖章）{{ pv('bn')
              }}<br />答辩委员会秘书签字（盖章）{{ pv('bo') }}</td>
            </tr>
          </table>
          <p class="footer-note">备注：（1）等级评定：优，良，中，及格，不及格（2）有不合格二次重做或重答辩的一律为及格（3）二次重做或重答辩还有不合格的一律为不及格</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.table-card {
  margin-top: 16px;
}

.empty-card {
  height: calc(100vh - 150px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.student-list-header {
  margin-bottom: 12px;
}

.left-col {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 220px);
}

.right-col {
  padding-left: 16px;
  height: calc(100vh - 220px);
}

.student-list {
  flex: 1;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  margin-bottom: 12px;
}

.action-bar {
  flex-shrink: 0;
  padding-top: 4px;
  display: flex;
  flex-direction: column;
}

.action-btn {
  width: 100%;
  margin: 0 0 8px 0;
  box-sizing: border-box;
  margin-left: 0 !important;
  margin-right: 0 !important;
}

.action-btn:last-child {
  margin-bottom: 0;
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

.detail-wrapper {
  display: flex;
  gap: 16px;
  height: calc(100vh - 220px);
}

.detail-panel {
  flex: 1;
  position: relative;
  height: 100%;
  overflow-y: auto;
  padding-right: 0;
}

.detail-header {
  margin-bottom: 16px;
  margin-left: 0px;
  padding-left: 10px;
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

.progress-horizontal {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.progress-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  background-color: #f5f7fa;
  border-radius: 6px;
  min-width: 80px;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.progress-item.success {
  background-color: #f0f9eb;
  border-color: #e1f3d8;
}

.progress-item.info {
  background-color: #f4f4f5;
  border-color: #e9e9eb;
}

.progress-tag {
  font-size: 11px;
}

.content-section {
  margin-bottom: 0;
}

.content-title {
  margin: 0 0 10px 0;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}

.progress-name {
  font-size: 12px;
  color: #606266;
  text-align: center;
  line-height: 1.3;
}

.doc-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px 24px;
  margin-bottom: 16px;
  padding: 16px;
  background-color: #f9fafc;
  border-radius: 4px;
}

.doc-info-item {
  font-size: 14px;
  line-height: 1.6;
}

.doc-info-item.full-width {
  grid-column: 1 / -1;
}

.info-label {
  color: #606266;
  font-weight: 500;
}

.info-value {
  color: #303133;
}

.doc-content {
  margin-top: 0;
}

.content-text {
  padding: 18px;
  background-color: #f5f7fa;
  border-radius: 4px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 14px;
  color: #303133;
}

.empty-hint {
  padding: 32px;
  background-color: #f5f7fa;
  color: #909399;
  text-align: center;
  border-radius: 4px;
  font-size: 14px;
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

.comment-block {
  margin-top: 16px;
}

.meta-info {
  font-size: 13px;
  color: #606266;
  margin-bottom: 10px;
}

.comment-text {
  padding: 14px;
  background-color: #f9fafc;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  min-height: 70px;
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.8;
  font-size: 14px;
  color: #303133;
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  font-size: 14px;
  font-weight: 600;
}

.total-score-area {
  display: flex;
  gap: 100px;
  margin: 20px 0;
  padding: 24px;
  background: linear-gradient(135deg, #ecf5ff 0%, #f0f9eb 100%);
  border-radius: 8px;
}

.total-score-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.total-label {
  font-size: 16px;
  color: #606266;
  font-weight: 500;
}

.total-score {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
}

.grade-tag {
  display: inline-block;
  padding: 8px 24px;
  border-radius: 6px;
  font-size: 22px;
  font-weight: bold;
}

.grade-tag.优 {
  background-color: #f0f9eb;
  color: #67c23a;
}

.grade-tag.良 {
  background-color: #ecf5ff;
  color: #409eff;
}

.grade-tag.中 {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.grade-tag.及格 {
  background-color: #faecd8;
  color: #e6a23c;
}

.grade-tag.不及格 {
  background-color: #fef0f0;
  color: #f56c6c;
}

.note-text {
  margin-top: 14px;
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
}

.float-nav {
  flex-shrink: 0;
  align-self: flex-start;
  position: sticky;
  top: 0;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 8px 0;
  min-width: 100px;
}

.nav-item {
  padding: 10px 16px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
  border-left: 3px solid transparent;
}

.nav-item:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

.nav-item.active {
  background-color: #ecf5ff;
  color: #409eff;
  border-left-color: #409eff;
  font-weight: 500;
}

/* === 预览弹窗样式 === */
.preview-dialog :deep(.el-dialog__body) {
  padding: 0;
  max-height: 82vh;
  overflow-y: auto;
  background: #f0f2f5;
}

.preview-doc {
  max-width: 794px;
  margin: 0 auto;
  padding: 20px 0;
}

.preview-page {
  background: #fff;
  margin-bottom: 24px;
  padding: 48px 60px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, .1);
  border-radius: 2px;
  min-height: 600px;
}

.preview-cover {
  text-align: center;
}

.cover-spacer {
  height: 24px;
}

.cover-school {
  font-size: 22pt;
  font-weight: bold;
  margin: 10px 0;
  font-family: SimHei, '黑体', serif;
}

.cover-dept {
  font-size: 22pt;
  font-weight: bold;
  margin: 10px 0;
  font-family: STXingkai, '华文行楷', serif;
}

.cover-title {
  font-size: 36pt;
  font-weight: bold;
  margin: 32px 0;
}

.cover-fields {
  display: inline-block;
  text-align: left;
}

.cover-fields p {
  font-size: 16pt;
  margin: 12px 0;
}

.fill-line {
  display: inline-block;
  min-width: 120px;
  border-bottom: 1px solid #000;
  padding: 0 16px;
}

.cover-time {
  margin-top: 60px;
  font-size: 12pt;
}

.section-heading {
  font-size: 15pt;
  font-weight: bold;
  margin-bottom: 12px;
}

.preview-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 16px;
  font-size: 12pt;
}

.preview-table,
.preview-table th,
.preview-table td {
  border: 1px solid #333;
}

.preview-table th,
.preview-table td {
  padding: 6px 8px;
  vertical-align: top;
  line-height: 1.6;
}

.preview-table th {
  background: #f5f7fa;
  font-weight: bold;
  text-align: center;
}

.col-label {
  width: 140px;
}

.col-select {
  width: 80px;
  text-align: center;
}

.content-cell {
  white-space: pre-wrap;
  word-break: break-all;
}

.footer-note {
  font-size: 10.5pt;
  margin-top: 12px;
  line-height: 1.8;
  color: #303133;
}
</style>

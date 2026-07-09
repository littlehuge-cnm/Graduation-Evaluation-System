<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import { getTeacherStudents } from '@/api/teacher.js'
import { getStudentTeachers, getStudentDocuments } from '@/api/student.js'
import { getScoreRecordList } from '@/api/scoreRecord.js'
import { getStudentGroupList } from '@/api/studentGroup.js'
import { exportManual, exportManualBatch } from '@/api/manualExport.js'

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
const activeSection = ref('')
const batchDialogVisible = ref(false)
const selectedStudentNos = ref([])

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
  { id: 'section-defense-score', label: '答辩成绩' },
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
    return r?.comment || '暂无答辩记录'
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
  return !!recordMap.value[itemType]?.score || !!recordMap.value[itemType]?.comment
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
  ElMessage.info('预览功能预留，待实现')
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
    teachers.value = teacherRes || null
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

const progressItems = ['任务书', '指导书', '开题成绩', '外文翻译', '中期检查', '指导评语', '评阅评语', '答辩记录', '答辩成绩', '委员会评定']

onMounted(fetchStudents)
</script>

<template>
  <div>
    <el-page-header title="评价手册查看" />
    <el-card class="table-card">
      <el-row :gutter="16">
        <el-col :span="5" class="left-col">
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
          <div class="action-bar">
            <el-button type="primary" size="small" @click="handlePreview" :disabled="!selectedStudent"
              class="action-btn">预览手册Word格式</el-button>
            <el-button type="success" size="small" @click="handleExportSingle" :disabled="!selectedStudent"
              class="action-btn">导出手册Word格式</el-button>
            <el-button type="warning" size="small" @click="openBatchExportDialog"
              class="action-btn">批量导出评价手册</el-button>
          </div>
        </el-col>
        <el-col :span="19">
          <div v-if="selectedStudent" v-loading="loading" class="detail-panel" @scroll="handleScroll">
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
                <div v-for="item in progressItems" :key="item" class="progress-item" :class="getStatusType(item)">
                  <el-tag :type="getStatusType(item)" size="small" class="progress-tag">
                    {{ isCompleted(item) ? '已完成' : '未完成' }}
                  </el-tag>
                  <div class="progress-name">{{ item }}</div>
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
              <el-table :data="[{
                name: '得分',
                item1: getSubScoreDisplay('开题成绩', 0),
                item2: getSubScoreDisplay('开题成绩', 1),
                item3: getSubScoreDisplay('开题成绩', 2),
                total: getScore('开题成绩')
              }, {
                name: '满分',
                item1: 20,
                item2: 20,
                item3: 60,
                total: 100
              }]" border>
                <el-table-column prop="name" label="" width="80" />
                <el-table-column prop="item1" label="选题质量" />
                <el-table-column prop="item2" label="文献调研" />
                <el-table-column prop="item3" label="开题报告" />
                <el-table-column prop="total" label="总成绩" width="100" />
              </el-table>
              <div class="comment-block">
                <div class="meta-info"><strong>录入人：</strong>{{ getRecorderName('开题成绩') }} &nbsp;|&nbsp;
                  <strong>录入时间：</strong>{{ getRecordTime('开题成绩') }}
                </div>
                <div class="comment-text">{{ getComment('开题成绩') }}</div>
              </div>
            </div>

            <div id="section-translation" class="section">
              <h4>外文翻译成绩</h4>
              <el-table :data="[{
                name: '得分',
                item1: getSubScoreDisplay('外文翻译', 0),
                total: getScore('外文翻译')
              }, {
                name: '满分',
                item1: 100,
                total: 100
              }]" border>
                <el-table-column prop="name" label="" width="80" />
                <el-table-column prop="item1" label="翻译质量" />
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
              <el-table :data="[{
                name: '得分',
                item1: getSubScoreDisplay('中期检查', 0),
                item2: getSubScoreDisplay('中期检查', 1),
                item3: getSubScoreDisplay('中期检查', 2),
                total: getScore('中期检查')
              }, {
                name: '满分',
                item1: 40,
                item2: 40,
                item3: 20,
                total: 100
              }]" border>
                <el-table-column prop="name" label="" width="80" />
                <el-table-column prop="item1" label="进度完成" />
                <el-table-column prop="item2" label="工作质量" />
                <el-table-column prop="item3" label="工作态度" />
                <el-table-column prop="total" label="总成绩" width="100" />
              </el-table>
              <div class="comment-block">
                <div class="meta-info"><strong>录入人：</strong>{{ getRecorderName('中期检查') }} &nbsp;|&nbsp;
                  <strong>录入时间：</strong>{{ getRecordTime('中期检查') }}
                </div>
                <div class="comment-text">{{ getComment('中期检查') }}</div>
              </div>
            </div>

            <div id="section-supervisor" class="section">
              <h4>指导老师评语及建议成绩</h4>
              <el-table :data="[{
                name: '得分',
                item1: getSubScoreDisplay('指导评语', 0),
                item2: getSubScoreDisplay('指导评语', 1),
                item3: getSubScoreDisplay('指导评语', 2),
                total: getScore('指导评语')
              }, {
                name: '满分',
                item1: 20,
                item2: 50,
                item3: 30,
                total: 100
              }]" border>
                <el-table-column prop="name" label="" width="80" />
                <el-table-column prop="item1" label="学习态度" />
                <el-table-column prop="item2" label="能力水平" />
                <el-table-column prop="item3" label="完成质量" />
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
                total: getScore('评阅评语')
              }, {
                name: '满分',
                item1: 15,
                item2: 45,
                item3: 40,
                total: 100
              }]" border>
                <el-table-column prop="name" label="" width="80" />
                <el-table-column prop="item1" label="选题质量" />
                <el-table-column prop="item2" label="能力水平" />
                <el-table-column prop="item3" label="成果质量" />
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
              <h4>答辩小组评定成绩</h4>
              <el-table :data="[{
                name: '得分',
                item1: getSubScoreDisplay('答辩成绩', 0),
                item2: getSubScoreDisplay('答辩成绩', 1),
                item3: getSubScoreDisplay('答辩成绩', 2),
                total: getScore('答辩成绩')
              }, {
                name: '满分',
                item1: 35,
                item2: 35,
                item3: 30,
                total: 100
              }]" border>
                <el-table-column prop="name" label="" width="80" />
                <el-table-column prop="item1" label="报告内容" />
                <el-table-column prop="item2" label="答辩过程" />
                <el-table-column prop="item3" label="答辩小组" />
                <el-table-column prop="total" label="答辩成绩" width="100" />
              </el-table>
              <div class="comment-block">
                <div class="meta-info"><strong>录入人：</strong>{{ getRecorderName('答辩成绩') }} &nbsp;|&nbsp;
                  <strong>录入时间：</strong>{{ getRecordTime('答辩成绩') }}
                </div>
                <div class="comment-text">{{ getComment('答辩成绩') }}</div>
              </div>
            </div>

            <div id="section-final" class="section">
              <h4>答辩委员会评语及总评成绩</h4>
              <el-table :data="[
                { item: '开题报告', weight: '12%', score: getScore('开题成绩') },
                { item: '英文翻译', weight: '3%', score: getScore('外文翻译') },
                { item: '中期检查', weight: '15%', score: getScore('中期检查') },
                { item: '指导教师', weight: '15%', score: getScore('指导评语') },
                { item: '评阅教师', weight: '15%', score: getScore('评阅评语') },
                { item: '答辩', weight: '40%', score: getScore('答辩成绩') }
              ]" border>
                <el-table-column prop="item" label="成绩评定项" width="180" />
                <el-table-column prop="weight" label="权重" width="100" />
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
          <div v-else class="empty-select">
            <span>请选择左侧学生查看详情</span>
          </div>

          <div v-if="selectedStudent" class="float-nav">
            <div v-for="item in navItems" :key="item.id" class="nav-item" :class="{ active: activeSection === item.id }"
              @click="scrollToSection(item.id)">
              {{ item.label }}
            </div>
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

.detail-panel {
  position: relative;
  height: calc(100vh - 220px);
  overflow-y: auto;
  padding-right: 120px;
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
  position: fixed;
  right: 65px;
  top: 180px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
 /* box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);*/
  padding: 8px 0;
  z-index: 100;
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
</style>

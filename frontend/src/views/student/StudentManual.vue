<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user.js'
import { getStudentById, getStudentTeachers, getStudentDocuments } from '@/api/student.js'
import { getScoreRecordList } from '@/api/scoreRecord.js'
import { getStudentGroupList } from '@/api/studentGroup.js'
import { getGroupMappingList } from '@/api/groupMapping.js'
import { getTeacherGroupById } from '@/api/teacherGroup.js'

const userStore = useUserStore()
const loading = ref(false)
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

const studentInfo = ref(null)
const studentGroups = ref([])
const records = ref([])
const teachers = ref(null)
const documents = ref([])
const groupMappings = ref([])
const teacherGroups = ref({})
const activeSection = ref('')

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
  if (!studentInfo.value?.studentGroupId) return null
  const mapping = groupMappings.value.find(m =>
    m.studentGroupId === studentInfo.value.studentGroupId &&
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

async function fetchData() {
  loading.value = true
  try {
    const studentNo = userStore.username
    const [infoRes, groupsRes, recordRes, teacherRes, docRes] = await Promise.all([
      getStudentById(studentNo),
      getStudentGroupList(),
      getScoreRecordList(studentNo),
      getStudentTeachers(studentNo),
      getStudentDocuments(studentNo)
    ])

    studentInfo.value = infoRes
    studentGroups.value = groupsRes || []
    records.value = recordRes || []
    teachers.value = teacherRes || { supervisor: null, reviewer: null }
    documents.value = docRes || []

    await fetchGroupMappings()

    nextTick(() => {
      activeSection.value = 'section-progress'
    })
  } catch (error) {
    ElMessage.error(error.message || '获取评价记录失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <div v-loading="loading">
    <el-page-header title="评价手册查看" :icon="null" />
    <el-card class="table-card">
      <div class="detail-wrapper">
        <div class="detail-panel" @scroll="handleScroll">
          <div class="detail-header">
            <h3>{{ studentInfo?.studentName }}（{{ studentInfo?.studentNo }}）</h3>
            <div class="header-info">
              <span>专业：{{ studentInfo?.major || '-' }}</span>
              <span>班级：{{ studentInfo?.className || '-' }}</span>
              <span>年级：{{ studentInfo?.grade || '-' }}</span>
            </div>
            <div class="header-info">
              <span>学生组：{{ groupNameMap[studentInfo?.studentGroupId] || '未分组' }}</span>
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
            <div class="meta-info" style="margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px dashed #ebeef5;">
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
            <div class="meta-info" style="margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px dashed #ebeef5;">
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
            <div class="meta-info" style="margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px dashed #ebeef5;">
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
          <div v-for="item in navItems" :key="item.id" class="nav-item" :class="{ active: activeSection === item.id }"
            @click="scrollToSection(item.id)">
            {{ item.label }}
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.table-card {
  margin-top: 16px;
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

.grade-tag.未评定 {
  background-color: #f4f4f5;
  color: #909399;
}

.note-text {
  margin-top: 16px;
  padding: 12px;
  background-color: #fdf6ec;
  border-radius: 4px;
  font-size: 13px;
  color: #e6a23c;
  line-height: 1.6;
}

.float-nav {
  width: 120px;
  flex-shrink: 0;
  background: #f5f7fa;
  border-radius: 4px;
  padding: 8px 0;
  height: fit-content;
  position: sticky;
  top: 0;
}

.nav-item {
  padding: 8px 12px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}

.nav-item:hover {
  background: #ecf5ff;
  color: #409eff;
}

.nav-item.active {
  background: #409eff;
  color: white;
  font-weight: 500;
}
</style>

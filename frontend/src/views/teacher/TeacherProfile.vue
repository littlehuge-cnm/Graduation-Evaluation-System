<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import { getTeacherById, getTeacherStudents } from '@/api/teacher.js'
import { getTeacherGroupList } from '@/api/teacherGroup.js'
import { getStudentGroupList, getStudentGroupById } from '@/api/studentGroup.js'
import { getGroupMappingList } from '@/api/groupMapping.js'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const teacherInfo = ref(null)
const teacherGroups = ref([])
const studentGroups = ref([])
const mappings = ref([])
const myTeacherGroupIds = ref([])
const studentGroupDetails = ref({})
const superviseStudents = ref([])
const reviewStudents = ref([])
const superviseKeyword = ref('')
const reviewKeyword = ref('')
const stageKeyword = ref('')
const activeStage = ref('开题')

const myGroups = computed(() => {
  if (!teacherInfo.value) return []
  return teacherGroups.value.filter(g =>
    g.leaderNo === userStore.username ||
    g.secretaryNo === userStore.username ||
    (g.memberNo && g.memberNo.split(',').includes(userStore.username))
  ).map(g => ({
    ...g,
    myRole: g.leaderNo === userStore.username ? '组长' :
      g.secretaryNo === userStore.username ? '秘书' : '普通成员'
  }))
})

const filteredSuperviseStudents = computed(() => {
  if (!superviseKeyword.value) return superviseStudents.value
  const k = superviseKeyword.value.trim().toLowerCase()
  return superviseStudents.value.filter(s =>
    s.studentNo?.toLowerCase().includes(k) || s.studentName?.toLowerCase().includes(k)
  )
})

const filteredReviewStudents = computed(() => {
  if (!reviewKeyword.value) return reviewStudents.value
  const k = reviewKeyword.value.trim().toLowerCase()
  return reviewStudents.value.filter(s =>
    s.studentNo?.toLowerCase().includes(k) || s.studentName?.toLowerCase().includes(k)
  )
})

function getStageGroups(stage) {
  const stageMappings = mappings.value.filter(m =>
    m.stage === stage && myTeacherGroupIds.value.includes(m.teacherGroupId)
  )
  return stageMappings.map(m => {
    const studentGroup = studentGroups.value.find(g => g.groupId === m.studentGroupId)
    return studentGroup || {
      groupId: m.studentGroupId,
      groupName: m.studentGroupName || `学生组${m.studentGroupId}`
    }
  }).filter(Boolean)
}

function getGroupStudents(group) {
  if (!group) return []
  const detail = studentGroupDetails.value[group.groupId]
  if (!detail || !detail.students) return []
  return detail.students
}

function filterGroupStudents(group, keyword) {
  const students = getGroupStudents(group)
  if (!keyword) return students
  const k = keyword.trim().toLowerCase()
  return students.filter(s =>
    s.studentNo?.toLowerCase().includes(k) || s.studentName?.toLowerCase().includes(k)
  )
}

function getAllStageStudents(stage, keyword) {
  const groups = getStageGroups(stage)
  const result = []
  const k = keyword ? keyword.trim().toLowerCase() : ''
  groups.forEach(group => {
    const groupNameMatch = !k || group.groupName?.toLowerCase().includes(k)
    const students = getGroupStudents(group)
    students.forEach(student => {
      const studentMatch = !k ||
        student.studentNo?.toLowerCase().includes(k) ||
        student.studentName?.toLowerCase().includes(k)
      if (groupNameMatch || studentMatch) {
        result.push({
          ...student,
          groupName: group.groupName,
          groupId: group.groupId
        })
      }
    })
  })
  return result
}

const openingGroups = computed(() => getStageGroups('开题'))
const midtermGroups = computed(() => getStageGroups('中期'))
const defenseGroups = computed(() => getStageGroups('答辩'))

const currentStageStudents = computed(() => getAllStageStudents(activeStage.value, stageKeyword.value))

const allStageGroups = computed(() => [
  { stage: '开题', label: '开题报告', groups: openingGroups.value },
  { stage: '中期', label: '中期检查', groups: midtermGroups.value },
  { stage: '答辩', label: '毕业答辩', groups: defenseGroups.value }
])

const hasAnyStageGroups = computed(() => openingGroups.value.length > 0 || midtermGroups.value.length > 0 || defenseGroups.value.length > 0)

async function loadGroupDetails(groupIds) {
  const details = {}
  await Promise.all(groupIds.map(async (groupId) => {
    try {
      const detail = await getStudentGroupById(groupId)
      details[groupId] = detail
    } catch (e) {
      console.error(`获取学生组${groupId}详情失败`, e)
    }
  }))
  return details
}

async function loadData() {
  loading.value = true
  try {
    const teacherNo = userStore.username
    const [infoRes, groupsRes, mappingsRes, studentGroupsRes, superviseRes, reviewRes] = await Promise.all([
      getTeacherById(teacherNo),
      getTeacherGroupList(),
      getGroupMappingList(),
      getStudentGroupList(),
      getTeacherStudents(teacherNo, '指导'),
      getTeacherStudents(teacherNo, '评阅')
    ])
    teacherInfo.value = infoRes
    teacherGroups.value = groupsRes || []
    mappings.value = mappingsRes || []
    studentGroups.value = studentGroupsRes || []
    superviseStudents.value = superviseRes || []
    reviewStudents.value = reviewRes || []

    const myTeacherGroups = teacherGroups.value.filter(g =>
      g.leaderNo === teacherNo ||
      g.secretaryNo === teacherNo ||
      (g.memberNo && g.memberNo.split(',').includes(teacherNo))
    )
    myTeacherGroupIds.value = myTeacherGroups.map(g => g.groupId)

    const stages = ['开题', '中期', '答辩']
    const relevantMappings = mappings.value.filter(m =>
      stages.includes(m.stage) && myTeacherGroupIds.value.includes(m.teacherGroupId)
    )
    const uniqueGroupIds = [...new Set(relevantMappings.map(m => m.studentGroupId))]
    studentGroupDetails.value = await loadGroupDetails(uniqueGroupIds)
  } catch (error) {
    ElMessage.error(error.message || '加载信息失败')
  } finally {
    loading.value = false
  }
}

function goToManualReview(student) {
  router.push({ path: '/teacher/supervise/manual-review', query: { studentNo: student.studentNo } })
}

function goToDocuments(student) {
  router.push({ path: '/teacher/supervise/documents', query: { studentNo: student.studentNo } })
}

function goToSuperviseComment(student) {
  router.push({ path: '/teacher/supervise/comment', query: { studentNo: student.studentNo } })
}

function goToTranslation(student) {
  router.push({ path: '/teacher/supervise/translation', query: { studentNo: student.studentNo } })
}

function goToReviewComment(student) {
  router.push({ path: '/teacher/review/comment', query: { studentNo: student.studentNo } })
}

function goToOpening(groupId, studentNo) {
  router.push({ path: '/teacher/group/opening', query: { groupId, studentNo } })
}

function goToMidterm(groupId, studentNo) {
  router.push({ path: '/teacher/group/midterm', query: { groupId, studentNo } })
}

function goToDefense(groupId, studentNo) {
  router.push({ path: '/teacher/group/defense', query: { groupId, studentNo } })
}

function goToStageEvaluation(stage, groupId, studentNo) {
  switch (stage) {
    case '开题':
      goToOpening(groupId, studentNo)
      break
    case '中期':
      goToMidterm(groupId, studentNo)
      break
    case '答辩':
      goToDefense(groupId, studentNo)
      break
  }
}

function getStageColumnLabel(stage) {
  switch (stage) {
    case '开题':
    case '答辩':
      return '答辩分组'
    case '中期':
      return '检查分组'
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div v-loading="loading">
    <el-page-header title="个人信息" />

    <el-card class="info-card">
      <template #header>
        <span>基本信息</span>
      </template>
      <el-descriptions :column="3" border v-if="teacherInfo">
        <el-descriptions-item label="姓名">{{ teacherInfo.teacherName }}</el-descriptions-item>
        <el-descriptions-item label="工号">{{ teacherInfo.teacherNo }}</el-descriptions-item>
        <el-descriptions-item label="职称">{{ teacherInfo.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所在院系" :span="2">{{ teacherInfo.department || '-' }}</el-descriptions-item>
        <el-descriptions-item label="我的身份" :span="3">
          <el-tag v-for="tag in userStore.identities" :key="tag"
            :type="tag === '组长' ? 'danger' : tag === '秘书' ? 'warning' : tag === '指导教师' ? 'success' : tag === '评阅教师' ? 'primary' : 'info'"
            style="margin-right: 8px;" effect="dark">
            {{ tag }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="info-card" v-if="myGroups.length > 0">
      <template #header>
        <span>所在教师分组</span>
      </template>
      <el-table :data="myGroups" border size="small">
        <el-table-column prop="groupName" label="组名" width="180" />
        <el-table-column prop="myRole" label="我的角色" width="120">
          <template #default="{ row }">
            <el-tag :type="row.myRole === '组长' ? 'danger' : row.myRole === '秘书' ? 'warning' : 'info'" size="small">
              {{ row.myRole }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="组成员">
          <template #default="{ row }">
            <span>{{ row.leaderName }}（组长）、{{ row.secretaryName }}（秘书）、{{ row.memberName || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="info-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>我指导的学生（{{ superviseStudents.length }}人）</span>
          <el-input v-model="superviseKeyword" placeholder="搜索学号或姓名" clearable style="width: 240px;" size="small">
            <template #prefix>
              <el-icon>
                <Search />
              </el-icon>
            </template>
          </el-input>
        </div>
      </template>
      <el-table :data="filteredSuperviseStudents" border size="small" v-if="superviseStudents.length > 0">
        <el-table-column prop="studentNo" label="学号" width="150" />
        <el-table-column prop="studentName" label="姓名" width="120" />
        <el-table-column prop="className" label="班级" width="150" />
        <el-table-column prop="major" label="专业" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="goToManualReview(row)">查看手册</el-button>
            <el-button type="info" link size="small" @click="goToDocuments(row)">任务书/指导书</el-button>
            <el-button type="warning" link size="small" @click="goToTranslation(row)">翻译评定</el-button>
            <el-button type="success" link size="small" @click="goToSuperviseComment(row)">指导评定</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无指导学生" :image-size="80" />
    </el-card>

    <el-card class="info-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>我评阅的学生（{{ reviewStudents.length }}人）</span>
          <el-input v-model="reviewKeyword" placeholder="搜索学号或姓名" clearable style="width: 240px;" size="small">
            <template #prefix>
              <el-icon>
                <Search />
              </el-icon>
            </template>
          </el-input>
        </div>
      </template>
      <el-table :data="filteredReviewStudents" border size="small" v-if="reviewStudents.length > 0">
        <el-table-column prop="studentNo" label="学号" width="150" />
        <el-table-column prop="studentName" label="姓名" width="120" />
        <el-table-column prop="className" label="班级" width="150" />
        <el-table-column prop="major" label="专业" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="goToReviewComment(row)">评阅评定</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无评阅学生" :image-size="80" />
    </el-card>

    <el-card class="info-card" v-if="hasAnyStageGroups">
      <div class="stage-tabs-wrapper">
        <el-tabs v-model="activeStage" class="stage-tabs">
          <el-tab-pane v-for="item in allStageGroups" :key="item.stage"
            :label="`${item.label} (${getAllStageStudents(item.stage, stageKeyword).length}人)`" :name="item.stage">
          </el-tab-pane>
        </el-tabs>
        <el-input v-model="stageKeyword" placeholder="搜索组名/学号/姓名" clearable style="width: 240px;" size="small"
          class="stage-search">
          <template #prefix>
            <el-icon>
              <Search />
            </el-icon>
          </template>
        </el-input>
      </div>
      <el-table :data="currentStageStudents" border size="small" style="width: 100%;">
        <el-table-column prop="groupName" :label="getStageColumnLabel(activeStage)" min-width="180" />
        <el-table-column prop="studentNo" label="学号" width="150" />
        <el-table-column prop="studentName" label="姓名" width="120" />
        <el-table-column prop="className" label="班级" min-width="150" />
        <el-table-column prop="major" label="专业" min-width="150" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small"
              @click="goToStageEvaluation(activeStage, row.groupId, row.studentNo)">
              {{allStageGroups.find(g => g.stage === activeStage)?.label?.replace('分组', '')}}评定
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.info-card {
  margin-top: 16px;
}

.stage-tabs-wrapper {
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--el-border-color-light);
}

.stage-tabs {
  flex: 1;
  margin-bottom: -1px;
}

.stage-search {
  margin-left: 16px;
  margin-bottom: 8px;
}

:deep(.stage-tabs .el-tabs__header) {
  margin: 0;
}
</style>

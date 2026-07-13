<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user.js'
import { getStudentById, getStudentTeachers } from '@/api/student.js'
import { getStudentGroupById } from '@/api/studentGroup.js'
import { getGroupMappingList } from '@/api/groupMapping.js'
import { getTeacherGroupById } from '@/api/teacherGroup.js'

const userStore = useUserStore()
const loading = ref(false)
const studentInfo = ref(null)
const teachers = ref(null)
const studentGroup = ref(null)
const groupMappings = ref([])
const teacherGroups = ref({})

const stages = ['开题', '中期', '答辩']

const stageGroupNames = computed(() => {
  const result = {}
  stages.forEach(stage => {
    result[stage] = getStageTeacherGroup(stage)
  })
  return result
})

const groupMembersText = computed(() => {
  if (!studentGroup.value?.students || studentGroup.value.students.length === 0) return '-'
  return studentGroup.value.students.map(m => m.studentName).join('、')
})

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
  const names = []
  if (group.leaderName) names.push(`${group.leaderName}（组长）`)
  if (group.secretaryName) names.push(`${group.secretaryName}（秘书）`)
  if (group.memberName) names.push(`${group.memberName}（组员）`)
  return names.length > 0 ? names.join('、') : '-'
}

async function fetchData() {
  loading.value = true
  try {
    const studentNo = userStore.username
    const [infoRes, teacherRes, mappingsRes] = await Promise.all([
      getStudentById(studentNo),
      getStudentTeachers(studentNo),
      getGroupMappingList()
    ])

    studentInfo.value = infoRes
    teachers.value = teacherRes
    groupMappings.value = mappingsRes || []

    if (infoRes?.studentGroupId) {
      const groupDetail = await getStudentGroupById(infoRes.studentGroupId)
      studentGroup.value = groupDetail
    }

    const groupIds = [...new Set((mappingsRes || []).map(m => m.teacherGroupId))]
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
    console.error('获取信息失败', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <div v-loading="loading">
    <el-page-header title="个人信息" :icon="null" />

    <el-card class="info-card" style="margin-top: 16px;">
      <template #header>
        <span>基本信息</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="姓名">{{ studentInfo?.studentName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ studentInfo?.studentNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ studentInfo?.major || '-' }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ studentInfo?.className || '-' }}</el-descriptions-item>
        <el-descriptions-item label="年级">{{ studentInfo?.grade || '-' }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ studentInfo?.gender || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="info-card">
      <template #header>
        <span>指导教师与评阅教师</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="指导教师">{{ teachers?.supervisor?.teacherName || '-' }}
          <span v-if="teachers?.supervisor?.teacherNo" class="sub-info">{{ teachers.supervisor.teacherNo }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="评阅教师">{{ teachers?.reviewer?.teacherName || '-' }}
          <span v-if="teachers?.reviewer?.teacherNo" class="sub-info">{{ teachers.reviewer.teacherNo }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="info-card">
      <template #header>
        <span>学生组信息</span>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="所属学生组">{{ studentGroup?.groupName || '未分组' }}</el-descriptions-item>
        <el-descriptions-item label="组员">{{ groupMembersText }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="info-card">
      <template #header>
        <span>各环节评定教师组</span>
      </template>
      <el-table :data="stages.map(stage => ({
        stage,
        group: getStageTeacherGroup(stage),
        members: getStageMembersText(stage)
      }))" border>
        <el-table-column prop="stage" label="环节名称" width="120" />
        <el-table-column label="评定教师组">
          <template #default="{ row }">
            {{ row.group?.groupName || '未分配' }}
          </template>
        </el-table-column>
        <el-table-column label="组员" min-width="200">
          <template #default="{ row }">
            {{ row.members }}
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

.sub-info {
  color: #909399;
  font-size: 13px;
  margin-left: 8px;
}
</style>
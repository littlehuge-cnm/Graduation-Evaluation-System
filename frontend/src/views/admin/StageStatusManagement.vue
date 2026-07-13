<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getStudentGroupList } from '@/api/studentGroup.js'
import { getStageOverview, startStageBatch } from '@/api/stageStatus.js'

const loading = ref(false)
const groups = ref([])
const activeStage = ref('开题')
const overview = ref(null)

const stageOptions = ['开题', '中期', '答辩']
const statusTypeMap = {
  '未开始': 'info',
  '进行中': 'warning',
  '已完成': 'success'
}

async function fetchGroups() {
  const res = await getStudentGroupList({ page: 1, pageSize: 1000 })
  groups.value = res.records || []
}

async function fetchOverview() {
  if (!activeStage.value) return
  loading.value = true
  try {
    const results = []
    for (const group of groups.value) {
      const res = await getStageOverview(activeStage.value, group.id)
      results.push({ groupId: group.id, groupName: group.groupName, ...res })
    }
    overview.value = results
  } catch (error) {
    ElMessage.error(error.message || '查询失败')
  } finally {
    loading.value = false
  }
}

async function handleStartBatch(groupId) {
  try {
    await startStageBatch({ stage: activeStage.value, studentGroupId: groupId })
    ElMessage.success('启动成功')
    fetchOverview()
  } catch (error) {
    ElMessage.error(error.message || '启动失败')
  }
}

function getStatusType(statusDesc) {
  return statusTypeMap[statusDesc] || 'info'
}

onMounted(async () => {
  await fetchGroups()
  await fetchOverview()
})
</script>

<template>
  <div>
    <el-page-header title="环节状态管理" :icon="null" />
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <el-radio-group v-model="activeStage" size="large" @change="fetchOverview">
            <el-radio-button v-for="stage in stageOptions" :key="stage" :label="stage">{{ stage }}</el-radio-button>
          </el-radio-group>
          <el-button type="primary" :loading="loading" @click="fetchOverview">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="overview" border>
        <el-table-column prop="groupName" label="学生组" min-width="180" />
        <el-table-column label="状态统计" min-width="240">
          <template #default="{ row }">
            <div v-if="row.statistics" class="statistics">
              <el-tag v-for="(count, status) in row.statistics" :key="status" :type="getStatusType(status)" class="stat-tag">
                {{ status }}: {{ count }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="明细" min-width="300">
          <template #default="{ row }">
            <div v-if="row.list" class="student-list">
              <el-tag
                v-for="item in row.list"
                :key="item.studentNo"
                :type="getStatusType(item.statusDesc)"
                size="small"
                class="student-tag"
              >
                {{ item.studentName }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleStartBatch(row.groupId)">批量启动</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.table-card {
  margin-top: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.statistics {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.stat-tag {
  margin-right: 0;
}

.student-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.student-tag {
  margin-right: 0;
}
</style>

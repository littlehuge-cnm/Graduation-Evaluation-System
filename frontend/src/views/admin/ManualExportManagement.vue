<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getStudentGroupList } from '@/api/studentGroup.js'
import { getStudentList } from '@/api/student.js'
import { exportManualBatch, exportManualByGroup } from '@/api/manualExport.js'

const activeTab = ref('batch')
const loading = ref(false)
const groups = ref([])
const students = ref([])
const selectedStudents = ref([])
const selectedGroupId = ref(null)

async function fetchGroups() {
  const res = await getStudentGroupList({ page: 1, pageSize: 1000 })
  groups.value = res.records || []
}

async function fetchStudents() {
  const res = await getStudentList({ page: 1, pageSize: 1000 })
  students.value = res.records || []
}

async function handleBatchExport() {
  if (selectedStudents.value.length === 0) {
    ElMessage.warning('请至少选择一名学生')
    return
  }
  loading.value = true
  try {
    const blob = await exportManualBatch(selectedStudents.value)
    downloadBlob(blob, '评价手册批量导出.zip')
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error(error.message || '导出失败')
  } finally {
    loading.value = false
  }
}

async function handleGroupExport() {
  if (!selectedGroupId.value) {
    ElMessage.warning('请选择学生组')
    return
  }
  loading.value = true
  try {
    const blob = await exportManualByGroup(selectedGroupId.value)
    const group = groups.value.find(g => g.id === selectedGroupId.value)
    downloadBlob(blob, `${group?.groupName || '学生组'}评价手册.zip`)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error(error.message || '导出失败')
  } finally {
    loading.value = false
  }
}

function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

onMounted(() => {
  fetchGroups()
  fetchStudents()
})
</script>

<template>
  <div>
    <el-page-header title="评价手册导出" :icon="null" />
    <el-card class="table-card">
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="批量导出" name="batch">
          <div class="tab-content">
            <p class="hint">请选择需要批量导出的学生：</p>
            <el-transfer
              v-model="selectedStudents"
              :data="students.map(s => ({ key: s.studentNo, label: `${s.studentNo} ${s.studentName}` }))"
              :titles="['学生列表', '已选学生']"
              filterable
            />
            <div class="actions">
              <el-button type="primary" :loading="loading" @click="handleBatchExport">导出 ZIP</el-button>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="按组导出" name="group">
          <div class="tab-content">
            <p class="hint">请选择需要导出的学生组：</p>
            <el-select v-model="selectedGroupId" placeholder="请选择学生组" style="width: 300px;">
              <el-option
                v-for="group in groups"
                :key="group.id"
                :label="group.groupName"
                :value="group.id"
              />
            </el-select>
            <div class="actions">
              <el-button type="primary" :loading="loading" @click="handleGroupExport">导出 ZIP</el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.table-card {
  margin-top: 16px;
}

.tab-content {
  padding: 16px 0;
}

.hint {
  margin-bottom: 12px;
  color: #606266;
}

.actions {
  margin-top: 16px;
}
</style>

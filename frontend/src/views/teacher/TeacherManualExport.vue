<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user.js'
import { exportManual } from '@/api/manualExport.js'
import { getScoreRecordTodo } from '@/api/scoreRecord.js'

const userStore = useUserStore()
const loading = ref(false)
const students = ref([])

async function fetchData() {
  loading.value = true
  try {
    const res = await getScoreRecordTodo(userStore.username, 'teacher')
    const studentNos = [...new Set(res.map(item => item.studentNo))]
    students.value = studentNos.map(no => {
      const item = res.find(r => r.studentNo === no)
      return { studentNo: no, studentName: item.studentName }
    })
  } finally {
    loading.value = false
  }
}

async function handleExport(studentNo, studentName) {
  try {
    const blob = await exportManual(studentNo)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${studentName}.docx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error(error.message || '导出失败')
  }
}

onMounted(fetchData)
</script>

<template>
  <div>
    <el-page-header title="评价手册导出" :icon="null" />
    <el-card class="table-card">
      <template #header>
        <span>可导出的学生列表</span>
      </template>

      <el-table v-loading="loading" :data="students" border>
        <el-table-column prop="studentNo" label="学号" min-width="150" />
        <el-table-column prop="studentName" label="姓名" min-width="150" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleExport(row.studentNo, row.studentName)">导出</el-button>
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
</style>

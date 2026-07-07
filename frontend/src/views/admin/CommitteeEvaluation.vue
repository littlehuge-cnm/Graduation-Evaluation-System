<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user.js'
import { getScoreRecordTodo, addScoreRecord, updateScoreRecord, confirmScoreRecord, unlockScoreRecord } from '@/api/scoreRecord.js'
import { getStudentList } from '@/api/student.js'

const userStore = useUserStore()
const loading = ref(false)
const tableData = ref([])
const allStudents = ref([])
const keyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  studentNo: '',
  score: null,
  grade: '',
  comment: '',
  recordStatus: 1
})

const rules = {
  studentNo: [{ required: true, message: '请选择学生', trigger: 'change' }],
  score: [{ required: true, message: '请输入加权总分', trigger: 'blur' }],
  grade: [{ required: true, message: '请选择评定等级', trigger: 'change' }],
  comment: [{ required: true, message: '请输入评语', trigger: 'blur' }]
}

const gradeOptions = ['优', '良', '中', '及格', '不及格']
const statusOptions = [
  { label: '暂存', value: 1 },
  { label: '已确认', value: 2 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getScoreRecordTodo(userStore.username, 'admin')
    tableData.value = res || []
  } finally {
    loading.value = false
  }
}

async function fetchStudents() {
  try {
    const res = await getStudentList({ pageNum: 1, pageSize: 1000 })
    allStudents.value = res.list || []
  } catch (error) {
    console.error(error)
  }
}

const filteredData = computed(() => {
  if (!keyword.value) return tableData.value
  const k = keyword.value.trim()
  return tableData.value.filter(item =>
    item.studentNo?.includes(k) || item.studentName?.includes(k)
  )
})

function handleEvaluate(row) {
  isEdit.value = !!row.recordId
  dialogTitle.value = isEdit.value ? '编辑委员会评定' : '委员会评定'
  Object.assign(form, {
    id: row.recordId || null,
    studentNo: row.studentNo,
    score: row.score || null,
    grade: row.grade || '',
    comment: row.comment || '',
    recordStatus: row.recordStatus || 1
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    const data = {
      studentNo: form.studentNo,
      itemType: '委员会评定',
      score: form.score,
      grade: form.grade,
      comment: form.comment,
      recordStatus: form.recordStatus
    }

    if (isEdit.value) {
      await updateScoreRecord(form.id, data)
      ElMessage.success('修改成功')
    } else {
      await addScoreRecord(data, userStore.username)
      ElMessage.success('录入成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

async function handleConfirm(row) {
  try {
    await ElMessageBox.confirm('确认后将不能修改，是否继续？', '提示', { type: 'warning' })
    await confirmScoreRecord(row.recordId)
    ElMessage.success('确认成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '确认失败')
    }
  }
}

async function handleUnlock(row) {
  try {
    await unlockScoreRecord(row.recordId)
    ElMessage.success('解锁成功')
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '解锁失败')
  }
}

function getStatusType(status) {
  return status === 2 ? 'success' : 'info'
}

function getStatusLabel(status) {
  const item = statusOptions.find(i => i.value === status)
  return item ? item.label : '未知'
}

onMounted(() => {
  fetchData()
  fetchStudents()
})
</script>

<template>
  <div>
    <el-page-header title="委员会评定" />
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <div class="search-bar">
            <el-input v-model="keyword" placeholder="按学号/姓名搜索" clearable style="width: 220px;" />
          </div>
          <el-button type="primary" @click="handleEvaluate({})">新增评定</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="filteredData" border>
        <el-table-column prop="studentNo" label="学号" min-width="120" />
        <el-table-column prop="studentName" label="姓名" min-width="120" />
        <el-table-column prop="score" label="加权总分" width="100" />
        <el-table-column prop="grade" label="评定等级" width="100" />
        <el-table-column prop="comment" label="评语" min-width="200" show-overflow-tooltip />
        <el-table-column prop="recordStatusDesc" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.recordStatus)">{{ row.recordStatusDesc || getStatusLabel(row.recordStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEvaluate(row)">评定</el-button>
            <el-button v-if="row.recordStatus === 1" type="success" link @click="handleConfirm(row)">确认</el-button>
            <el-button v-if="row.recordStatus === 2" type="warning" link @click="handleUnlock(row)">解锁</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="学生" prop="studentNo">
          <el-select v-model="form.studentNo" placeholder="请选择学生" style="width: 100%;" :disabled="isEdit">
            <el-option
              v-for="student in allStudents"
              :key="student.studentNo"
              :label="`${student.studentNo} - ${student.studentName}`"
              :value="student.studentNo"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="加权总分" prop="score">
          <el-input-number v-model="form.score" :min="0" :max="100" :precision="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="评定等级" prop="grade">
          <el-select v-model="form.grade" placeholder="请选择等级" style="width: 100%;">
            <el-option v-for="g in gradeOptions" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="评语" prop="comment">
          <el-input v-model="form.comment" type="textarea" :rows="4" placeholder="请输入委员会评语" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.recordStatus">
            <el-radio :label="1">暂存</el-radio>
            <el-radio :label="2">已确认</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
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
  flex-wrap: wrap;
  gap: 12px;
}

.search-bar {
  display: flex;
  gap: 8px;
}
</style>

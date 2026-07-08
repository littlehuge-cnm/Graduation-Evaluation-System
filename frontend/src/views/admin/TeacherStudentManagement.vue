<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTeacherStudentList, batchAssignTeacherStudent } from '@/api/teacherStudent.js'
import { getTeacherList } from '@/api/teacher.js'
import { getStudentList } from '@/api/student.js'

const loading = ref(false)
const students = ref([])
const relations = ref([])
const teacherOptions = ref([])
const keyword = ref('')
const selectedStudents = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogMode = ref('single')
const currentStudent = ref(null)
const formRef = ref(null)
const form = reactive({
  guideTeacherNo: '',
  reviewTeacherNo: ''
})

const rules = {
  guideTeacherNo: [{ required: false }],
  reviewTeacherNo: [{ required: false }]
}

const tableData = computed(() => {
  const k = keyword.value.trim()
  const list = students.value.map(s => {
    const guide = relations.value.find(r => r.studentNo === s.studentNo && r.relationType === '指导' && r.relationStatus === 1)
    const review = relations.value.find(r => r.studentNo === s.studentNo && r.relationType === '评阅' && r.relationStatus === 1)
    return {
      ...s,
      guideTeacherNo: guide?.teacherNo || '',
      guideTeacherName: guide?.teacherName || '未分配',
      reviewTeacherNo: review?.teacherNo || '',
      reviewTeacherName: review?.teacherName || '未分配'
    }
  })
  if (!k) return list
  return list.filter(s => s.studentNo?.includes(k) || s.studentName?.includes(k))
})

async function fetchData() {
  loading.value = true
  try {
    const [sRes, rRes, tRes] = await Promise.all([
      getStudentList({ pageNum: 1, pageSize: 1000 }),
      getTeacherStudentList({}),
      getTeacherList({ pageNum: 1, pageSize: 1000 })
    ])
    students.value = sRes.list || []
    relations.value = rRes || []
    teacherOptions.value = tRes.list || []
  } catch (error) {
    ElMessage.error(error.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

function handleEdit(row) {
  currentStudent.value = row
  dialogMode.value = 'single'
  dialogTitle.value = `分配教师 - ${row.studentName}（${row.studentNo}）`
  form.guideTeacherNo = row.guideTeacherNo || ''
  form.reviewTeacherNo = row.reviewTeacherNo || ''
  dialogVisible.value = true
}

function handleBatchAssign() {
  if (selectedStudents.value.length === 0) {
    ElMessage.warning('请至少选择一名学生')
    return
  }
  currentStudent.value = null
  dialogMode.value = 'batch'
  dialogTitle.value = `批量分配教师（已选 ${selectedStudents.value.length} 人）`
  form.guideTeacherNo = ''
  form.reviewTeacherNo = ''
  dialogVisible.value = true
}

function validateAssignList(list) {
  const conflicts = []
  for (const item of list) {
    if (item.guideTeacherNo && item.reviewTeacherNo && item.guideTeacherNo === item.reviewTeacherNo) {
      const s = students.value.find(st => st.studentNo === item.studentNo)
      conflicts.push(s ? `${s.studentName}（${item.studentNo}）` : item.studentNo)
    }
  }
  if (conflicts.length > 0) {
    ElMessage.error(`以下学生的指导教师与评阅教师相同，请修改：${conflicts.join('、')}`)
    return false
  }
  return true
}

function buildSingleList() {
  return [{
    studentNo: currentStudent.value.studentNo,
    guideTeacherNo: form.guideTeacherNo,
    reviewTeacherNo: form.reviewTeacherNo
  }]
}

function buildBatchList() {
  return selectedStudents.value.map(s => ({
    studentNo: s.studentNo,
    guideTeacherNo: form.guideTeacherNo || s.guideTeacherNo,
    reviewTeacherNo: form.reviewTeacherNo || s.reviewTeacherNo
  }))
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const list = dialogMode.value === 'single' ? buildSingleList() : buildBatchList()
  if (!validateAssignList(list)) return

  try {
    await batchAssignTeacherStudent(list)
    ElMessage.success('分配成功')
    dialogVisible.value = false
    selectedStudents.value = []
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '分配失败')
  }
}

function handleSelectionChange(rows) {
  selectedStudents.value = rows
}

onMounted(fetchData)
</script>

<template>
  <div>
    <el-page-header title="教师分配" />
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <el-input v-model="keyword" placeholder="搜索学号/姓名" clearable style="width: 220px;" />
          <el-button type="primary" :disabled="selectedStudents.length === 0" @click="handleBatchAssign">
            批量分配
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="studentNo" label="学号" min-width="120" />
        <el-table-column prop="studentName" label="学生姓名" min-width="120" />
        <el-table-column label="指导教师" min-width="180">
          <template #default="{ row }">
            <span>{{ row.guideTeacherName }}</span>
            <span v-if="row.guideTeacherNo" class="teacher-no">（{{ row.guideTeacherNo }}）</span>
          </template>
        </el-table-column>
        <el-table-column label="评阅教师" min-width="180">
          <template #default="{ row }">
            <span>{{ row.reviewTeacherName }}</span>
            <span v-if="row.reviewTeacherNo" class="teacher-no">（{{ row.reviewTeacherNo }}）</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">
              编辑
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="指导教师">
          <el-select v-model="form.guideTeacherNo" placeholder="请选择指导教师" clearable filterable style="width: 100%;">
            <el-option v-for="teacher in teacherOptions" :key="teacher.teacherNo"
              :label="`${teacher.teacherNo} - ${teacher.teacherName}`" :value="teacher.teacherNo" />
          </el-select>
        </el-form-item>
        <el-form-item label="评阅教师">
          <el-select v-model="form.reviewTeacherNo" placeholder="请选择评阅教师" clearable filterable style="width: 100%;">
            <el-option v-for="teacher in teacherOptions" :key="teacher.teacherNo"
              :label="`${teacher.teacherNo} - ${teacher.teacherName}`" :value="teacher.teacherNo" />
          </el-select>
        </el-form-item>
        <el-alert v-if="dialogMode === 'batch'" title="批量分配时，留空表示保持该学生原有教师不变" type="info" :closable="false" />
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

.teacher-no {
  color: #909399;
  font-size: 12px;
  margin-left: 4px;
}
</style>

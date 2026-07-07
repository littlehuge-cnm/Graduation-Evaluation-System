<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTeacherStudentList, addTeacherStudent, updateTeacherStudent, deleteTeacherStudent, updateRelationStatus } from '@/api/teacherStudent.js'
import { getTeacherList } from '@/api/teacher.js'
import { getStudentList } from '@/api/student.js'

const loading = ref(false)
const tableData = ref([])
const teacherOptions = ref([])
const studentOptions = ref([])
const filters = reactive({
  teacherNo: '',
  studentNo: '',
  relationType: ''
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  studentNo: '',
  teacherNo: '',
  relationType: '指导',
  relationStatus: 1
})

const rules = {
  studentNo: [{ required: true, message: '请选择学生', trigger: 'change' }],
  teacherNo: [{ required: true, message: '请选择教师', trigger: 'change' }],
  relationType: [{ required: true, message: '请选择关系类型', trigger: 'change' }]
}

const relationTypeOptions = ['指导', '评阅']
const relationStatusOptions = [
  { label: '生效', value: 1 },
  { label: '已解除', value: 2 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getTeacherStudentList({
      teacherNo: filters.teacherNo || undefined,
      studentNo: filters.studentNo || undefined,
      relationType: filters.relationType || undefined
    })
    tableData.value = res || []
  } finally {
    loading.value = false
  }
}

async function fetchOptions() {
  try {
    const [tRes, sRes] = await Promise.all([
      getTeacherList({ pageNum: 1, pageSize: 1000 }),
      getStudentList({ pageNum: 1, pageSize: 1000 })
    ])
    teacherOptions.value = tRes.list || []
    studentOptions.value = sRes.list || []
  } catch (error) {
    console.error(error)
  }
}

function handleSearch() {
  fetchData()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增师生关系'
  Object.assign(form, {
    id: null,
    studentNo: '',
    teacherNo: '',
    relationType: '指导',
    relationStatus: 1
  })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  dialogTitle.value = '编辑师生关系'
  Object.assign(form, {
    id: row.id,
    studentNo: row.studentNo,
    teacherNo: row.teacherNo,
    relationType: row.relationType,
    relationStatus: row.relationStatus
  })
  dialogVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该师生关系吗？', '提示', { type: 'warning' })
    await deleteTeacherStudent(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

async function handleStatusChange(row) {
  try {
    await updateRelationStatus(row.id, row.relationStatus)
    ElMessage.success('状态更新成功')
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '状态更新失败')
    fetchData()
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    const data = {
      studentNo: form.studentNo,
      teacherNo: form.teacherNo,
      relationType: form.relationType,
      relationStatus: form.relationStatus
    }

    if (isEdit.value) {
      await updateTeacherStudent(form.id, data)
      ElMessage.success('修改成功')
    } else {
      await addTeacherStudent(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

function getStatusType(status) {
  return status === 1 ? 'success' : status === 2 ? 'info' : 'info'
}

function getStatusLabel(status) {
  const item = relationStatusOptions.find(i => i.value === status)
  return item ? item.label : '未知'
}

onMounted(() => {
  fetchData()
  fetchOptions()
})
</script>

<template>
  <div>
    <el-page-header title="师生关系管理" />
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <div class="search-bar">
            <el-select v-model="filters.teacherNo" placeholder="按教师筛选" clearable style="width: 180px;">
              <el-option
                v-for="teacher in teacherOptions"
                :key="teacher.teacherNo"
                :label="`${teacher.teacherNo} - ${teacher.teacherName}`"
                :value="teacher.teacherNo"
              />
            </el-select>
            <el-select v-model="filters.studentNo" placeholder="按学生筛选" clearable style="width: 180px;">
              <el-option
                v-for="student in studentOptions"
                :key="student.studentNo"
                :label="`${student.studentNo} - ${student.studentName}`"
                :value="student.studentNo"
              />
            </el-select>
            <el-select v-model="filters.relationType" placeholder="按关系类型" clearable style="width: 140px;">
              <el-option v-for="type in relationTypeOptions" :key="type" :label="type" :value="type" />
            </el-select>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </div>
          <el-button type="primary" @click="handleAdd">新增师生关系</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="studentNo" label="学号" min-width="120" />
        <el-table-column prop="studentName" label="学生姓名" min-width="120" />
        <el-table-column prop="teacherNo" label="工号" min-width="120" />
        <el-table-column prop="teacherName" label="教师姓名" min-width="120" />
        <el-table-column prop="relationType" label="关系类型" width="100" />
        <el-table-column prop="relationStatusDesc" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.relationStatus)">{{ row.relationStatusDesc || getStatusLabel(row.relationStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
            <el-select v-model="row.relationStatus" size="small" style="width: 90px; margin-left: 8px;" @change="handleStatusChange(row)">
              <el-option
                v-for="item in relationStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="学生" prop="studentNo">
          <el-select v-model="form.studentNo" placeholder="请选择学生" style="width: 100%;" :disabled="isEdit">
            <el-option
              v-for="student in studentOptions"
              :key="student.studentNo"
              :label="`${student.studentNo} - ${student.studentName}`"
              :value="student.studentNo"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="教师" prop="teacherNo">
          <el-select v-model="form.teacherNo" placeholder="请选择教师" style="width: 100%;" :disabled="isEdit">
            <el-option
              v-for="teacher in teacherOptions"
              :key="teacher.teacherNo"
              :label="`${teacher.teacherNo} - ${teacher.teacherName}`"
              :value="teacher.teacherNo"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关系类型" prop="relationType">
          <el-radio-group v-model="form.relationType" :disabled="isEdit">
            <el-radio v-for="type in relationTypeOptions" :key="type" :label="type">{{ type }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态">
          <el-radio-group v-model="form.relationStatus">
            <el-radio :label="1">生效</el-radio>
            <el-radio :label="2">已解除</el-radio>
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

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStudentGroupList, addStudentGroup, updateStudentGroup, deleteStudentGroup } from '@/api/studentGroup.js'
import { getStudentList } from '@/api/student.js'

const loading = ref(false)
const tableData = ref([])
const allStudents = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({
  groupId: null,
  groupName: '',
  studentNos: []
})

const rules = {
  groupName: [{ required: true, message: '请输入组名', trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getStudentGroupList()
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

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增学生分组'
  Object.assign(form, {
    groupId: null,
    groupName: '',
    studentNos: []
  })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  dialogTitle.value = '编辑学生分组'
  Object.assign(form, {
    groupId: row.groupId,
    groupName: row.groupName,
    studentNos: row.students ? row.students.map(s => s.studentNo) : []
  })
  dialogVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该学生分组吗？', '提示', { type: 'warning' })
    await deleteStudentGroup(row.groupId)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    const data = {
      groupName: form.groupName,
      studentNos: form.studentNos
    }

    if (isEdit.value) {
      await updateStudentGroup(form.groupId, data)
      ElMessage.success('修改成功')
    } else {
      await addStudentGroup(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

onMounted(() => {
  fetchData()
  fetchStudents()
})
</script>

<template>
  <div>
    <el-page-header title="学生分组管理" />
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>学生分组列表</span>
          <el-button type="primary" @click="handleAdd">新增学生分组</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="groupId" label="组号" width="80" />
        <el-table-column prop="groupName" label="组名" min-width="160" />
        <el-table-column prop="studentCount" label="学生数" width="100" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="组名" prop="groupName">
          <el-input v-model="form.groupName" placeholder="请输入组名" />
        </el-form-item>
        <el-form-item label="组内学生">
          <el-select-v2
            v-model="form.studentNos"
            :options="allStudents.map(s => ({ label: `${s.studentNo} - ${s.studentName}`, value: s.studentNo }))"
            placeholder="请选择学生"
            multiple
            clearable
            :height="200"
            style="width: 100%;"
          />
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
}
</style>

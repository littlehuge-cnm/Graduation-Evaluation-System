<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getGroupMappingList, addGroupMapping, updateGroupMapping, deleteGroupMapping } from '@/api/groupMapping.js'
import { getTeacherGroupList } from '@/api/teacherGroup.js'
import { getStudentGroupList } from '@/api/studentGroup.js'

const loading = ref(false)
const tableData = ref([])
const teacherGroupOptions = ref([])
const studentGroupOptions = ref([])
const stage = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  stage: '开题',
  teacherGroupId: '',
  studentGroupId: ''
})

const rules = {
  stage: [{ required: true, message: '请选择环节', trigger: 'change' }],
  teacherGroupId: [{ required: true, message: '请选择教师组', trigger: 'change' }],
  studentGroupId: [{ required: true, message: '请选择学生组', trigger: 'change' }]
}

const stageOptions = ['开题', '中期', '答辩']

async function fetchData() {
  loading.value = true
  try {
    const res = await getGroupMappingList({ stage: stage.value || undefined })
    tableData.value = res || []
  } finally {
    loading.value = false
  }
}

async function fetchGroups() {
  try {
    const [tRes, sRes] = await Promise.all([getTeacherGroupList(), getStudentGroupList()])
    teacherGroupOptions.value = tRes || []
    studentGroupOptions.value = sRes || []
  } catch (error) {
    console.error(error)
  }
}

function handleSearch() {
  fetchData()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增环节对应关系'
  Object.assign(form, {
    id: null,
    stage: '开题',
    teacherGroupId: '',
    studentGroupId: ''
  })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  dialogTitle.value = '编辑环节对应关系'
  Object.assign(form, {
    id: row.id,
    stage: row.stage,
    teacherGroupId: row.teacherGroupId,
    studentGroupId: row.studentGroupId
  })
  dialogVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该对应关系吗？', '提示', { type: 'warning' })
    await deleteGroupMapping(row.id)
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
      stage: form.stage,
      teacherGroupId: form.teacherGroupId,
      studentGroupId: form.studentGroupId
    }

    if (isEdit.value) {
      await updateGroupMapping(form.id, data)
      ElMessage.success('修改成功')
    } else {
      await addGroupMapping(data)
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
  fetchGroups()
})
</script>

<template>
  <div>
    <el-page-header title="环节对应关系管理" />
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <div class="search-bar">
            <el-select v-model="stage" placeholder="按环节筛选" clearable style="width: 160px;">
              <el-option v-for="s in stageOptions" :key="s" :label="s" :value="s" />
            </el-select>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </div>
          <el-button type="primary" @click="handleAdd">新增对应关系</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="stage" label="环节" width="100" />
        <el-table-column prop="teacherGroupName" label="教师组" min-width="160" />
        <el-table-column prop="studentGroupName" label="学生组" min-width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="环节" prop="stage">
          <el-select v-model="form.stage" placeholder="请选择环节" style="width: 100%;">
            <el-option v-for="s in stageOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="教师组" prop="teacherGroupId">
          <el-select v-model="form.teacherGroupId" placeholder="请选择教师组" style="width: 100%;">
            <el-option
              v-for="group in teacherGroupOptions"
              :key="group.groupId"
              :label="group.groupName"
              :value="group.groupId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="学生组" prop="studentGroupId">
          <el-select v-model="form.studentGroupId" placeholder="请选择学生组" style="width: 100%;">
            <el-option
              v-for="group in studentGroupOptions"
              :key="group.groupId"
              :label="group.groupName"
              :value="group.groupId"
            />
          </el-select>
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

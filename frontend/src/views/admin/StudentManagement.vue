<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStudentList, addStudent, updateStudent, deleteStudent, importStudents } from '@/api/student.js'
import { getStudentGroupList } from '@/api/studentGroup.js'

const loading = ref(false)
const tableData = ref([])
const keyword = ref('')
const groupId = ref('')
const groupOptions = ref([])
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({
  studentNo: '',
  studentName: '',
  gender: '男',
  className: '',
  major: '',
  grade: '',
  studentGroupId: '',
  password: '',
  accountStatus: 1,
  overallStatus: 1
})

const rules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  studentName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const groupNameMap = computed(() => {
  return Object.fromEntries(groupOptions.value.map(g => [g.groupId, g.groupName]))
})

function getStudentGroupName(groupId) {
  return groupNameMap.value[groupId] || '未分组'
}

const genderOptions = ['男', '女']
const overallStatusOptions = [
  { label: '待分配', value: 1 },
  { label: '进行中', value: 2 },
  { label: '待答辩', value: 3 },
  { label: '已完成', value: 4 },
  { label: '已弃做', value: 5 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getStudentList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: keyword.value || undefined,
      groupId: groupId.value || undefined
    })
    tableData.value = res.list || []
    pagination.total = res.total || 0
  } finally {
    loading.value = false
  }
}

async function fetchGroups() {
  try {
    const res = await getStudentGroupList()
    groupOptions.value = res || []
  } catch (error) {
    console.error(error)
  }
}

function handleSearch() {
  pagination.pageNum = 1
  fetchData()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增学生'
  Object.assign(form, {
    studentNo: '',
    studentName: '',
    gender: '男',
    className: '',
    major: '',
    grade: '',
    studentGroupId: null,
    password: '',
    accountStatus: 1,
    overallStatus: 1
  })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  dialogTitle.value = '编辑学生'
  Object.assign(form, {
    studentNo: row.studentNo,
    studentName: row.studentName,
    gender: row.gender || '男',
    className: row.className || '',
    major: row.major || '',
    grade: row.grade || '',
    studentGroupId: row.studentGroupId || null,
    password: '',
    accountStatus: row.accountStatus,
    overallStatus: row.overallStatus || 1
  })
  dialogVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该学生吗？', '提示', { type: 'warning' })
    await deleteStudent(row.studentNo)
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
      studentNo: form.studentNo,
      studentName: form.studentName,
      gender: form.gender,
      className: form.className,
      major: form.major,
      grade: form.grade,
      studentGroupId: form.studentGroupId || null,
      accountStatus: form.accountStatus,
      overallStatus: form.overallStatus
    }
    if (form.password) {
      data.password = form.password
    }

    if (isEdit.value) {
      await updateStudent(form.studentNo, data)
      ElMessage.success('修改成功')
    } else {
      data.password = form.password
      await addStudent(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

async function handleImport(file) {
  try {
    const count = await importStudents(file.raw)
    ElMessage.success(`成功导入 ${count} 条学生记录`)
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '导入失败')
  }
  return false
}

function handleSizeChange(val) {
  pagination.pageSize = val
  pagination.pageNum = 1
  fetchData()
}

function handleCurrentChange(val) {
  pagination.pageNum = val
  fetchData()
}

function getOverallStatusLabel(value) {
  const item = overallStatusOptions.find(i => i.value === value)
  return item ? item.label : '未知'
}

onMounted(() => {
  fetchData()
  fetchGroups()
})
</script>

<template>
  <div>
    <el-page-header title="学生管理" />
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <div class="search-bar">
            <el-input v-model="keyword" placeholder="按学号/姓名搜索" clearable style="width: 180px;"
              @keyup.enter="handleSearch" />
            <el-select v-model="groupId" placeholder="按学生组筛选" clearable style="width: 160px;">
              <el-option v-for="group in groupOptions" :key="group.groupId" :label="group.groupName"
                :value="group.groupId" />
            </el-select>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </div>
          <div>
            <el-upload action="" :before-upload="handleImport" :show-file-list="false" accept=".xlsx,.xls,.csv"
              style="display: inline-block; margin-right: 12px;">
              <el-button>批量导入</el-button>
            </el-upload>
            <el-button type="primary" @click="handleAdd">新增学生</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="studentNo" label="学号" min-width="120" />
        <el-table-column prop="studentName" label="姓名" min-width="100" />
        <el-table-column prop="gender" label="性别" width="80" />
        <el-table-column prop="className" label="班级" min-width="120" />
        <el-table-column prop="major" label="专业" min-width="140" />
        <el-table-column prop="grade" label="年级" width="100" />
        <el-table-column label="学生组" min-width="120">
          <template #default="{ row }">
            {{ getStudentGroupName(row.studentGroupId) }}
          </template>
        </el-table-column>
        <el-table-column prop="overallStatusDesc" label="整体进度" width="100" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pagination.pageNum" v-model:page-size="pagination.pageSize"
        :total="pagination.total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
        class="pagination" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="学号" prop="studentNo">
          <el-input v-model="form.studentNo" :disabled="isEdit" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="姓名" prop="studentName">
          <el-input v-model="form.studentName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio v-for="g in genderOptions" :key="g" :label="g">{{ g }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="班级">
          <el-input v-model="form.className" placeholder="请输入班级" />
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="form.major" placeholder="请输入专业" />
        </el-form-item>
        <el-form-item label="年级">
          <el-input v-model="form.grade" placeholder="请输入年级" />
        </el-form-item>
        <el-form-item label="学生组">
          <el-select v-model="form.studentGroupId" placeholder="请选择学生组" clearable style="width: 100%;">
            <el-option v-for="group in groupOptions" :key="group.groupId" :label="group.groupName"
              :value="group.groupId" />
          </el-select>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" :placeholder="isEdit ? '不修改请留空' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="整体进度">
          <el-select v-model="form.overallStatus" placeholder="请选择整体进度" style="width: 100%;">
            <el-option v-for="item in overallStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
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

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>

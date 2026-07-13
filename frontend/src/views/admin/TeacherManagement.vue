<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTeacherList, addTeacher, updateTeacher, deleteTeacher, importTeachers } from '@/api/teacher.js'

const loading = ref(false)
const tableData = ref([])
const keyword = ref('')
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
  teacherNo: '',
  teacherName: '',
  gender: '男',
  department: '',
  title: '',
  phone: '',
  password: '',
  accountStatus: 1
})

const rules = {
  teacherNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  teacherName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const genderOptions = ['男', '女']

async function fetchData() {
  loading.value = true
  try {
    const res = await getTeacherList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: keyword.value || undefined
    })
    tableData.value = res.list || []
    pagination.total = res.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  fetchData()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增教师'
  Object.assign(form, {
    teacherNo: '',
    teacherName: '',
    gender: '男',
    department: '',
    title: '',
    phone: '',
    password: '',
    accountStatus: 1
  })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  dialogTitle.value = '编辑教师'
  Object.assign(form, {
    teacherNo: row.teacherNo,
    teacherName: row.teacherName,
    gender: row.gender || '男',
    department: row.department || '',
    title: row.title || '',
    phone: row.phone || '',
    password: '',
    accountStatus: row.accountStatus
  })
  dialogVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该教师吗？', '提示', { type: 'warning' })
    await deleteTeacher(row.teacherNo)
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
      teacherNo: form.teacherNo,
      teacherName: form.teacherName,
      gender: form.gender,
      department: form.department,
      title: form.title,
      phone: form.phone,
      accountStatus: form.accountStatus
    }
    if (form.password) {
      data.password = form.password
    }

    if (isEdit.value) {
      await updateTeacher(form.teacherNo, data)
      ElMessage.success('修改成功')
    } else {
      data.password = form.password
      await addTeacher(data)
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
    const count = await importTeachers(file.raw)
    ElMessage.success(`成功导入 ${count} 条教师记录`)
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

onMounted(fetchData)
</script>

<template>
  <div>
    <el-page-header title="教师管理" :icon="null" />
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <div class="search-bar">
            <el-input v-model="keyword" placeholder="按工号/姓名搜索" clearable style="width: 220px;"
              @keyup.enter="handleSearch" />
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </div>
          <div>
            <el-upload action="" :before-upload="handleImport" :show-file-list="false" accept=".xlsx,.xls,.csv"
              style="display: inline-block; margin-right: 12px;">
              <el-button>批量导入</el-button>
            </el-upload>
            <el-button type="primary" @click="handleAdd">新增教师</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="teacherNo" label="工号" min-width="120" />
        <el-table-column prop="teacherName" label="姓名" min-width="100" />
        <el-table-column prop="gender" label="性别" width="80" />
        <el-table-column prop="department" label="院系" min-width="140" />
        <el-table-column prop="title" label="职称" min-width="120" />
        <el-table-column prop="phone" label="联系方式" min-width="140" />
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="工号" prop="teacherNo">
          <el-input v-model="form.teacherNo" :disabled="isEdit" placeholder="请输入工号" />
        </el-form-item>
        <el-form-item label="姓名" prop="teacherName">
          <el-input v-model="form.teacherName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio v-for="g in genderOptions" :key="g" :label="g">{{ g }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="院系">
          <el-input v-model="form.department" placeholder="请输入院系" />
        </el-form-item>
        <el-form-item label="职称">
          <el-input v-model="form.title" placeholder="请输入职称" />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="form.phone" placeholder="请输入联系方式" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" :placeholder="isEdit ? '不修改请留空' : '请输入密码'" />
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

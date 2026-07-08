<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminList, addAdmin, updateAdmin, deleteAdmin } from '@/api/admin.js'
import { updateAccountStatus } from '@/api/account.js'

const loading = ref(false)
const tableData = ref([])
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
  adminId: '',
  adminName: '',
  password: '',
  accountStatus: 1
})

const rules = {
  adminId: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  adminName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 2 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getAdminList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.records || []
    pagination.total = res.total || 0
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增管理员'
  Object.assign(form, {
    adminId: '',
    adminName: '',
    password: '',
    accountStatus: 1
  })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  dialogTitle.value = '编辑管理员'
  Object.assign(form, {
    adminId: row.adminId,
    adminName: row.adminName,
    password: '',
    accountStatus: row.accountStatus
  })
  dialogVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该管理员吗？', '提示', { type: 'warning' })
    await deleteAdmin(row.adminId)
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
    await updateAccountStatus({
      userType: 'admin',
      username: row.adminId,
      accountStatus: row.accountStatus
    })
    ElMessage.success('状态更新成功')
  } catch (error) {
    row.accountStatus = row.accountStatus === 1 ? 2 : 1
    ElMessage.error(error.message || '状态更新失败')
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    const data = {
      adminId: form.adminId,
      adminName: form.adminName,
      accountStatus: form.accountStatus
    }
    if (form.password) {
      data.password = form.password
    }

    if (isEdit.value) {
      await updateAdmin(form.adminId, data)
      ElMessage.success('修改成功')
    } else {
      data.password = form.password
      await addAdmin(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
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

function getStatusText(status) {
  return status === 1 ? '启用' : status === 2 ? '禁用' : '未知'
}

function getStatusType(status) {
  return status === 1 ? 'success' : status === 2 ? 'danger' : 'info'
}

onMounted(fetchData)
</script>

<template>
  <div>
    <el-page-header title="管理员管理" />
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>管理员列表</span>
          <el-button type="primary" @click="handleAdd">新增管理员</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="adminId" label="管理员账号" min-width="140" />
        <el-table-column prop="adminName" label="姓名" min-width="120" />
        <el-table-column prop="accountStatusDesc" label="状态" min-width="120">
          <template #default="{ row }">
            <el-switch v-model="row.accountStatus" :active-value="1" :inactive-value="2" active-text="启用"
              inactive-text="禁用" inline-prompt @change="handleStatusChange(row)" />
          </template>
        </el-table-column>
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
        <el-form-item label="账号" prop="adminId">
          <el-input v-model="form.adminId" :disabled="isEdit" placeholder="请输入管理员账号" />
        </el-form-item>
        <el-form-item label="姓名" prop="adminName">
          <el-input v-model="form.adminName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" :placeholder="isEdit ? '不修改请留空' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.accountStatus">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="2">禁用</el-radio>
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
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTeacherGroupList, addTeacherGroup, updateTeacherGroup, deleteTeacherGroup } from '@/api/teacherGroup.js'
import { getTeacherList } from '@/api/teacher.js'

const loading = ref(false)
const tableData = ref([])
const teacherOptions = ref([])
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({
  groupId: null,
  groupName: '',
  leaderNo: '',
  secretaryNo: '',
  memberNo: '',
  groupStatus: 1
})

const rules = {
  groupName: [{ required: true, message: '请输入组名', trigger: 'blur' }],
  leaderNo: [{ required: true, message: '请选择组长', trigger: 'change' }],
  secretaryNo: [{ required: true, message: '请选择秘书', trigger: 'change' }],
  memberNo: [{ required: true, message: '请选择普通成员', trigger: 'change' }]
}

const filteredDataAll = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return tableData.value
  return tableData.value.filter(item =>
    item.groupName?.toLowerCase().includes(k) ||
    item.leaderName?.toLowerCase().includes(k) ||
    item.secretaryName?.toLowerCase().includes(k) ||
    item.memberName?.toLowerCase().includes(k)
  )
})

const total = computed(() => filteredDataAll.value.length)

const filteredData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredDataAll.value.slice(start, end)
})

function handlePageChange(page) {
  currentPage.value = page
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getTeacherGroupList()
    tableData.value = res || []
  } finally {
    loading.value = false
  }
}

async function fetchTeachers() {
  try {
    const res = await getTeacherList({ pageNum: 1, pageSize: 1000 })
    teacherOptions.value = res.list || []
  } catch (error) {
    console.error(error)
  }
}

function handleSearch() {
  keyword.value = keyword.value.trim()
  currentPage.value = 1
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增教师分组'
  Object.assign(form, {
    groupId: null,
    groupName: '',
    leaderNo: '',
    secretaryNo: '',
    memberNo: '',
    groupStatus: 2
  })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  dialogTitle.value = '编辑教师分组'
  Object.assign(form, {
    groupId: row.groupId,
    groupName: row.groupName,
    leaderNo: row.leaderNo,
    secretaryNo: row.secretaryNo,
    memberNo: row.memberNo,
    groupStatus: row.groupStatus
  })
  dialogVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该教师分组吗？', '提示', { type: 'warning' })
    await deleteTeacherGroup(row.groupId)
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
      leaderNo: form.leaderNo,
      secretaryNo: form.secretaryNo,
      memberNo: form.memberNo,
      groupStatus: form.groupStatus
    }

    if (isEdit.value) {
      await updateTeacherGroup(form.groupId, data)
      ElMessage.success('修改成功')
    } else {
      await addTeacherGroup(data)
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
  fetchTeachers()
})
</script>

<template>
  <div>
    <el-page-header title="教师分组管理" />
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <div class="search-bar">
            <el-input v-model="keyword" placeholder="搜索组名/组长/秘书/成员" clearable style="width: 260px;"
              @keyup.enter="handleSearch" />
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </div>
          <el-button type="primary" @click="handleAdd">新增教师分组</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="filteredData" border>
        <el-table-column prop="groupId" label="组号" width="80" />
        <el-table-column prop="groupName" label="组名" min-width="140" />
        <el-table-column prop="leaderName" label="组长" min-width="120" />
        <el-table-column prop="secretaryName" label="秘书" min-width="120" />
        <el-table-column prop="memberName" label="普通成员" min-width="120" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]"
          :total="total" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
          @current-change="handlePageChange" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="组名" prop="groupName">
          <el-input v-model="form.groupName" placeholder="请输入组名" />
        </el-form-item>
        <el-form-item label="组长" prop="leaderNo">
          <el-select v-model="form.leaderNo" placeholder="请选择组长" filterable style="width: 100%;">
            <el-option v-for="teacher in teacherOptions" :key="teacher.teacherNo"
              :label="`${teacher.teacherNo} - ${teacher.teacherName}`" :value="teacher.teacherNo" />
          </el-select>
        </el-form-item>
        <el-form-item label="秘书" prop="secretaryNo">
          <el-select v-model="form.secretaryNo" placeholder="请选择秘书" filterable style="width: 100%;">
            <el-option v-for="teacher in teacherOptions" :key="teacher.teacherNo"
              :label="`${teacher.teacherNo} - ${teacher.teacherName}`" :value="teacher.teacherNo" />
          </el-select>
        </el-form-item>
        <el-form-item label="普通成员" prop="memberNo">
          <el-select v-model="form.memberNo" placeholder="请选择普通成员" filterable style="width: 100%;">
            <el-option v-for="teacher in teacherOptions" :key="teacher.teacherNo"
              :label="`${teacher.teacherNo} - ${teacher.teacherName}`" :value="teacher.teacherNo" />
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

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>

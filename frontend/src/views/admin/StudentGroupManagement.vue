<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getStudentGroupList,
  addStudentGroup,
  updateStudentGroup,
  deleteStudentGroup
} from '@/api/studentGroup.js'
import { getStudentList } from '@/api/student.js'

const loading = ref(false)
const tableData = ref([])
const allStudents = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitting = ref(false)
const searchKeyword = ref('')
const formRef = ref(null)
const form = reactive({
  groupId: null,
  groupName: '',
  studentNos: []
})

const rules = {
  groupName: [
    { required: true, message: '请输入组名', trigger: 'blur' },
    { validator: validateGroupName, trigger: 'blur' }
  ]
}

function validateGroupName(rule, value, callback) {
  if (!value) {
    callback()
    return
  }
  const lowerValue = value.toLowerCase()
  const duplicate = tableData.value.find(
    g => g.groupName?.toLowerCase() === lowerValue && g.groupId !== form.groupId
  )
  if (duplicate) {
    callback(new Error('组名已存在，请更换'))
  } else {
    callback()
  }
}

async function fetchGroups() {
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
    allStudents.value = res.records || res.list || []
  } catch (error) {
    console.error(error)
  }
}

const groupMap = computed(() => {
  return Object.fromEntries(tableData.value.map(g => [g.groupId, g.groupName]))
})

const selectedSet = computed(() => new Set(form.studentNos))

const filteredAvailableStudents = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return allStudents.value.filter(s => {
    const text = `${s.studentNo} ${s.studentName}`.toLowerCase()
    return !keyword || text.includes(keyword)
  })
})

function getStudentDisplayLabel(student) {
  const inOtherGroup = form.groupId && student.studentGroupId && student.studentGroupId !== form.groupId
  const otherGroupName = inOtherGroup ? groupMap.value[student.studentGroupId] : ''
  return `${student.studentNo} ${student.studentName}${otherGroupName ? ` [${otherGroupName}]` : ''}`
}

const selectedStudents = computed(() => {
  return form.studentNos
    .map(no => allStudents.value.find(s => s.studentNo === no))
    .filter(Boolean)
})

function isSelected(studentNo) {
  return selectedSet.value.has(studentNo)
}

function isStudentDisabled(student) {
  return Boolean(student.studentGroupId) && student.studentGroupId !== form.groupId
}

function toggleStudent(studentNo) {
  const student = allStudents.value.find(s => s.studentNo === studentNo)
  if (student && isStudentDisabled(student)) {
    return
  }
  const index = form.studentNos.indexOf(studentNo)
  if (index > -1) {
    form.studentNos.splice(index, 1)
  } else {
    form.studentNos.push(studentNo)
  }
}

function removeSelected(studentNo) {
  const index = form.studentNos.indexOf(studentNo)
  if (index > -1) {
    form.studentNos.splice(index, 1)
  }
}

function clearSelected() {
  form.studentNos = []
}

function openAddDialog() {
  isEdit.value = false
  dialogTitle.value = '新增学生分组'
  searchKeyword.value = ''
  Object.assign(form, {
    groupId: null,
    groupName: '',
    studentNos: []
  })
  dialogVisible.value = true
}

async function openEditDialog(row) {
  isEdit.value = true
  dialogTitle.value = '编辑学生分组'
  searchKeyword.value = ''
  let studentNos = []
  if (row.studentNo) {
    studentNos = row.studentNo.split(',').map(s => s.trim()).filter(Boolean)
  }
  Object.assign(form, {
    groupId: row.groupId,
    groupName: row.groupName,
    studentNos
  })
  dialogVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该学生分组吗？', '提示', { type: 'warning' })
    await deleteStudentGroup(row.groupId)
    ElMessage.success('删除成功')
    await fetchGroups()
    await fetchStudents()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 检测重复组员：已存在于其他分组的学生
  const duplicateStudents = form.studentNos
    .map(no => allStudents.value.find(s => s.studentNo === no))
    .filter(s => s && s.studentGroupId && s.studentGroupId !== form.groupId)

  if (duplicateStudents.length > 0) {
    const names = duplicateStudents
      .map(s => {
        const groupName = groupMap.value[s.studentGroupId] || '其他分组'
        return `${s.studentNo} ${s.studentName}（${groupName}）`
      })
      .join('、')
    try {
      await ElMessageBox.confirm(
        `以下学生已在其他分组中：${names}。确定要将其移动到当前分组吗？`,
        '重复组员提示',
        { confirmButtonText: '确定移动', cancelButtonText: '取消', type: 'warning' }
      )
    } catch (error) {
      if (error !== 'cancel') {
        console.error(error)
      }
      return
    }
  }

  submitting.value = true
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
    await fetchGroups()
    await fetchStudents()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchGroups()
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
          <el-button type="primary" @click="openAddDialog">新增学生分组</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="groupId" label="组号" width="80" />
        <el-table-column prop="groupName" label="组名" min-width="160" />
        <el-table-column prop="studentCount" label="学生数" width="100" />
        <el-table-column label="组内学生" min-width="320">
          <template #default="{ row }">
            <div class="student-tags">
              <el-tag v-for="s in row.students || []" :key="s.studentNo" size="small" type="info">
                {{ s.studentNo }} {{ s.studentName }}
              </el-tag>
              <el-tag v-if="!row.students || row.students.length === 0" size="small" type="info">暂无学生</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="900px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="组名" prop="groupName">
          <el-input v-model="form.groupName" placeholder="请输入组名" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="组内学生">
          <div class="student-selector">
            <div class="selector-panel">
              <div class="panel-header">
                <span>可选学生（{{ filteredAvailableStudents.length }}）</span>
              </div>
              <el-input v-model="searchKeyword" placeholder="搜索学号/姓名" clearable size="small" class="panel-search" />
              <div class="panel-list">
                <el-scrollbar max-height="360px">
                  <div v-for="s in filteredAvailableStudents" :key="s.studentNo" class="student-item"
                    :class="{ selected: isSelected(s.studentNo), disabled: isStudentDisabled(s) }"
                    @click="!isStudentDisabled(s) && toggleStudent(s.studentNo)">
                    <el-checkbox :model-value="isSelected(s.studentNo)" :disabled="isStudentDisabled(s)" @click.stop />
                    <span class="student-name">{{ getStudentDisplayLabel(s) }}</span>
                  </div>
                  <el-empty v-if="filteredAvailableStudents.length === 0" description="无匹配学生" :image-size="60" />
                </el-scrollbar>
              </div>
            </div>

            <div class="selector-divider" />

            <div class="selector-panel">
              <div class="panel-header">
                <span>已选择（{{ selectedStudents.length }}）</span>
                <el-button v-if="selectedStudents.length > 0" type="danger" link size="small"
                  @click="clearSelected">清空</el-button>
              </div>
              <div class="panel-list selected-panel">
                <el-scrollbar max-height="400px">
                  <div v-for="s in selectedStudents" :key="s.studentNo" class="selected-tag">
                    <el-tag closable size="small" @close="removeSelected(s.studentNo)">
                      {{ s.studentNo }} {{ s.studentName }}
                    </el-tag>
                  </div>
                  <el-empty v-if="selectedStudents.length === 0" description="未选择学生" :image-size="60" />
                </el-scrollbar>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
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

.student-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.select-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.student-selector {
  display: flex;
  gap: 16px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 12px;
  min-width: 500px;
}

.selector-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.panel-search {
  margin-bottom: 8px;
}

.panel-list {
  flex: 1;
  min-height: 120px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 4px;
}

.student-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.student-item:hover {
  background-color: #f5f7fa;
}

.student-item.selected {
  background-color: #ecf5ff;
}

.student-item.disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background-color: #f5f7fa;
}

.student-item.disabled .student-name {
  color: #909399;
}

.student-name {
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selector-divider {
  width: 1px;
  background-color: #ebeef5;
}

.selected-panel {
  display: flex;
  flex-direction: column;
}

.selected-tag {
  padding: 4px 6px;
}
</style>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { batchAssignGroupMapping, getGroupMappingList, randomAssignGroupMapping } from '@/api/groupMapping.js'
import { getTeacherGroupList } from '@/api/teacherGroup.js'
import { getStudentGroupList } from '@/api/studentGroup.js'

const loading = ref(false)
const studentGroups = ref([])
const teacherGroups = ref([])
const mappings = ref([])
const keyword = ref('')
const selectedGroups = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogMode = ref('single')
const currentGroup = ref(null)
const formRef = ref(null)
const form = reactive({
  openingTeacherGroupId: '',
  midtermTeacherGroupId: '',
  defenseTeacherGroupId: ''
})

const rules = {
  openingTeacherGroupId: [{ required: false }],
  midtermTeacherGroupId: [{ required: false }],
  defenseTeacherGroupId: [{ required: false }]
}

function getStageMapping(studentGroupId, stage) {
  return mappings.value.find(m => m.studentGroupId === studentGroupId && m.stage === stage)
}

const tableData = computed(() => {
  const list = studentGroups.value.map(g => {
    const opening = getStageMapping(g.groupId, '开题')
    const midterm = getStageMapping(g.groupId, '中期')
    const defense = getStageMapping(g.groupId, '答辩')
    return {
      ...g,
      openingTeacherGroupId: opening?.teacherGroupId || '',
      openingTeacherGroupName: opening?.teacherGroupName || '未分配',
      midtermTeacherGroupId: midterm?.teacherGroupId || '',
      midtermTeacherGroupName: midterm?.teacherGroupName || '未分配',
      defenseTeacherGroupId: defense?.teacherGroupId || '',
      defenseTeacherGroupName: defense?.teacherGroupName || '未分配'
    }
  })
  const k = keyword.value.trim().toLowerCase()
  if (!k) return list
  return list.filter(g => {
    if (g.groupName?.toLowerCase().includes(k)) return true
    if (g.students && g.students.length > 0) {
      return g.students.some(s =>
        s.studentNo?.toLowerCase().includes(k) ||
        s.studentName?.toLowerCase().includes(k)
      )
    }
    return false
  })
})

async function fetchData() {
  loading.value = true
  try {
    const [sgRes, tgRes] = await Promise.all([
      getStudentGroupList(),
      getTeacherGroupList()
    ])
    studentGroups.value = sgRes || []
    teacherGroups.value = tgRes || []
    await fetchMappings()
  } catch (error) {
    ElMessage.error(error.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

async function fetchMappings() {
  mappings.value = await getGroupMappingList({})
}

function handleSearch() {
  keyword.value = keyword.value.trim()
}

function handleEdit(row) {
  currentGroup.value = row
  dialogMode.value = 'single'
  dialogTitle.value = `分配教师组 - ${row.groupName}`
  form.openingTeacherGroupId = row.openingTeacherGroupId || ''
  form.midtermTeacherGroupId = row.midtermTeacherGroupId || ''
  form.defenseTeacherGroupId = row.defenseTeacherGroupId || ''
  dialogVisible.value = true
}

function handleBatchAssign() {
  if (selectedGroups.value.length === 0) {
    ElMessage.warning('请至少选择一个学生组')
    return
  }
  currentGroup.value = null
  dialogMode.value = 'batch'
  dialogTitle.value = `批量分配教师组（已选 ${selectedGroups.value.length} 个学生组）`
  form.openingTeacherGroupId = ''
  form.midtermTeacherGroupId = ''
  form.defenseTeacherGroupId = ''
  dialogVisible.value = true
}

function validateAssignList(list) {
  for (const item of list) {
    const opening = item.openingTeacherGroupId || null
    const midterm = item.midtermTeacherGroupId || null
    const defense = item.defenseTeacherGroupId || null
    const values = [opening, midterm, defense].filter(v => v !== null)
    const unique = new Set(values)
    if (unique.size !== values.length) {
      ElMessage.error(`学生组【${item.studentGroupName}】在同一学生组的三个环节中不能分配相同的教师组`)
      return false
    }
  }
  return true
}

function buildSingleList() {
  const row = currentGroup.value
  return [{
    studentGroupId: row.groupId,
    studentGroupName: row.groupName,
    openingTeacherGroupId: toNumber(form.openingTeacherGroupId) || row.openingTeacherGroupId || null,
    midtermTeacherGroupId: toNumber(form.midtermTeacherGroupId) || row.midtermTeacherGroupId || null,
    defenseTeacherGroupId: toNumber(form.defenseTeacherGroupId) || row.defenseTeacherGroupId || null
  }]
}

function buildBatchList() {
  const opening = toNumber(form.openingTeacherGroupId)
  const midterm = toNumber(form.midtermTeacherGroupId)
  const defense = toNumber(form.defenseTeacherGroupId)
  return selectedGroups.value.map(row => ({
    studentGroupId: row.groupId,
    studentGroupName: row.groupName,
    openingTeacherGroupId: opening !== null ? opening : row.openingTeacherGroupId || null,
    midtermTeacherGroupId: midterm !== null ? midterm : row.midtermTeacherGroupId || null,
    defenseTeacherGroupId: defense !== null ? defense : row.defenseTeacherGroupId || null
  }))
}

function toNumber(value) {
  if (value === '' || value === null || value === undefined) return null
  const n = Number(value)
  return Number.isNaN(n) ? null : n
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const list = dialogMode.value === 'single' ? buildSingleList() : buildBatchList()
  if (!validateAssignList(list)) return

  try {
    await batchAssignGroupMapping(list)
    ElMessage.success('分配成功')
    dialogVisible.value = false
    selectedGroups.value = []
    await fetchMappings()
  } catch (error) {
    ElMessage.error(error.message || '分配失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定清空该学生组的所有环节分配吗？', '提示', { type: 'warning' })
    const list = [{
      studentGroupId: row.groupId,
      studentGroupName: row.groupName,
      openingTeacherGroupId: null,
      midtermTeacherGroupId: null,
      defenseTeacherGroupId: null
    }]
    await batchAssignGroupMapping(list)
    ElMessage.success('清空成功')
    await fetchMappings()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '清空失败')
    }
  }
}

async function handleRandomAssign() {
  const isAll = selectedGroups.value.length === 0
  const groupNames = selectedGroups.value.map(g => g.groupName).join('、')
  const confirmMsg = isAll
    ? '确定要为所有学生组随机分配三个环节的教师组吗？\n这将覆盖现有的分配关系，且：\n1. 同一环节教师组与学生组一一对应\n2. 同一学生组三个环节教师组互不相同'
    : `确定要为选中的 ${selectedGroups.value.length} 个学生组（${groupNames}）随机分配三个环节的教师组吗？\n这将覆盖这些组的现有分配，且：\n1. 同一环节教师组与学生组一一对应（不与其他未选中组冲突）\n2. 同一学生组三个环节教师组互不相同`
  try {
    await ElMessageBox.confirm(
      confirmMsg,
      '随机分配确认',
      { confirmButtonText: '确定分配', cancelButtonText: '取消', type: 'warning' }
    )
    const ids = isAll ? [] : selectedGroups.value.map(g => g.groupId)
    await randomAssignGroupMapping(ids)
    ElMessage.success('随机分配成功')
    selectedGroups.value = []
    await fetchMappings()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '随机分配失败')
    }
  }
}

function handleSelectionChange(rows) {
  selectedGroups.value = rows
}

onMounted(fetchData)
</script>

<template>
  <div>
    <el-page-header title="环节分配" />
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <div class="search-bar">
            <el-input v-model="keyword" placeholder="搜索学生组/学号/姓名" clearable style="width: 260px;"
              @keyup.enter="handleSearch" />
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </div>
          <div class="action-buttons">
            <el-button type="success" @click="handleRandomAssign">随机分配</el-button>
            <el-button type="primary" :disabled="selectedGroups.length === 0" @click="handleBatchAssign">
              批量分配
            </el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column label="学生组" min-width="240">
          <template #default="{ row }">
            <div class="group-cell">
              <div class="group-name">{{ row.groupName }}</div>
              <div v-if="row.students && row.students.length > 0" class="group-members">
                <template v-if="row.students.length <= 3">
                  <span v-for="(s, i) in row.students" :key="s.studentNo" class="member-tag">
                    {{ s.studentName }}{{ i < row.students.length - 1 ? '、' : '' }} </span>
                </template>
                <template v-else>
                  <span v-for="(s, i) in row.students.slice(0, 3)" :key="s.studentNo" class="member-tag">
                    {{ s.studentName }}{{ i < 2 ? '、' : '' }} </span>
                      <span class="member-tag more">等{{ row.students.length }}人</span>
                </template>
              </div>
              <div v-else class="group-members empty">暂无组员</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="开题教师组" min-width="180">
          <template #default="{ row }">
            <span>{{ row.openingTeacherGroupName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="中期教师组" min-width="180">
          <template #default="{ row }">
            <span>{{ row.midtermTeacherGroupName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="答辩教师组" min-width="180">
          <template #default="{ row }">
            <span>{{ row.defenseTeacherGroupName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">清空</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="开题教师组">
          <el-select v-model="form.openingTeacherGroupId" placeholder="请选择开题教师组" clearable filterable
            style="width: 100%;">
            <el-option v-for="group in teacherGroups" :key="group.groupId" :label="group.groupName"
              :value="group.groupId" />
          </el-select>
        </el-form-item>
        <el-form-item label="中期教师组">
          <el-select v-model="form.midtermTeacherGroupId" placeholder="请选择中期教师组" clearable filterable
            style="width: 100%;">
            <el-option v-for="group in teacherGroups" :key="group.groupId" :label="group.groupName"
              :value="group.groupId" />
          </el-select>
        </el-form-item>
        <el-form-item label="答辩教师组">
          <el-select v-model="form.defenseTeacherGroupId" placeholder="请选择答辩教师组" clearable filterable
            style="width: 100%;">
            <el-option v-for="group in teacherGroups" :key="group.groupId" :label="group.groupName"
              :value="group.groupId" />
          </el-select>
        </el-form-item>
        <el-alert v-if="dialogMode === 'batch'" title="批量分配时，留空表示保持该学生组原有教师组不变" type="info" :closable="false" />
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

.action-buttons {
  display: flex;
  gap: 8px;
}

.group-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.group-name {
  font-weight: 500;
  color: #303133;
}

.group-members {
  font-size: 12px;
  color: #909399;
}

.group-members .member-tag {
  margin-right: 2px;
}

.group-members .member-tag.more {
  color: #409eff;
}

.group-members.empty {
  color: #c0c4cc;
}
</style>

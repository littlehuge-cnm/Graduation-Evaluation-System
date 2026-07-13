<script setup>
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTeacherStudentList, batchAssignTeacherStudent } from '@/api/teacherStudent.js'
import { getTeacherList } from '@/api/teacher.js'
import { getStudentList } from '@/api/student.js'

const loading = ref(false)
const students = ref([])
const relations = ref([])
const teacherOptions = ref([])
const keyword = ref('')
const selectedStudentNos = ref(new Set())
const currentPage = ref(1)
const pageSize = ref(10)
const tableRef = ref(null)

const selectedStudents = computed(() => {
  return students.value.filter(s => selectedStudentNos.value.has(s.studentNo))
})

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

const tableDataAll = computed(() => {
  const k = keyword.value.trim().toLowerCase()
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
  return list.filter(s =>
    s.studentNo?.toLowerCase().includes(k) ||
    s.studentName?.toLowerCase().includes(k) ||
    s.className?.toLowerCase().includes(k) ||
    s.major?.toLowerCase().includes(k)
  )
})

const total = computed(() => tableDataAll.value.length)

const tableData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return tableDataAll.value.slice(start, end)
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
    nextTick(() => {
      if (tableRef.value) {
        tableData.value.forEach(row => {
          tableRef.value.toggleRowSelection(row, selectedStudentNos.value.has(row.studentNo))
        })
      }
    })
  }
}

function handleSearch() {
  keyword.value = keyword.value.trim()
  currentPage.value = 1
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
    ElMessage.warning('请至少选择一个学生')
    return
  }
  currentStudent.value = null
  dialogMode.value = 'batch'
  dialogTitle.value = `批量分配教师（已选 ${selectedStudents.value.length} 个学生）`
  form.guideTeacherNo = ''
  form.reviewTeacherNo = ''
  dialogVisible.value = true
}

async function handleClear(row) {
  try {
    await ElMessageBox.confirm(`确定清空学生 ${row.studentName}（${row.studentNo}）的指导教师和评阅教师吗？`, '提示', { type: 'warning' })
    const list = [{
      studentNo: row.studentNo,
      guideTeacherNo: null,
      reviewTeacherNo: null
    }]
    await batchAssignTeacherStudent(list)
    ElMessage.success('清空成功')
    await fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '清空失败')
    }
  }
}

function buildSingleList() {
  return [{
    studentNo: currentStudent.value.studentNo,
    guideTeacherNo: form.guideTeacherNo || null,
    reviewTeacherNo: form.reviewTeacherNo || null
  }]
}

function buildBatchList() {
  return selectedStudents.value.map(s => ({
    studentNo: s.studentNo,
    guideTeacherNo: form.guideTeacherNo || null,
    reviewTeacherNo: form.reviewTeacherNo || null
  }))
}

function validateAssignList(list) {
  for (const item of list) {
    const guide = item.guideTeacherNo
    const review = item.reviewTeacherNo
    if (guide && review && guide === review) {
      const student = students.value.find(s => s.studentNo === item.studentNo)
      const name = student ? student.studentName : item.studentNo
      ElMessage.error(`学生 ${name}（${item.studentNo}）的指导教师与评阅教师不能相同`)
      return false
    }
  }
  return true
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
    selectedStudentNos.value.clear()
    await fetchData()
  } catch (error) {
    ElMessage.error(error.message || '分配失败')
  }
}

function handleSelectionChange(rows) {
  tableData.value.forEach(s => {
    if (selectedStudentNos.value.has(s.studentNo)) {
      selectedStudentNos.value.delete(s.studentNo)
    }
  })
  rows.forEach(s => {
    selectedStudentNos.value.add(s.studentNo)
  })
}

watch([currentPage, pageSize], () => {
  nextTick(() => {
    if (tableRef.value) {
      tableData.value.forEach(row => {
        tableRef.value.toggleRowSelection(row, selectedStudentNos.value.has(row.studentNo))
      })
    }
  })
})

onMounted(fetchData)
</script>

<template>
  <div>
    <el-page-header title="教师分配" :icon="null" />
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <div class="search-bar">
            <el-input v-model="keyword" placeholder="搜索学号/姓名/班级/专业" clearable style="width: 260px;"
              @keyup.enter="handleSearch" />
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </div>
          <el-button type="primary" :disabled="selectedStudents.length === 0" @click="handleBatchAssign">
            批量分配
          </el-button>
        </div>
      </template>

      <el-table ref="tableRef" v-loading="loading" :data="tableData" border row-key="studentNo"
        @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" reserve-selection />
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
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleClear(row)">清空</el-button>
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

.search-bar {
  display: flex;
  gap: 8px;
}

.teacher-no {
  color: #909399;
  font-size: 12px;
  margin-left: 4px;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>

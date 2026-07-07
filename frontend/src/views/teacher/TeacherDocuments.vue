<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user.js'
import { getMyStudentDocuments, saveDocument, updateDocument, submitDocument } from '@/api/document.js'

const userStore = useUserStore()
const loading = ref(false)
const activeDocType = ref('任务书')
const tableData = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  studentNo: '',
  studentName: '',
  docType: '任务书',
  title: '',
  subjectCategory: '',
  subjectType: '',
  subjectNewOld: '',
  content: '',
  status: 1
})

const rules = {
  title: [{ required: true, message: '请输入题目', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const docTypeOptions = ['任务书', '指导书']
const categoryOptions = ['A', 'B', 'C', 'D']
const typeOptions = ['A', 'B', 'C']
const newOldOptions = ['A', 'B']
const statusOptions = [
  { label: '草稿', value: 1 },
  { label: '已提交', value: 2 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getMyStudentDocuments(userStore.username, activeDocType.value)
    tableData.value = res || []
  } finally {
    loading.value = false
  }
}

function handleDocTypeChange() {
  fetchData()
}

function handleEdit(row) {
  isEdit.value = true
  dialogTitle.value = `编辑${row.docType || activeDocType.value}`
  Object.assign(form, {
    id: row.docId || null,
    studentNo: row.studentNo,
    studentName: row.studentName,
    docType: row.docType || activeDocType.value,
    title: row.title || '',
    subjectCategory: row.subjectCategory || '',
    subjectType: row.subjectType || '',
    subjectNewOld: row.subjectNewOld || '',
    content: row.content || '',
    status: row.status || 1
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    const data = {
      studentNo: form.studentNo,
      docType: form.docType,
      title: form.title,
      subjectCategory: form.subjectCategory,
      subjectType: form.subjectType,
      subjectNewOld: form.subjectNewOld,
      content: form.content,
      status: form.status
    }

    if (isEdit.value && form.id) {
      await updateDocument(form.id, data)
      ElMessage.success('修改成功')
    } else {
      await saveDocument(data)
      ElMessage.success('保存成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

async function handleSubmitDoc(row) {
  try {
    await submitDocument(row.docId)
    ElMessage.success('提交成功')
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '提交失败')
  }
}

function getStatusType(status) {
  return status === 2 ? 'success' : 'info'
}

function getStatusLabel(status) {
  const item = statusOptions.find(i => i.value === status)
  return item ? item.label : '未知'
}

onMounted(fetchData)
</script>

<template>
  <div>
    <el-page-header title="任务书/指导书" />
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <el-radio-group v-model="activeDocType" size="large" @change="handleDocTypeChange">
            <el-radio-button v-for="type in docTypeOptions" :key="type" :label="type">{{ type }}</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="studentNo" label="学号" min-width="120" />
        <el-table-column prop="studentName" label="姓名" min-width="100" />
        <el-table-column prop="title" label="题目" min-width="200" show-overflow-tooltip />
        <el-table-column prop="statusDesc" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.statusDesc || getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">{{ row.docId ? '编辑' : '填写' }}</el-button>
            <el-button v-if="row.docId && row.status === 1" type="success" link @click="handleSubmitDoc(row)">提交</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="学生">
          <span>{{ form.studentNo }} - {{ form.studentName }}</span>
        </el-form-item>
        <el-form-item v-if="form.docType === '任务书'" label="题目" prop="title">
          <el-input v-model="form.title" placeholder="请输入毕业设计题目" />
        </el-form-item>
        <el-form-item v-if="form.docType === '任务书'" label="课题类别">
          <el-radio-group v-model="form.subjectCategory">
            <el-radio v-for="c in categoryOptions" :key="c" :label="c">{{ c }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.docType === '任务书'" label="课题类型">
          <el-radio-group v-model="form.subjectType">
            <el-radio v-for="t in typeOptions" :key="t" :label="t">{{ t }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.docType === '任务书'" label="新旧课题">
          <el-radio-group v-model="form.subjectNewOld">
            <el-radio v-for="n in newOldOptions" :key="n" :label="n">{{ n }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">草稿</el-radio>
            <el-radio :label="2">已提交</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
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

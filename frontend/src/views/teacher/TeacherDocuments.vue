<script setup>
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import { getTeacherStudents } from '@/api/teacher.js'
import { getStudentDocuments } from '@/api/student.js'
import { saveDocument, updateDocument } from '@/api/document.js'

const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const students = ref([])
const studentDocStatusMap = ref({})
const keyword = ref('')
const selectedStudent = ref(null)

const CONTENT_SPLIT = '\n\n=====基本要求=====\n\n'

const taskForm = reactive({
  id: null,
  title: '',
  subjectCategory: '',
  subjectType: '',
  subjectNewOld: '',
  mainContent: '',
  basicRequirement: ''
})

const guideForm = reactive({
  id: null,
  content: ''
})

const categoryOptions = [
  { value: 'A', label: 'A.工程设计' },
  { value: 'B', label: 'B.科学研究' },
  { value: 'C', label: 'C.技术开发' },
  { value: 'D', label: 'D.其他' }
]
const typeOptions = [
  { value: 'A', label: 'A.真题' },
  { value: 'B', label: 'B.模拟题（假题）' },
  { value: 'C', label: 'C.真题假作' }
]
const newOldOptions = [
  { value: 'A', label: 'A.新题' },
  { value: 'B', label: 'B.旧题' }
]

const filteredStudents = computed(() => {
  let list = students.value
  if (keyword.value) {
    const k = keyword.value.trim().toLowerCase()
    list = list.filter(s =>
      s.studentNo?.toLowerCase().includes(k) || s.studentName?.toLowerCase().includes(k)
    )
  }
  return [...list].sort((a, b) => {
    const statusA = studentDocStatusMap.value[a.studentNo]
    const statusB = studentDocStatusMap.value[b.studentNo]
    const orderA = statusA?.order || 1
    const orderB = statusB?.order || 1
    if (orderA !== orderB) return orderA - orderB
    return a.studentNo?.localeCompare(b.studentNo)
  })
})

function getDocStatus(taskDoc, guideDoc) {
  const hasTask = !!(taskDoc && taskDoc.status === 2)
  const hasGuide = !!(guideDoc && guideDoc.status === 2)
  if (hasTask && hasGuide) return { status: '已录入', type: 'success', order: 3 }
  if (hasTask || hasGuide) return { status: '部分录入', type: 'warning', order: 2 }
  return { status: '未录入', type: 'info', order: 1 }
}

async function fetchStudents() {
  loading.value = true
  try {
    const superviseRes = await getTeacherStudents(userStore.username, '指导')
    students.value = superviseRes
    const statusMap = {}
    await Promise.all(superviseRes.map(async (student) => {
      try {
        const [taskRes, guideRes] = await Promise.all([
          getStudentDocuments(student.studentNo, '任务书'),
          getStudentDocuments(student.studentNo, '指导书')
        ])
        const taskDoc = taskRes && taskRes.length > 0 ? taskRes[0] : null
        const guideDoc = guideRes && guideRes.length > 0 ? guideRes[0] : null
        statusMap[student.studentNo] = getDocStatus(taskDoc, guideDoc)
      } catch (e) {
        statusMap[student.studentNo] = { status: '未提交', type: 'info', order: 1 }
      }
    }))
    studentDocStatusMap.value = statusMap
    if (route.query.studentNo) {
      const targetStudent = students.value.find(s => s.studentNo === route.query.studentNo)
      if (targetStudent) {
        await nextTick()
        handleSelectStudent(targetStudent)
      }
    }
  } catch (error) {
    ElMessage.error(error.message || '获取学生列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchDocs() {
  if (!selectedStudent.value) return
  loading.value = true
  try {
    const [taskRes, guideRes] = await Promise.all([
      getStudentDocuments(selectedStudent.value.studentNo, '任务书'),
      getStudentDocuments(selectedStudent.value.studentNo, '指导书')
    ])
    const taskDoc = taskRes && taskRes.length > 0 ? taskRes[0] : null
    const guideDoc = guideRes && guideRes.length > 0 ? guideRes[0] : null

    if (taskDoc) {
      const content = taskDoc.content || ''
      const splitIndex = content.indexOf(CONTENT_SPLIT)
      let mainContent = ''
      let basicRequirement = ''
      if (splitIndex >= 0) {
        mainContent = content.substring(0, splitIndex)
        basicRequirement = content.substring(splitIndex + CONTENT_SPLIT.length)
      } else {
        mainContent = content
      }
      Object.assign(taskForm, {
        id: taskDoc.docId,
        title: taskDoc.title || '',
        subjectCategory: taskDoc.subjectCategory || '',
        subjectType: taskDoc.subjectType || '',
        subjectNewOld: taskDoc.subjectNewOld || '',
        mainContent,
        basicRequirement
      })
    } else {
      Object.assign(taskForm, {
        id: null,
        title: '',
        subjectCategory: '',
        subjectType: '',
        subjectNewOld: '',
        mainContent: '',
        basicRequirement: ''
      })
    }

    if (guideDoc) {
      Object.assign(guideForm, {
        id: guideDoc.docId,
        content: guideDoc.content || ''
      })
    } else {
      Object.assign(guideForm, {
        id: null,
        content: ''
      })
    }
  } catch (error) {
    ElMessage.error(error.message || '获取文档失败')
  } finally {
    loading.value = false
  }
}

async function handleSelectStudent(student) {
  selectedStudent.value = student
  await fetchDocs()
}

async function handleSaveTask() {
  if (!selectedStudent.value) {
    ElMessage.warning('请先选择学生')
    return
  }
  if (!taskForm.title) {
    ElMessage.warning('请输入题目')
    return
  }
  if (!taskForm.mainContent) {
    ElMessage.warning('请输入课题研究的主要内容')
    return
  }
  if (!taskForm.basicRequirement) {
    ElMessage.warning('请输入基本要求')
    return
  }

  try {
    const content = taskForm.mainContent + CONTENT_SPLIT + taskForm.basicRequirement
    const data = {
      studentNo: selectedStudent.value.studentNo,
      docType: '任务书',
      title: taskForm.title,
      subjectCategory: taskForm.subjectCategory,
      subjectType: taskForm.subjectType,
      subjectNewOld: taskForm.subjectNewOld,
      content: content,
      status: 2
    }

    if (taskForm.id) {
      await updateDocument(taskForm.id, data)
    } else {
      const res = await saveDocument(data)
      taskForm.id = res
    }
    ElMessage.success('任务书保存成功')
    fetchDocs()
    await loadStudentDocStatus(selectedStudent.value.studentNo)
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  }
}

async function handleSaveGuide() {
  if (!selectedStudent.value) {
    ElMessage.warning('请先选择学生')
    return
  }
  if (!guideForm.content) {
    ElMessage.warning('请输入指导书内容')
    return
  }

  try {
    const data = {
      studentNo: selectedStudent.value.studentNo,
      docType: '指导书',
      content: guideForm.content,
      status: 2
    }

    if (guideForm.id) {
      await updateDocument(guideForm.id, data)
    } else {
      const res = await saveDocument(data)
      guideForm.id = res
    }
    ElMessage.success('指导书保存成功')
    fetchDocs()
    await loadStudentDocStatus(selectedStudent.value.studentNo)
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  }
}

onMounted(fetchStudents)
</script>

<template>
  <div v-loading="loading">
    <el-page-header title="任务书/指导书填写" />
    <el-card v-if="!loading && students.length === 0" class="table-card empty-card">
      <el-empty description="您没有需要填写任务书/指导书的学生" />
    </el-card>
    <el-card v-else class="table-card">
      <el-row :gutter="16">
        <el-col :span="5" class="left-col">
          <div class="student-list-header">
            <el-input v-model="keyword" placeholder="搜索学号/姓名" clearable @input="filterStudents" />
          </div>
          <div class="student-list">
            <div v-for="student in filteredStudents" :key="student.studentNo" class="student-item"
              :class="{ active: selectedStudent?.studentNo === student.studentNo }"
              @click="handleSelectStudent(student)">
              <div class="student-item-header">
                <div class="student-name">{{ student.studentName }}</div>
                <el-tag :type="studentDocStatusMap[student.studentNo]?.type || 'info'" size="small" effect="light">
                  {{ studentDocStatusMap[student.studentNo]?.status || '未提交' }}
                </el-tag>
              </div>
              <div class="student-no">{{ student.studentNo }}</div>
            </div>
            <el-empty v-if="!filteredStudents.length" description="暂无匹配学生" />
          </div>
        </el-col>
        <el-col :span="19" class="right-col">
          <div v-if="selectedStudent" class="detail-panel">
            <div class="detail-header">
              <h3>{{ selectedStudent.studentName }}（{{ selectedStudent.studentNo }}）</h3>
              <div class="header-info" style="margin-top: 8px;">
                <span>专业：{{ selectedStudent.major || '-' }}</span>
                <span>班级：{{ selectedStudent.className || '-' }}</span>
              </div>
            </div>

            <div class="form-section">
              <h4>任务书</h4>
              <el-form label-width="130px">
                <el-form-item label="题目" required>
                  <el-input v-model="taskForm.title" placeholder="请输入毕业设计（论文）题目" />
                </el-form-item>
                <el-form-item label="课题类别">
                  <el-select v-model="taskForm.subjectCategory" placeholder="请选择课题类别" clearable style="width: 100%;">
                    <el-option v-for="opt in categoryOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="课题类型">
                  <el-select v-model="taskForm.subjectType" placeholder="请选择课题类型" clearable style="width: 100%;">
                    <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="新旧课题">
                  <el-select v-model="taskForm.subjectNewOld" placeholder="请选择新旧课题" clearable style="width: 100%;">
                    <el-option v-for="opt in newOldOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="课题研究的主要内容" required>
                  <el-input v-model="taskForm.mainContent" type="textarea" :rows="10"
                    placeholder="请输入课题研究的主要内容，包括研究内容、拟解决的问题、预期成果等" />
                </el-form-item>
                <el-form-item label="基本要求" required>
                  <el-input v-model="taskForm.basicRequirement" type="textarea" :rows="8"
                    placeholder="请输入毕业设计（论文）的基本要求，包括对学生的能力要求、成果要求、进度要求等" />
                </el-form-item>
              </el-form>
              <div class="form-actions">
                <el-button type="primary" @click="handleSaveTask">保存任务书</el-button>
              </div>
            </div>

            <div class="form-section" style="margin-top: 20px;">
              <h4>指导书</h4>
              <el-form label-width="100px">
                <el-form-item label="指导书内容" required>
                  <el-input v-model="guideForm.content" type="textarea" :rows="12" placeholder="请输入指导书内容" />
                </el-form-item>
              </el-form>
              <div class="form-actions">
                <el-button type="primary" @click="handleSaveGuide">保存指导书</el-button>
              </div>
            </div>
          </div>
          <div v-else class="empty-select">
            <span>请选择左侧学生填写任务书/指导书</span>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<style scoped>
.table-card {
  margin-top: 16px;
}

.empty-card {
  height: calc(100vh - 150px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.student-list-header {
  margin-bottom: 12px;
}

.left-col {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 220px);
}

.right-col {
  padding-left: 16px;
  height: calc(100vh - 220px);
}

.student-list {
  flex: 1;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.student-item {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.2s;
}

.student-item:hover {
  background-color: #f5f7fa;
}

.student-item.active {
  background-color: #ecf5ff;
  border-left: 3px solid #409eff;
}

.student-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.student-name {
  font-weight: 500;
  color: #303133;
}

.student-no {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.detail-panel {
  height: calc(100vh - 220px);
  overflow-y: auto;
  padding-right: 20px;
}

.detail-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
  padding-left: 12px;
}

.detail-header h3 {
  margin: 0;
  font-size: 20px;
  color: #303133;
  font-weight: 600;
}

.header-info {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  color: #606266;
  font-size: 14px;
  margin-top: 6px;
}

.form-section {
  padding: 20px;
  background-color: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.form-section h4 {
  margin: 0 0 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #409eff;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.form-actions {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}

.empty-select {
  height: calc(100vh - 220px);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 14px;
  background-color: #fafafa;
  border-radius: 4px;
}
</style>

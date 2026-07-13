<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user.js'
import { getScoreRecordTodo, addScoreRecord, updateScoreRecord } from '@/api/scoreRecord.js'

const userStore = useUserStore()
const loading = ref(false)
const tableData = ref([])
const activeItemType = ref('')

const itemTypeConfigs = {
  '开题报告成绩': { label: '开题报告成绩', max: 4, count: 3, hasComment: false, labels: ['调研资料的获取能力', '课题方案设计的合理性', '开题报告的规范性与质量'] },
  '外文翻译': { label: '外文翻译成绩', max: 1, count: 3, hasComment: false, labels: ['对外文资料的阅读理解能力', '专业词语翻译的准确性', '译文规范性与质量'] },
  '中期检查成绩': { label: '中期检查成绩', max: 5, count: 3, hasComment: true, labels: ['完成毕业设计进度情况', '综合能力', '已完成的部分毕业设计质量'] },
  '指导评语': { label: '指导教师评语', max: 3, count: 5, hasComment: true, labels: ['设计（实验）方案、研究方案及软硬件方案设计能力', '基本概念、基本理论的应用能力', '分析问题、解决问题及知识综合运用能力', '科学素养、学习态度、纪律表现', '工作量及毕业设计（论文）规范与质量'] },
  '评阅评语': { label: '评阅教师评语', max: 4, count: 4, hasComment: true, maxList: [4, 4, 4, 3], labels: ['毕业设计（论文）规范性与质量', '基本理论和基本知识运用情况', '研究方案及设计方案', '毕业设计（论文）创新性'] },
  '答辩记录': { label: '答辩记录', max: 0, count: 0, hasComment: true },
  '毕业答辩成绩': { label: '毕业答辩成绩', max: 10, count: 4, hasComment: true, labels: ['毕业设计（论文）陈述情况', '毕业设计（论文）水平', '毕业设计（论文）工作量评价', '答辩情况'] }
}

const dialogVisible = ref(false)
const currentConfig = ref(null)
const formRef = ref(null)
const form = reactive({
  recordId: null,
  studentNo: '',
  studentName: '',
  itemType: '',
  subScores: [],
  score: null,
  grade: '',
  comment: ''
})

const gradeOptions = ['优', '良', '中', '及格', '不及格']

async function fetchData() {
  loading.value = true
  try {
    const res = await getScoreRecordTodo(userStore.username, 'teacher')
    tableData.value = res || []
  } finally {
    loading.value = false
  }
}

const filteredData = computed(() => {
  if (!activeItemType.value) return tableData.value
  return tableData.value.filter(item => item.itemType === activeItemType.value)
})

const itemTypeOptions = computed(() => {
  const types = [...new Set(tableData.value.map(item => item.itemType))]
  return types.map(type => ({ label: itemTypeConfigs[type]?.label || type, value: type }))
})

function getMax(index) {
  if (!currentConfig.value) return 100
  if (currentConfig.value.maxList) {
    return currentConfig.value.maxList[index] || currentConfig.value.max
  }
  return currentConfig.value.max
}

function handleEntry(row) {
  currentConfig.value = itemTypeConfigs[row.itemType]
  if (!currentConfig.value) {
    ElMessage.error('未知条目类型')
    return
  }

  form.recordId = row.recordId || null
  form.studentNo = row.studentNo
  form.studentName = row.studentName
  form.itemType = row.itemType
  form.score = row.score || null
  form.grade = row.grade || ''
  form.comment = row.comment || ''

  if (row.subScores) {
    form.subScores = row.subScores.split(',').map(Number)
  } else {
    form.subScores = Array(currentConfig.value.count).fill(0)
  }

  dialogVisible.value = true
}

function calculateScore() {
  if (!currentConfig.value || currentConfig.value.count === 0) return
  const total = form.subScores.reduce((sum, val) => sum + (Number(val) || 0), 0)
  form.score = total
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    const data = {
      studentNo: form.studentNo,
      itemType: form.itemType,
      subScores: currentConfig.value.count > 0 ? form.subScores.join(',') : '',
      score: form.score,
      grade: form.grade,
      comment: form.comment
    }

    if (form.recordId) {
      await updateScoreRecord(form.recordId, data)
      ElMessage.success('修改成功')
    } else {
      await addScoreRecord(data, userStore.username)
      ElMessage.success('录入成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

onMounted(fetchData)
</script>

<template>
  <div>
    <el-page-header title="成绩/评语录入" :icon="null" />
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <el-select v-model="activeItemType" placeholder="全部条目类型" clearable style="width: 200px;">
            <el-option v-for="item in itemTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
      </template>

      <el-table v-loading="loading" :data="filteredData" border>
        <el-table-column prop="itemType" label="条目类型" min-width="120" />
        <el-table-column prop="studentNo" label="学号" min-width="120" />
        <el-table-column prop="studentName" label="姓名" min-width="100" />
        <el-table-column prop="score" label="当前成绩" width="100" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEntry(row)">录入</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="currentConfig?.label" width="600px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="学生">
          <span>{{ form.studentNo }} - {{ form.studentName }}</span>
        </el-form-item>
        <el-form-item v-if="currentConfig?.count > 0" label="分项成绩">
          <div class="sub-scores">
            <div v-for="(score, index) in form.subScores" :key="index" class="score-item">
              <span>{{ currentConfig.labels ? currentConfig.labels[index] : `第${index + 1}项` }}</span>
              <el-input-number v-model="form.subScores[index]" :min="0" :max="getMax(index)" :precision="0"
                @change="calculateScore" />
              <span class="max-score">/{{ getMax(index) }}</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item v-if="currentConfig?.count > 0" label="总成绩">
          <el-input-number v-model="form.score" :min="0" :max="100" :precision="1" />
        </el-form-item>
        <el-form-item v-if="form.itemType === '委员会评定'" label="评定等级">
          <el-select v-model="form.grade" placeholder="请选择等级" style="width: 100%;">
            <el-option v-for="g in gradeOptions" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="currentConfig?.hasComment" label="评语/记录">
          <el-input v-model="form.comment" type="textarea" :rows="6" placeholder="请输入评语或记录内容" maxlength="2000"
            show-word-limit />
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

.sub-scores {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.score-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.max-score {
  color: #909399;
}
</style>

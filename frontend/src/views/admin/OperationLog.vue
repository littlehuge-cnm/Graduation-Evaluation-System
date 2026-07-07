<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getOperationLogList } from '@/api/operationLog.js'

const loading = ref(false)
const tableData = ref([])
const filters = reactive({
  userType: '',
  userNo: '',
  startTime: '',
  endTime: ''
})
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const userTypeOptions = [
  { label: '管理员', value: 'admin' },
  { label: '教师', value: 'teacher' },
  { label: '学生', value: 'student' }
]

async function fetchData() {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      userType: filters.userType || undefined,
      userNo: filters.userNo || undefined,
      startTime: filters.startTime || undefined,
      endTime: filters.endTime || undefined
    }
    const res = await getOperationLogList(params)
    tableData.value = res.records || []
    pagination.total = res.total || 0
  } catch (error) {
    ElMessage.error(error.message || '查询失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  fetchData()
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
    <el-page-header title="操作日志" />
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <div class="search-bar">
            <el-select v-model="filters.userType" placeholder="用户类型" clearable style="width: 140px;">
              <el-option v-for="item in userTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-input v-model="filters.userNo" placeholder="用户账号" clearable style="width: 160px;" @keyup.enter="handleSearch" />
            <el-date-picker
              v-model="filters.startTime"
              type="date"
              placeholder="开始日期"
              value-format="YYYY-MM-DD"
              style="width: 150px;"
            />
            <el-date-picker
              v-model="filters.endTime"
              type="date"
              placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 150px;"
            />
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="userType" label="用户类型" width="100" />
        <el-table-column prop="userNo" label="用户账号" min-width="120" />
        <el-table-column prop="operationType" label="操作类型" min-width="120" />
        <el-table-column prop="operationDesc" label="操作描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="operationTime" label="操作时间" min-width="160" />
        <el-table-column prop="ipAddress" label="IP地址" min-width="140" />
      </el-table>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>
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

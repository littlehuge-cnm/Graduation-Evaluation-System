<script setup>
import { useUserStore } from '@/stores/user.js'

const userStore = useUserStore()
</script>

<template>
  <div>
    <el-page-header title="学生中心" />
    <el-row :gutter="16" class="dashboard-cards">
      <el-col :xs="24" :sm="12" :md="8">
        <el-card>
          <div class="card-title">欢迎</div>
          <div class="card-value">{{ userStore.name }}（{{ userStore.username }}）</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="feature-card">
      <template #header>
        <span>功能说明</span>
      </template>
      <p>学生可查看个人毕业设计进度、各环节成绩与评语、任务书/指导书内容，并导出个人评价手册。</p>
      <p>后续可在左侧菜单扩展各模块入口。</p>
    </el-card>
  </div>
</template>

<style scoped>
.dashboard-cards {
  margin: 16px 0;
}

.card-title {
  color: #909399;
  font-size: 14px;
}

.card-value {
  margin-top: 8px;
  font-size: 20px;
  font-weight: 500;
  color: #303133;
}

.feature-card {
  margin-top: 16px;
}
</style>

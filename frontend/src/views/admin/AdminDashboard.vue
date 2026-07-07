<script setup>
import { useUserStore } from '@/stores/user.js'

const userStore = useUserStore()
</script>

<template>
  <div>
    <el-page-header title="超级管理员控制台" />
    <el-row :gutter="16" class="dashboard-cards">
      <el-col :xs="24" :sm="12" :md="8">
        <el-card>
          <div class="card-title">欢迎使用</div>
          <div class="card-value">{{ userStore.name }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="feature-card">
      <template #header>
        <span>功能说明</span>
      </template>
      <p>超级管理员可进行学生管理、教师管理、分组管理、环节对应关系维护、师生关系维护及评价手册批量导出。</p>
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

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user.js'

const userStore = useUserStore()

const identityTags = computed(() => {
  return userStore.identities.map((identity) => {
    const typeMap = {
      '组长': 'danger',
      '秘书': 'warning',
      '指导教师': 'success',
      '评阅教师': 'primary',
      '普通成员': 'info'
    }
    return { name: identity, type: typeMap[identity] || 'info' }
  })
})
</script>

<template>
  <div>
    <el-page-header title="教师工作台" />
    <el-row :gutter="16" class="dashboard-cards">
      <el-col :xs="24" :sm="12">
        <el-card>
          <div class="card-title">当前教师</div>
          <div class="card-value">{{ userStore.name }}（{{ userStore.username }}）</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-card>
          <div class="card-title">我的身份</div>
          <div class="identity-list">
            <el-tag
              v-for="tag in identityTags"
              :key="tag.name"
              :type="tag.type"
              size="large"
              effect="dark"
            >
              {{ tag.name }}
            </el-tag>
            <span v-if="identityTags.length === 0" class="empty-text">暂无身份</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="feature-card">
      <template #header>
        <span>功能说明</span>
      </template>
      <p>教师工作台将根据您的身份动态展示可操作模块：</p>
      <ul>
        <li><strong>组长</strong>：录入开题成绩、中期检查成绩与评语、答辩小组成绩与评语</li>
        <li><strong>秘书</strong>：录入答辩记录</li>
        <li><strong>指导教师</strong>：填写任务书/指导书、录入外文翻译成绩、填写指导教师评语</li>
        <li><strong>评阅教师</strong>：填写评阅教师评语与成绩</li>
      </ul>
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

.identity-list {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-text {
  color: #909399;
}

.feature-card {
  margin-top: 16px;
}

.feature-card ul {
  line-height: 2;
}
</style>

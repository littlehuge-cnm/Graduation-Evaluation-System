<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import { logout } from '@/api/auth.js'
import {
  User,
  Reading,
  Histogram,
  SwitchButton,
  UserFilled,
  School,
  OfficeBuilding,
  Collection,
  Connection,
  Notebook,
  Medal,
  EditPen,
  Document,
  Management,
  FirstAidKit,
  Timer
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const adminMenus = [
  { index: '/admin/dashboard', title: '控制台', icon: Histogram },
  {
    title: '人员管理',
    icon: UserFilled,
    children: [
      { index: '/admin/students', title: '学生管理', icon: User },
      { index: '/admin/teachers', title: '教师管理', icon: School },
      { index: '/admin/management', title: '管理员管理', icon: Management }
    ]
  },
  {
    title: '分组管理',
    icon: OfficeBuilding,
    children: [
      { index: '/admin/teacher-students', title: '教师分配', icon: Connection },
      { index: '/admin/group-mappings', title: '环节分配', icon: FirstAidKit },
      { index: '/admin/student-groups', title: '学生分组', icon: Collection },
      { index: '/admin/teacher-groups', title: '教师分组', icon: OfficeBuilding }
    ]
  },
  {
    title: '任务管理',
    icon: Notebook,
    children: [
      { index: '/admin/manual-review', title: '评价手册查看', icon: Document },
      { index: '/admin/committee-evaluation', title: '答辩委员会评定', icon: Medal }
    ]
  }
]

const teacherMenus = [
  { index: '/teacher/dashboard', title: '控制台', icon: Reading },
  { index: '/teacher/profile', title: '个人信息', icon: User },
  {
    title: '指导任务',
    icon: EditPen,
    children: [
      { index: '/teacher/supervise/manual-review', title: '评价手册查看', icon: Document },
      { index: '/teacher/supervise/documents', title: '任务书/指导书填写', icon: Notebook },
      { index: '/teacher/supervise/translation', title: '外文翻译评定', icon: Document },
      { index: '/teacher/supervise/comment', title: '指导教师评定', icon: Medal }
    ]
  },
  {
    title: '评阅任务',
    icon: FirstAidKit,
    children: [
      { index: '/teacher/review/comment', title: '评阅教师评定', icon: Medal }
    ]
  },
  {
    title: '答辩任务',
    icon: OfficeBuilding,
    children: [
      { index: '/teacher/group/opening', title: '开题报告评定', icon: Connection },
      { index: '/teacher/group/midterm', title: '中期检查评定', icon: Timer },
      { index: '/teacher/group/defense', title: '毕业答辩评定', icon: Medal }
    ]
  }
]

const studentMenus = [
  { index: '/student/dashboard', title: '我的毕业设计', icon: User }
]

const menus = computed(() => {
  if (userStore.isAdmin) return adminMenus
  if (userStore.isTeacher) return teacherMenus
  if (userStore.isStudent) return studentMenus
  return []
})

const title = computed(() => {
  if (userStore.isAdmin) return '超级管理员后台'
  if (userStore.isTeacher) return '教师工作台'
  if (userStore.isStudent) return '学生中心'
  return '毕业设计评价系统'
})

async function handleLogout() {
  try {
    await logout()
  } finally {
    userStore.clearUser()
    router.push('/login')
  }
}

function handleSelect(index) {
  router.push(index)
}
</script>

<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="layout-aside">
      <div class="logo">
        <span>毕业设计评价系统</span>
      </div>
      <el-menu :default-active="activeMenu" class="layout-menu" background-color="#304156" text-color="#bfcbd9"
        active-text-color="#409EFF" @select="handleSelect">
        <template v-for="menu in menus" :key="menu.index || menu.title">
          <el-sub-menu v-if="menu.children" :index="menu.title">
            <template #title>
              <el-icon>
                <component :is="menu.icon" />
              </el-icon>
              <span>{{ menu.title }}</span>
            </template>
            <el-menu-item v-for="child in menu.children" :key="child.index" :index="child.index">
              <el-icon>
                <component :is="child.icon" />
              </el-icon>
              <span>{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="menu.index">
            <el-icon>
              <component :is="menu.icon" />
            </el-icon>
            <span>{{ menu.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-title">{{ title }}</div>
        <div class="header-right">
          <span class="username">{{ userStore.name }}（{{ userStore.username }}）</span>
          <el-button type="danger" :icon="SwitchButton" link @click="handleLogout">
            退出登录
          </el-button>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-aside {
  background-color: #304156;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #1f2d3d;
}

.layout-menu {
  border-right: none;
}

.layout-header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-title {
  font-size: 18px;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.username {
  color: #606266;
}

.layout-main {
  background-color: #f0f2f5;
  overflow-y: auto;
}
</style>

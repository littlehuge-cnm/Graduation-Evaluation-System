import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import LoginView from '@/views/LoginView.vue'
import AppLayout from '@/components/AppLayout.vue'
import AdminDashboard from '@/views/admin/AdminDashboard.vue'
import TeacherDashboard from '@/views/teacher/TeacherDashboard.vue'
import StudentDashboard from '@/views/student/StudentDashboard.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { public: true }
  },
  {
    path: '/',
    component: AppLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => {
          const userStore = useUserStore()
          if (userStore.isAdmin) return AdminDashboard
          if (userStore.isTeacher) return TeacherDashboard
          if (userStore.isStudent) return StudentDashboard
          return AdminDashboard
        }
      },
      {
        path: 'admin/dashboard',
        name: 'AdminDashboard',
        component: AdminDashboard,
        meta: { roles: ['admin'] }
      },
      {
        path: 'teacher/dashboard',
        name: 'TeacherDashboard',
        component: TeacherDashboard,
        meta: { roles: ['teacher'] }
      },
      {
        path: 'student/dashboard',
        name: 'StudentDashboard',
        component: StudentDashboard,
        meta: { roles: ['student'] }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.public) {
    if (userStore.isLogin) {
      return next('/dashboard')
    }
    return next()
  }

  if (!userStore.isLogin) {
    return next('/login')
  }

  if (!userStore.name) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      userStore.clearUser()
      return next('/login')
    }
  }

  if (to.meta.roles && !to.meta.roles.includes(userStore.userType)) {
    return next('/dashboard')
  }

  next()
})

export default router

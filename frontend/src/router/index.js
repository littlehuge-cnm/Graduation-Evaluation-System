import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import LoginView from '@/views/LoginView.vue'
import AppLayout from '@/components/AppLayout.vue'
import AdminDashboard from '@/views/admin/AdminDashboard.vue'
import TeacherDashboard from '@/views/teacher/TeacherDashboard.vue'
import TeacherProfile from '@/views/teacher/TeacherProfile.vue'
import StudentDashboard from '@/views/student/StudentDashboard.vue'
import TeacherDocuments from '@/views/teacher/TeacherDocuments.vue'
import TeacherScoreEntry from '@/views/teacher/TeacherScoreEntry.vue'
import TeacherManualExport from '@/views/teacher/TeacherManualExport.vue'
import TeacherManualReview from '@/views/teacher/TeacherManualReview.vue'
import TeacherTranslation from '@/views/teacher/TeacherTranslation.vue'
import TeacherSuperviseComment from '@/views/teacher/TeacherSuperviseComment.vue'
import TeacherReviewComment from '@/views/teacher/TeacherReviewComment.vue'
import TeacherGroupOpening from '@/views/teacher/TeacherGroupOpening.vue'
import TeacherGroupMidterm from '@/views/teacher/TeacherGroupMidterm.vue'
import TeacherGroupDefense from '@/views/teacher/TeacherGroupDefense.vue'
import AdminManagement from '@/views/admin/AdminManagement.vue'
import TeacherManagement from '@/views/admin/TeacherManagement.vue'
import StudentManagement from '@/views/admin/StudentManagement.vue'
import TeacherGroupManagement from '@/views/admin/TeacherGroupManagement.vue'
import StudentGroupManagement from '@/views/admin/StudentGroupManagement.vue'
import GroupMappingManagement from '@/views/admin/GroupMappingManagement.vue'
import TeacherStudentManagement from '@/views/admin/TeacherStudentManagement.vue'
import StageStatusManagement from '@/views/admin/StageStatusManagement.vue'
import ManualReviewManagement from '@/views/admin/ManualReviewManagement.vue'
import CommitteeEvaluationManagement from '@/views/admin/CommitteeEvaluationManagement.vue'

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
        path: 'admin/management',
        name: 'AdminManagement',
        component: AdminManagement,
        meta: { roles: ['admin'] }
      },
      {
        path: 'admin/teachers',
        name: 'TeacherManagement',
        component: TeacherManagement,
        meta: { roles: ['admin'] }
      },
      {
        path: 'admin/students',
        name: 'StudentManagement',
        component: StudentManagement,
        meta: { roles: ['admin'] }
      },
      {
        path: 'admin/teacher-groups',
        name: 'TeacherGroupManagement',
        component: TeacherGroupManagement,
        meta: { roles: ['admin'] }
      },
      {
        path: 'admin/student-groups',
        name: 'StudentGroupManagement',
        component: StudentGroupManagement,
        meta: { roles: ['admin'] }
      },
      {
        path: 'admin/group-mappings',
        name: 'GroupMappingManagement',
        component: GroupMappingManagement,
        meta: { roles: ['admin'] }
      },
      {
        path: 'admin/teacher-students',
        name: 'TeacherStudentManagement',
        component: TeacherStudentManagement,
        meta: { roles: ['admin'] }
      },
      {
        path: 'admin/stage-status',
        name: 'StageStatusManagement',
        component: StageStatusManagement,
        meta: { roles: ['admin'] }
      },
      {
        path: 'admin/manual-review',
        name: 'ManualReviewManagement',
        component: ManualReviewManagement,
        meta: { roles: ['admin'] }
      },
      {
        path: 'admin/committee-evaluation',
        name: 'CommitteeEvaluationManagement',
        component: CommitteeEvaluationManagement,
        meta: { roles: ['admin'] }
      },
      {
        path: 'teacher/dashboard',
        name: 'TeacherDashboard',
        component: TeacherDashboard,
        meta: { roles: ['teacher'] }
      },
      {
        path: 'teacher/profile',
        name: 'TeacherProfile',
        component: TeacherProfile,
        meta: { roles: ['teacher'] }
      },
      {
        path: 'teacher/supervise/manual-review',
        name: 'TeacherManualReview',
        component: TeacherManualReview,
        meta: { roles: ['teacher'] }
      },
      {
        path: 'teacher/supervise/documents',
        name: 'TeacherDocuments',
        component: TeacherDocuments,
        meta: { roles: ['teacher'] }
      },
      {
        path: 'teacher/supervise/translation',
        name: 'TeacherTranslation',
        component: TeacherTranslation,
        meta: { roles: ['teacher'] }
      },
      {
        path: 'teacher/supervise/comment',
        name: 'TeacherSuperviseComment',
        component: TeacherSuperviseComment,
        meta: { roles: ['teacher'] }
      },
      {
        path: 'teacher/review/comment',
        name: 'TeacherReviewComment',
        component: TeacherReviewComment,
        meta: { roles: ['teacher'] }
      },
      {
        path: 'teacher/group/opening',
        name: 'TeacherGroupOpening',
        component: TeacherGroupOpening,
        meta: { roles: ['teacher'] }
      },
      {
        path: 'teacher/group/midterm',
        name: 'TeacherGroupMidterm',
        component: TeacherGroupMidterm,
        meta: { roles: ['teacher'] }
      },
      {
        path: 'teacher/group/defense',
        name: 'TeacherGroupDefense',
        component: TeacherGroupDefense,
        meta: { roles: ['teacher'] }
      },
      {
        path: 'teacher/manual-export',
        name: 'TeacherManualExport',
        component: TeacherManualExport,
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

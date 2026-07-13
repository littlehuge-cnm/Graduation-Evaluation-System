<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user.js'
import { login } from '@/api/auth.js'
import { User, Lock, UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  userType: 'admin',
  username: '',
  password: ''
})

const loading = ref(false)
const formRef = ref(null)

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data = await login({
      userType: form.userType,
      username: form.username,
      password: form.password
    })

    userStore.setToken(data.token)
    userStore.setUserType(data.userType)

    await userStore.fetchUserInfo()

    ElMessage.success('登录成功')

    if (userStore.isAdmin) {
      router.push('/admin/dashboard')
    } else if (userStore.isTeacher) {
      router.push('/teacher/dashboard')
    } else if (userStore.isStudent) {
      router.push('/student/dashboard')
    } else {
      router.push('/dashboard')
    }
  } catch (error) {
    // 错误提示已由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-content">
      <div class="login-left">
        <div class="brand-area">
          <div class="logo-icon">
            <el-icon :size="48">
              <UserFilled />
            </el-icon>
          </div>
          <h1 class="system-title">毕业设计评价系统</h1>
          <p class="system-desc">Graduation Design Evaluation System</p>
          <div class="divider"></div>

        </div>
      </div>
      <div class="login-right">
        <div class="login-form-wrapper">
          <h2 class="welcome-title">欢迎登录</h2>
          <p class="welcome-subtitle">请使用您的账号登录系统</p>

          <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin" class="login-form">
            <el-form-item>
              <el-radio-group v-model="form.userType" size="large" class="user-type-group">
                <el-radio-button label="admin">管理员</el-radio-button>
                <el-radio-button label="teacher">教师</el-radio-button>
                <el-radio-button label="student">学生</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item prop="username">
              <el-input v-model="form.username"
                :placeholder="form.userType === 'admin' ? '请输入管理员账号' : form.userType === 'teacher' ? '请输入教师工号' : '请输入学生学号'"
                size="large" :prefix-icon="User" clearable />
            </el-form-item>

            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" :prefix-icon="Lock"
                show-password clearable />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">
                登 录
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f8fafc;
  position: relative;
  overflow: hidden;
}

.login-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 6px;
  background: linear-gradient(90deg, #1e3a8a 0%, #1e40af 50%, #3b82f6 100%);
}

.login-container::after {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 800px;
  height: 800px;
  background: radial-gradient(circle, rgba(30, 64, 175, 0.03) 0%, transparent 70%);
  border-radius: 50%;
}

.login-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.login-left {
  flex: 1;
  max-width: 500px;
  padding-right: 80px;
  display: none;
}

.brand-area {
  text-align: left;
}

.logo-icon {
  width: 80px;
  height: 80px;
  border-radius: 16px;
  background: linear-gradient(135deg, #1e3a8a 0%, #1e40af 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-bottom: 32px;
  box-shadow: 0 10px 25px -5px rgba(30, 64, 175, 0.3);
}

.system-title {
  margin: 0 0 12px;
  font-size: 42px;
  font-weight: 700;
  color: #1e3a8a;
  letter-spacing: 2px;
}

.system-desc {
  margin: 0 0 24px;
  font-size: 16px;
  color: #64748b;
  letter-spacing: 1px;
}

.divider {
  width: 60px;
  height: 4px;
  background: linear-gradient(90deg, #1e40af 0%, #3b82f6 100%);
  border-radius: 2px;
  margin-bottom: 24px;
}

.system-tip {
  margin: 0;
  font-size: 15px;
  color: #475569;
  line-height: 1.6;
}

.login-right {
  width: 100%;
  max-width: 440px;
}

.login-form-wrapper {
  background: white;
  padding: 48px 40px;
  border-radius: 12px;
  box-shadow: 0 20px 40px -12px rgba(0, 0, 0, 0.1), 0 0 0 1px rgba(0, 0, 0, 0.05);
}

.welcome-title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
}

.welcome-subtitle {
  margin: 0 0 32px;
  font-size: 14px;
  color: #64748b;
}

.login-form {
  margin-top: 24px;
}

.user-type-group {
  width: 100%;
  display: flex;
}

.user-type-group :deep(.el-radio-button) {
  flex: 1;
}

.user-type-group :deep(.el-radio-button__inner) {
  width: 100%;
  padding: 12px 0;
  font-size: 15px;
  text-align: center;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  margin-top: 8px;
}

.footer-copyright {
  text-align: center;
  padding: 20px;
  color: #94a3b8;
  font-size: 13px;
}

@media (min-width: 1024px) {
  .login-left {
    display: block;
  }

  .login-content {
    gap: 60px;
  }
}

@media (max-width: 768px) {
  .login-form-wrapper {
    padding: 32px 24px;
  }

  .welcome-title {
    font-size: 24px;
  }
}
</style>

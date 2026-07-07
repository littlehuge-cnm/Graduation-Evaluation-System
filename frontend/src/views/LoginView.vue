<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user.js'
import { login } from '@/api/auth.js'

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
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <el-card class="login-card" shadow="hover">
      <template #header>
        <div class="login-header">
          <h2>毕业设计评价系统</h2>
          <p>请登录您的账号</p>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @keyup.enter="handleLogin"
      >
        <el-form-item label="用户类型">
          <el-radio-group v-model="form.userType" size="large">
            <el-radio-button label="admin">管理员</el-radio-button>
            <el-radio-button label="teacher">教师</el-radio-button>
            <el-radio-button label="student">学生</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="账号" prop="username">
          <el-input
            v-model="form.username"
            :placeholder="form.userType === 'admin' ? '管理员账号' : form.userType === 'teacher' ? '工号' : '学号'"
            size="large"
            clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%;"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  max-width: 90%;
  border-radius: 12px;
}

.login-header {
  text-align: center;
}

.login-header h2 {
  margin: 0 0 8px;
  color: #303133;
}

.login-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}
</style>

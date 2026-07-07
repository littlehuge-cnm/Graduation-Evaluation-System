import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUserInfo } from '@/api/auth.js'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userType = ref(localStorage.getItem('userType') || '')
  const username = ref('')
  const name = ref('')
  const identities = ref([])

  const isLogin = computed(() => !!token.value)
  const isAdmin = computed(() => userType.value === 'admin')
  const isTeacher = computed(() => userType.value === 'teacher')
  const isStudent = computed(() => userType.value === 'student')

  function setToken(value) {
    token.value = value
    localStorage.setItem('token', value)
  }

  function setUserType(value) {
    userType.value = value
    localStorage.setItem('userType', value)
  }

  function clearUser() {
    token.value = ''
    userType.value = ''
    username.value = ''
    name.value = ''
    identities.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('userType')
  }

  async function fetchUserInfo() {
    const data = await getUserInfo()
    userType.value = data.userType || userType.value
    username.value = data.username || ''
    name.value = data.name || ''
    identities.value = data.identities || []
    return data
  }

  return {
    token,
    userType,
    username,
    name,
    identities,
    isLogin,
    isAdmin,
    isTeacher,
    isStudent,
    setToken,
    setUserType,
    clearUser,
    fetchUserInfo
  }
})

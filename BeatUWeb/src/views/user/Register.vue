<template>
  <div class="user-page">
    <el-card class="form-card" shadow="hover">
      <template #header><div class="card-header">用户注册</div></template>
      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="onSubmit">
        <el-form-item prop="userName">
          <el-input v-model="form.userName" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="onSubmit">注册</el-button>
        </el-form-item>
      </el-form>
      <div class="footer">
        <router-link to="/login">已有账号？去登录</router-link>
        <router-link to="/" style="margin-left:16px">返回首页</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { register } from '@/api/auth'

const router = useRouter()
const auth = useAuthStore()
const form = reactive({ userName: '', password: '', confirmPassword: '' })
const formRef = ref(null)
const loading = ref(false)

const validateConfirm = (rule, value, cb) => {
  if (value !== form.password) cb(new Error('两次密码不一致'))
  else cb()
}

const rules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: validateConfirm, trigger: 'blur' }],
}

const onSubmit = async () => {
  await formRef.value?.validate().catch(() => {})
  loading.value = true
  try {
    const data = await register({ userName: form.userName, password: form.password })
    auth.setAuth(data)
    ElMessage.success('注册成功')
    router.push('/')
  } catch (_) {}
  finally { loading.value = false }
}
</script>

<style scoped>
.user-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.form-card { width: 380px; }
.card-header { font-size: 20px; font-weight: 600; text-align: center; }
.footer { text-align: center; margin-top: 12px; }
.footer a { color: #409EFF; text-decoration: none; }
</style>

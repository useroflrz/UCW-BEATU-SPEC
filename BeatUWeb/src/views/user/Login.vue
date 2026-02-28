<template>
  <div class="user-page">
    <el-card class="form-card" shadow="hover">
      <template #header><div class="card-header">用户登录</div></template>
      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="onSubmit">
        <el-form-item prop="userName">
          <el-input v-model="form.userName" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="onSubmit">登录</el-button>
        </el-form-item>
      </el-form>
      <div class="footer">
        <router-link to="/register">没有账号？去注册</router-link>
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
import { login } from '@/api/auth'

const router = useRouter()
const auth = useAuthStore()
const form = reactive({ userName: '', password: '' })
const formRef = ref(null)
const loading = ref(false)
const rules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const onSubmit = async () => {
  await formRef.value?.validate().catch(() => {})
  loading.value = true
  try {
    const data = await login(form)
    auth.setAuth(data)
    ElMessage.success('登录成功')
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

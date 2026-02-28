<template>
  <div>
    <h2>用户管理</h2>
    <el-table :data="paginatedList" v-loading="loading" stripe style="margin-top:16px">
      <el-table-column prop="userId" label="用户ID" width="180" />
      <el-table-column prop="userName" label="用户名" width="150" />
      <el-table-column prop="name" label="昵称" />
      <el-table-column prop="bio" label="简介" show-overflow-tooltip />
      <el-table-column prop="followingCount" label="关注" width="80" align="right" />
      <el-table-column prop="followersCount" label="粉丝" width="80" align="right" />
    </el-table>
    <el-pagination
      v-model:current-page="page"
      :page-size="limit"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top:16px"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { listUsers } from '@/api/user'

const list = ref([])
const loading = ref(false)
const page = ref(1)
const limit = 20
const total = computed(() => list.value.length)
const paginatedList = computed(() => {
  const start = (page.value - 1) * limit
  return list.value.slice(start, start + limit)
})

const fetch = async () => {
  loading.value = true
  try {
    const res = await listUsers()
    list.value = Array.isArray(res) ? res : (res?.items ?? [])
  } catch (_) {}
  finally { loading.value = false }
}

onMounted(fetch)
</script>

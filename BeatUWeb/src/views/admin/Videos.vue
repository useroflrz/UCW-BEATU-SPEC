<template>
  <div>
    <h2>视频管理</h2>
    <el-table :data="list" v-loading="loading" stripe style="margin-top:16px">
      <el-table-column prop="id" label="ID" width="80" align="right" />
      <el-table-column label="封面" width="100">
        <template #default="{ row }">
          <el-image v-if="row.coverUrl" :src="row.coverUrl" fit="cover" style="width:80px;height:45px;border-radius:4px" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" show-overflow-tooltip />
      <el-table-column prop="authorName" label="作者" width="120" />
      <el-table-column prop="orientation" label="方向" width="80" />
      <el-table-column prop="likeCount" label="点赞" width="70" align="right" />
      <el-table-column prop="commentCount" label="评论" width="70" align="right" />
    </el-table>
    <el-pagination
      v-model:current-page="page"
      :page-size="limit"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="fetch"
      style="margin-top:16px"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listVideos } from '@/api/video'

const list = ref([])
const loading = ref(false)
const page = ref(1)
const limit = 20
const total = ref(0)

const fetch = async () => {
  loading.value = true
  try {
    const res = await listVideos({ page: page.value, limit })
    list.value = res?.items ?? []
    total.value = res?.total ?? 0
  } catch (_) {}
  finally { loading.value = false }
}

onMounted(fetch)
</script>

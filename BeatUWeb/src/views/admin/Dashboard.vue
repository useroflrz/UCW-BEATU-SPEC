<template>
  <div>
    <h2>仪表盘</h2>
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>用户</template>
          <div class="stat-value">{{ stats.users }}</div>
          <div class="stat-label">用户总数</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>视频</template>
          <div class="stat-value">{{ stats.videos }}</div>
          <div class="stat-label">视频总数</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>状态</template>
          <div class="stat-value">
            <el-tag type="success">运行中</el-tag>
          </div>
          <div class="stat-label">系统状态</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listUsers } from '@/api/user'
import { listVideos } from '@/api/video'

const stats = ref({ users: '-', videos: '-' })

onMounted(async () => {
  try {
    const [u, v] = await Promise.all([
      listUsers(),
      listVideos({ page: 1, limit: 1 }),
    ])
    stats.value.users = Array.isArray(u) ? u.length : (u?.total ?? '-')
    stats.value.videos = v?.total ?? '-'
  } catch (_) {}
})
</script>

<style scoped>
.stat-value { font-size: 28px; font-weight: 600; color: #303133; }
.stat-label { font-size: 14px; color: #909399; margin-top: 8px; }
</style>

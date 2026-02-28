<template>
  <div class="user-home">
    <header class="home-header">
      <h1>BeatU</h1>
      <p class="slogan">沉浸式短视频</p>
      <div class="actions">
        <el-button type="primary" size="large" @click="$router.push('/login')">登录</el-button>
        <el-button size="large" @click="$router.push('/register')">注册</el-button>
        <el-button type="info" size="large" @click="$router.push('/admin/login')">管理后台</el-button>
      </div>
    </header>
    <section class="intro">
      <p>推荐使用 BeatU Android 客户端获得最佳体验。</p>
      <p>管理员可在此登录后台进行用户与视频管理。</p>
    </section>
  </div>
</template>

<script setup>
</script>

<style scoped>
.user-home {
  min-height: 100vh;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  color: #fff;
  padding: 60px 20px;
}
.home-header { text-align: center; }
.home-header h1 { font-size: 48px; margin: 0; }
.slogan { font-size: 18px; color: #a0aec0; margin: 12px 0 32px; }
.actions { display: flex; gap: 16px; justify-content: center; flex-wrap: wrap; }
.intro {
  max-width: 500px;
  margin: 60px auto 0;
  text-align: center;
  color: #a0aec0;
  font-size: 14px;
  line-height: 1.8;
}
</style>

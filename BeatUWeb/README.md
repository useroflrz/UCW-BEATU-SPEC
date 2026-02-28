# BeatUWeb

BeatU 前端项目，主要供管理员使用，其次供用户在 Web 端访问。

## 功能

### 管理员（主要）

- 登录
- 仪表盘：概览统计
- 用户管理：用户列表、查看详情
- 视频管理：视频列表、查看详情

### 用户（次要）

- 首页：展示与入口
- 登录 / 注册
- 个人中心（可扩展）

## 启动

```bash
# 安装依赖
npm install

# 开发
npm run dev

# 构建
npm run build

# 预览构建结果
npm run preview
```

默认端口 5174。API 代理到 `http://localhost:9306`，需确保 BeatUBackend 已启动。

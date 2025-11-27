## BeatU Backend 快速启动

### 1. 环境要求

- Python 3.11+
- MySQL 8.x（本地示例使用 `beatu_content` 数据库，账号 `root` / 密码 `2218502641`）
- 可选：Redis 7.x（当前功能未强制依赖）

### 2. 配置数据库

1. 创建数据库：
   ```sql
   CREATE DATABASE IF NOT EXISTS beatu_content CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. 确保用户 `root` 能以密码 `2218502641` 访问 `localhost:3306`。
3. 如需修改凭证，请在 `.env` 中覆盖 `DATABASE_URL`（示例：`mysql+pymysql://root:2218502641@localhost:3306/beatu_content`）。

> `core/config.py` 默认已经指向上述 MySQL 实例；未提供 `.env` 也会连接此库。

### 3. 安装依赖

```bash
cd BeatUBackend
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
```

### 4. 初始化表结构

```bash
python -m database.init_db --drop
```

如需使用 MySQL 原生脚本，可执行 `BeatUContentService/sql/init_schema.sql`。

### 5. 运行服务

```bash
uvicorn main:app --reload
```

访问 `http://127.0.0.1:8000/docs` 查看 Swagger，并使用 `/api/...` 系列接口。

### 6. 常见问题

- **连接失败**：确认 MySQL 服务已启动、端口 3306 未被占用。
- **依赖缺失**：再次执行 `pip install -r requirements.txt`，确保安装了 `pymysql`。
- **数据库不存在**：手动执行上面的 `CREATE DATABASE` 语句或通过 MySQL 客户端创建。



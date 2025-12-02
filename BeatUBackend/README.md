## BeatU Backend 快速启动

### 1. 环境要求

- Python 3.11+
- MySQL 8.x（使用 `jeecg-boot3` 数据库，服务器 `192.168.1.206:3306`，账号 `jeecg` / 密码 `haomo123`）
- 可选：Redis 7.x（当前功能未强制依赖）

### 2. 配置数据库

1. 数据库信息（默认值）：
   - 数据库名：`jeecg-boot3`
   - 服务器：`192.168.1.206:3306`
   - 用户名：`jeecg`
   - 密码：`haomo123`
2. 确保数据库 `jeecg-boot3` 已存在，如果不存在请先创建：
   ```sql
   CREATE DATABASE IF NOT EXISTS `jeecg-boot3` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. 确保用户 `jeecg` 有访问 `jeecg-boot3` 数据库的权限。

### 2.1 配置文件设置

项目使用 `.env` 文件管理配置。首次使用需要：

1. 复制配置文件模板：
   ```bash
   cd BeatUBackend
   cp .env.example .env
   ```

2. 编辑 `.env` 文件，修改数据库连接等配置：
   ```bash
   # 编辑 .env 文件
   DATABASE_URL=mysql+pymysql://jeecg:haomo123@192.168.1.206:3306/jeecg-boot3
   ```

3. 配置说明：
   - 配置优先级：环境变量 > `.env` 文件 > 默认值
   - 详细配置说明请参考 [CONFIG.md](CONFIG.md)
   - `.env` 文件包含敏感信息，已加入 `.gitignore`，不会提交到版本控制

> **注意**：如果 `.env` 文件不存在，将使用 `core/config.py` 中的默认值。

### 3. 创建 Conda 虚拟环境并安装依赖

**重要**：本项目**必须**使用 Conda 虚拟环境 `beatu-backend` 运行，不支持 venv 或其他虚拟环境。

#### 创建并激活环境

```bash
cd BeatUBackend

# 创建 conda 环境（首次使用）
conda env create -f environment.yml

# 激活环境（每次运行服务前都需要激活）
conda activate beatu-backend

# 验证安装
python --version  # 应该显示 Python 3.11
conda info --envs  # 确认 beatu-backend 环境已创建
```

#### 环境说明

- **环境名称**：`beatu-backend`
- **Python 版本**：3.11+
- **依赖管理**：通过 `environment.yml` 管理，包含所有必需的 Python 包
- **激活方式**：`conda activate beatu-backend`
- **退出方式**：`conda deactivate`

### 4. 初始化表结构

```bash
python -m database.init_db --drop
```

如需使用 MySQL 原生脚本，可执行 `BeatUContentService/sql/init_schema.sql`。

### 5. 运行服务

**重要**：运行服务前，**必须**先激活 conda 虚拟环境 `beatu-backend`。

```bash
# 1. 激活 conda 环境（必须步骤）
conda activate beatu-backend

# 2. 确认环境已激活（可选，用于验证）
which python  # 应该显示 conda 环境中的 python 路径
python --version  # 应该显示 Python 3.11

# 3. 运行服务（监听所有网络接口，允许手机通过 WiFi 访问）
uvicorn main:app --reload --host 0.0.0.0 --port 9306
```

**注意事项**：
- 每次打开新的终端窗口运行服务时，都需要先执行 `conda activate beatu-backend`
- 如果忘记激活环境，服务可能无法正常运行或出现依赖缺失错误
- 可以通过命令提示符前的 `(beatu-backend)` 标识确认环境已激活

**服务地址**：
- 本地访问 API 文档：http://127.0.0.1:9306/docs
- 局域网访问 API 文档：http://192.168.1.181:9306/docs（根据实际 IP 调整）
- 接口根路径：http://192.168.1.181:9306/api

**注意**：
- `--host 0.0.0.0` 允许从局域网访问服务
- 如果电脑 IP 地址变化，需要更新：
  - 客户端：`BeatUClient/app/src/main/res/values/config.xml` 中的 `base_url`
  - 或通过环境变量设置服务地址
- 确保防火墙允许 9306 端口的访问

### 6. 环境管理

#### 退出环境
```bash
conda deactivate
```

#### 删除环境（如需要）
```bash
conda env remove -n beatu-backend
```

#### 更新环境
```bash
conda env update -f environment.yml --prune
```

### 7. 常见问题

- **连接失败**：确认 MySQL 服务已启动、端口 3306 未被占用，检查 `192.168.1.206:3306` 是否可访问。
- **依赖缺失**：
  - 确认已激活 conda 环境：`conda activate beatu-backend`
  - 重新创建环境：`conda env create -f environment.yml --force`
  - 更新环境：`conda env update -f environment.yml --prune`
- **环境未激活**：
  - 错误提示：`ModuleNotFoundError` 或 `command not found: uvicorn`
  - 解决方法：执行 `conda activate beatu-backend` 激活环境
  - 验证：执行 `which python` 确认使用的是 conda 环境中的 Python
- **数据库不存在**：使用 Navicat 执行 `database/init_database.sql` 脚本初始化数据库。
- **端口被占用**：修改启动命令中的端口号，如 `--port 8001`。



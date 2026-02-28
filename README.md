# notes

基于 Scala 3 + Scala.js 的前后端工程骨架（g8 模板初始化）。

## 模块

- `shared`：前后端共享模型
- `backend`：后端服务模块（Scala 3）
- `frontend`：前端模块（Scala.js）

## 本地命令

```bash
sbt projects
sbt backend/run
sbt frontend/fastLinkJS
```

## PostgreSQL 初始化（幂等）

后端默认数据库配置：

- `NOTES_DB_URL=jdbc:postgresql://127.0.0.1:5432/notes`
- `NOTES_DB_USER=notes`
- `NOTES_DB_PASSWORD=notes`

先用管理员账户初始化（可重复执行）：

```powershell
./scripts/init-postgres.ps1 -DbHost 127.0.0.1 -Port 5432 -AdminUser postgres -AppDb notes -AppUser notes -AppPassword notes
```

Linux/macOS:

```bash
chmod +x ./scripts/init-postgres.sh
./scripts/init-postgres.sh --db-host 127.0.0.1 --db-port 5432 --admin-user postgres --app-db notes --app-user notes --app-password notes
```

该脚本会确保以下状态成立（幂等）：

- 存在 `notes` 角色，并设置登录密码
- 存在 `notes` 数据库，并设置 owner
- 存在 `notes` 表结构并授予最小读写权限

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

## 后端打包（sbt-native-packager）

生成可分发包（zip）：

```bash
sbt backend/Universal/packageBin
```

产物默认位置：

- `backend/target/universal/notes-backend-0.1.0-SNAPSHOT.zip`

## PostgreSQL 初始化（幂等）

后端默认配置位于 [application.conf](backend/src/main/resources/application.conf)，可通过环境变量覆盖。

数据库相关默认项：

- `NOTES_DB_URL=jdbc:postgresql://127.0.0.1:5432/notes`
- `NOTES_DB_USER=notes`
- `NOTES_DB_PASSWORD=notes`

HTTP 相关默认项：

- `NOTES_HTTP_HOST=0.0.0.0`
- `NOTES_HTTP_PORT=8080`

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

## Docker Compose 本地启动

前提：你已经在本地构建好后端镜像（默认镜像名 `notes-backend:local`）。

启动：

```bash
docker compose up -d
```

查看日志：

```bash
docker compose logs -f backend
```

停止并清理：

```bash
docker compose down
```

如需覆盖镜像名：

```bash
NOTES_BACKEND_IMAGE=your-image:tag docker compose up -d
```

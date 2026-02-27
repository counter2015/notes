# Note.ms 克隆版技术方案文档（Scala 全栈）

本文面向项目开发者，目标是在 [MVP 设计文档](./note-ms-mvp-design.md) 基础上明确 Scala 全栈实现的模块划分与技术选型。默认读者了解 Scala 3、HTTP 与基础前端开发；不覆盖视觉设计与部署手册细节。

## 1. 结论

- 结论 1：项目可以采用 Scala 全栈实现，前端使用 Scala.js，后端使用 Scala 3。
- 结论 2：后端可以采用 Scala Native 产出单二进制，这是可行路径。
- 结论 3：为支持多实例并发写入，存储采用共享数据库（PostgreSQL）并使用乐观锁。
- 结论 4：API 描述使用 `tapir`，JSON 使用 `circe`，构建使用 `sbt`，与个人偏好对齐。

## 2. Scala Native 可行性评估

### 2.1 可行部分

- HTTP 服务：可用 Scala Native 运行后端服务并监听端口。
- 业务逻辑：短链分配、内容保存、参数校验等纯 Scala 逻辑适合 Native。
- 静态资源：可在构建阶段把 Scala.js 产物打包进后端资源，最终由后端二进制对外提供页面与静态文件。

### 2.2 约束与取舍

- 生态约束：部分 JVM 常用库在 Scala Native 上不可用或功能不完整。
- 编译成本：Native 链接时间明显高于 JVM，开发期迭代速度更慢。
- 存储选型：引入 PostgreSQL 后，应用仍可保持单二进制发布，但系统形态不再是“零外部依赖”。

### 2.3 结论

- 如果目标是“学习 + 验证 + 最终单二进制”，Scala Native 是合理方案。
- 推荐采用“Native 后端 + Scala.js 前端 + PostgreSQL（乐观锁）”的多实例路线。

## 3. 总体架构

### 3.1 分层

- `frontend`（Scala.js）：编辑页、自动保存、状态提示。
- `backend`（Scala Native）：HTTP 路由、ID 分配、读写逻辑、静态资源服务。
- `shared`（跨端共享）：数据模型、请求/响应结构、校验规则。

### 3.2 运行形态

- 产物：一个后端 Native 可执行文件。
- 前端交付：Scala.js 编译为 JS/CSS 后随二进制一起分发。
- 启动方式：执行单个二进制即可对外提供完整服务。

## 4. 模块划分

| 模块 | 职责 | 备注 |
|---|---|---|
| `shared-model` | 笔记模型、ID 规则、错误码定义 | 跨前后端复用 |
| `shared-codec` | `circe` 编解码与表单转换 | 保证协议一致 |
| `backend-api` | `tapir` Endpoint 定义与路由挂载 | 协议先行 |
| `backend-service` | 业务规则（读取、保存、分配、冲突处理） | 不依赖传输层 |
| `backend-repo` | PostgreSQL 读写与条件更新 | 多实例共享存储 |
| `backend-web` | 静态资源与 HTML 输出 | 服务 Scala.js 产物 |
| `frontend-app` | 编辑器逻辑、事件驱动自动保存、失败重试 | 无框架或轻量框架均可 |

## 5. 技术选型

### 5.1 构建与代码质量

- 构建：`sbt`
- 格式化：`scalafmt`
- 重构/规则修复：`scalafix`

### 5.2 API 与数据格式

- API：`tapir`
- JSON：`circe`
- 表单：`application/x-www-form-urlencoded`（与目标站点行为保持一致）
- 接口文档：见 [note-ms-api-spec.md](./note-ms-api-spec.md)
- Tapir 草案清单：见 [note-ms-tapir-endpoint-draft.md](./note-ms-tapir-endpoint-draft.md)

### 5.3 前端

- 语言：Scala.js
- 方案：优先简单 DOM 操作与最小依赖，避免为 MVP 引入重型前端框架

### 5.4 后端

- 语言：Scala 3 + Scala Native
- 目标：单二进制运行

## 6. 存储方案（MVP）

### 6.1 选型结论

- 采用 PostgreSQL 作为共享存储，支持多实例部署。
- 使用乐观锁字段 `version` 处理并发写入冲突。
- 写入采用“条件更新”策略：仅在 `version` 匹配时更新成功。

### 6.2 取舍说明

- 优点：多实例一致性更好，冲突可检测，演进空间更大。
- 缺点：引入外部数据库依赖，部署复杂度上升。

### 6.3 表结构建议

```sql
create table notes (
  id          char(4) primary key,
  content     text not null default '',
  version     bigint not null default 0,
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);
```

### 6.4 乐观锁写入语义

- 读取时返回 `content` + `version`。
- 写入请求必须携带客户端持有的 `version`。
- 执行条件更新：
  - `update notes set content=?, version=version+1, updated_at=now() where id=? and version=?`
- 若更新行数为 `0`，返回 `409 Conflict`（表示并发冲突）。

## 7. 核心流程

### 7.1 `GET /` 随机分配

1. 生成 4 位 `[a-z0-9]` 候选 ID。
2. 检查对应文件是否已存在。
3. 已存在则重试，最多 8 次。
4. 成功后 302 跳转到 `/:id`。
5. 超过重试上限返回 503（`ID_POOL_BUSY`）。

约束：不得跳转到已存在文档。

### 7.2 `GET /:id` 手动输入访问

1. 校验 `id` 为 4 位 `[a-z0-9]`。
2. 若存在则读取内容与 `version` 返回页面。
3. 若不存在则返回空页面与初始 `version=0`。

### 7.3 `POST /:id` 自动保存

1. 前端监听 `input` 事件，内容变化时标记 `dirty`。
2. 通过防抖（建议 800 ms）触发保存，而非固定间隔轮询。
3. 触发保存时提交 `t=<urlencoded content>`，同时携带当前 `version`。
4. 后端执行乐观锁条件更新。
5. 成功返回 200 + 新 `version`；冲突返回 409。

### 7.4 冲突时用户感知

- 前端状态显示“保存冲突”，本地输入内容不丢失。
- 自动重试暂停，等待用户处理。
- 最小交互提供两个动作：`刷新加载最新`、`强制覆盖保存（可选）`。

### 7.5 前端自动保存状态机

- 状态：`idle` -> `dirty` -> `saving` -> `idle`。
- 标记：`pendingDirty` 用于处理“保存中再次输入”场景。
- 规则：
  - `input`：进入 `dirty`，重置防抖计时器。
  - 防抖到期：若当前非 `saving`，发起保存并进入 `saving`。
  - `saving` 期间再次 `input`：仅置 `pendingDirty=true`。
  - 保存成功：若 `pendingDirty=true`，立即进入下一轮保存；否则回到 `idle`。
  - 保存冲突（409）：进入 `conflict`，停止自动重试，等待用户动作。

### 7.6 立即保存触发点

- `blur`：输入框失焦时立即尝试保存。
- `visibilitychange`：页面切到后台前立即尝试保存。
- `beforeunload`：页面关闭前触发一次尽力保存（如 `sendBeacon`/keepalive 请求）。

## 8. 工程结构建议

```text
notes/
  build.sbt
  project/
  shared/
    src/main/scala/...
  backend/
    src/main/scala/...
    src/main/resources/public/...
  frontend/
    src/main/scala/...
  docs/
```

说明：`frontend` 产物在构建阶段复制到 `backend/src/main/resources/public/`，由后端统一分发。

## 9. 里程碑

- M1：完成 Scala.js 页面 + Native 后端最小读写链路。
- M2：完善错误处理、限流与日志指标。
- M3：优化构建流程（前端产物自动嵌入二进制）与发布脚本。

### 9.1 M1 Checklist（最小可用链路）

- [ ] 完成 `GET /` 随机分配，且不跳转到已存在文档。
- [ ] 完成 `GET /:id`，支持手动输入 URL 访问，不存在返回空页面。
- [ ] 完成 `POST /:id` 保存链路，包含 `version` 参数与乐观锁条件更新。
- [ ] 完成 Scala.js 编辑页，接入“事件驱动 + 防抖保存”。
- [ ] 完成基础冲突提示：收到 `409` 后进入冲突态并暂停自动重试。

**M1 DoD**
- [ ] 从 `/` 创建新文档可用。
- [ ] 手动输入 `/:id` 读写可用。
- [ ] 乐观锁冲突能稳定复现并被前端正确提示。

### 9.2 M2 Checklist（可用性与稳定性加固）

- [ ] 统一错误语义与错误体格式（`400/409/413/429/500/503`）。
- [ ] 增加请求体大小限制（建议 256KB）与内容长度限制（建议 200KB）。
- [ ] 增加限流策略（按 `IP + 路由`）。
- [ ] 完成结构化日志：`request_id`、`route`、`status`、`latency_ms`。
- [ ] 完成核心指标：保存成功率、冲突率、保存延迟、ID 分配重试次数。
- [ ] 完成前端失败态处理：保存失败、冲突态、用户手动恢复流程。
- [ ] 完成并发写与弱网场景回归测试。

**M2 DoD**
- [ ] 在冲突、限流、超长输入场景下，返回码与前端提示符合文档定义。
- [ ] 日志与指标可用于定位失败原因与性能瓶颈。
- [ ] 自动保存在异常场景下不丢本地输入内容。

### 9.3 M3 Checklist（交付与工程化）

- [ ] 完成前端构建产物自动嵌入后端资源目录。
- [ ] 完成单二进制构建脚本与发布脚本。
- [ ] 完成本地开发与发布命令文档（包含环境变量说明）。
- [ ] 完成容量阈值监控（短链空间使用率 80%/95% 告警）。
- [ ] 完成基础健康检查与启动自检（端口、存储连接、必要目录/配置）。

**M3 DoD**
- [ ] 通过一条构建命令可产出可运行二进制。
- [ ] 通过一条启动命令可提供完整服务（页面 + API）。
- [ ] 发布包具备最小运行文档，可由新环境独立启动。

## 10. 已知限制

- 匿名模型下，知晓 URL 即可访问内容。
- 并发写入采用乐观锁，冲突时需要用户介入处理。
- 4 位短链总空间 `36^4 = 1,679,616`，接近上限时需扩展 ID 长度。

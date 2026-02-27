# Note.ms 前后端接口文档（MVP）

本文定义前后端交互协议，面向前端与后端开发。目标是保证自动保存、乐观锁冲突处理与错误语义一致。范围仅覆盖 MVP 已确认接口，不包含历史版本、协同编辑与鉴权体系。

## 1. 协议约定

- 协议：HTTP/1.1 或 HTTP/2
- 字符编码：UTF-8
- 认证：无（匿名访问）
- `noteId` 规则：固定 4 位，字符集 `[a-z0-9]`
- 时间格式：RFC 3339（示例：`2026-02-07T10:30:12Z`）

## 2. 错误响应格式

除 `302` 之外，错误默认返回 JSON：

```json
{
  "error": {
    "code": "CONFLICT",
    "message": "note has been modified by another session",
    "requestId": "req_01JABCDEF..."
  }
}
```

字段说明：

- `code`：稳定错误码，供前端分支处理
- `message`：面向展示/日志的错误描述
- `requestId`：请求追踪 ID

## 3. 接口定义

## 3.1 `GET /`

用途：分配新的随机短链并跳转。

- 请求参数：无
- 响应：
  - `302 Found`
  - Header: `Location: /{noteId}`

约束：

- 必须分配未占用 `noteId`
- 若连续冲突达到重试上限（8 次），返回 `503`

错误：

- `503 Service Unavailable`

```json
{
  "error": {
    "code": "ID_POOL_BUSY",
    "message": "failed to allocate new note id",
    "requestId": "req_01J..."
  }
}
```

## 3.2 `GET /:id`

用途：加载笔记页面（包含初始内容与版本号）。

- Path 参数：
  - `id`：4 位 `[a-z0-9]`
- 响应：
  - `200 OK`
  - `Content-Type: text/html; charset=utf-8`

页面要求（供前端初始化）：

- 初始内容：`content`
- 当前版本：`version`（不存在时为 `0`）

错误：

- `400 Bad Request`（`id` 非法）

```json
{
  "error": {
    "code": "INVALID_ID",
    "message": "note id must match [a-z0-9]{4}",
    "requestId": "req_01J..."
  }
}
```

## 3.3 `POST /:id`

用途：自动保存（创建或更新笔记）。

- Path 参数：
  - `id`：4 位 `[a-z0-9]`
- Header：
  - `Content-Type: application/x-www-form-urlencoded; charset=UTF-8`
- Body（form）：
  - `t`：笔记内容（必填）
  - `version`：客户端当前版本（必填，`long`）

示例请求体：

```text
t=hello%20world&version=3
```

成功响应：

- `200 OK`
- `Content-Type: application/json`

```json
{
  "id": "ab12",
  "version": 4,
  "updatedAt": "2026-02-07T10:30:12Z"
}
```

冲突响应（乐观锁）：

- `409 Conflict`
- `Content-Type: application/json`

```json
{
  "error": {
    "code": "VERSION_CONFLICT",
    "message": "note has been modified by another session",
    "requestId": "req_01J..."
  },
  "current": {
    "version": 5
  }
}
```

其他错误：

- `400 Bad Request`（参数缺失或格式错误）
- `413 Payload Too Large`（内容超限）
- `429 Too Many Requests`（触发限流）
- `500 Internal Server Error`

## 4. 前端状态与接口映射

- `idle/dirty/saving`：
  - `input` 触发 `dirty`，防抖后调用 `POST /:id`
- `save success`：
  - 用响应里的 `version` 覆盖本地 `version`
- `conflict`（`409`）：
  - 进入冲突态，暂停自动重试
  - 提供动作：刷新加载最新 / 强制覆盖（后续可选）

## 5. 幂等性与并发语义

- `POST /:id` 不是传统幂等接口，但在同一 `(id, version, t)` 输入下应得到一致冲突/成功结果。
- 服务端并发控制依据：`where id = ? and version = ?`
- 更新行数为 `0` 即判定冲突，不允许静默覆盖。

## 6. 限制与默认值

- 内容上限：200 KB（超限返回 `413`）
- 请求体上限：256 KB（超限返回 `413`）
- 自动保存推荐防抖：800 ms（前端策略）
- `GET /` 分配重试上限：8 次

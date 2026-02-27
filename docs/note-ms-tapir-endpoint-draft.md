# Note.ms Tapir Endpoint 草案清单

本文给出基于 [接口文档](./note-ms-api-spec.md) 的 Tapir 草案清单，目标是把协议定义映射为可实现的 endpoint 结构。本文仅定义草案，不包含具体代码实现。

## 1. 结论

- 先定义一个共享 `baseEndpoint`（统一错误输出与 `requestId`）。
- 业务 endpoint 分为 3 个：`allocateNote`、`getNotePage`、`saveNote`。
- 错误模型统一为代数数据类型（ADT），由统一错误映射器输出 HTTP 状态码。

## 2. 领域模型草案

## 2.1 值对象

- `NoteId(value: String)`：必须满足 `[a-z0-9]{4}`
- `NoteContent(value: String)`：最大 200 KB
- `NoteVersion(value: Long)`：非负整数
- `RequestId(value: String)`

## 2.2 成功输出模型

- `SaveNoteResponse`
  - `id: String`
  - `version: Long`
  - `updatedAt: Instant`

## 2.3 错误输出模型

- `ApiErrorEnvelope`
  - `error: ApiErrorBody`
  - `current: Option[CurrentVersion]`（仅冲突时出现）
- `ApiErrorBody`
  - `code: String`
  - `message: String`
  - `requestId: String`
- `CurrentVersion`
  - `version: Long`

## 3. 错误 ADT 草案

- `InvalidId`
- `InvalidArgument`
- `ContentTooLarge`
- `TooManyRequests`
- `VersionConflict(currentVersion: Long)`
- `IdPoolBusy`
- `InternalError`

状态码映射：

- `InvalidId` / `InvalidArgument` -> `400`
- `ContentTooLarge` -> `413`
- `TooManyRequests` -> `429`
- `VersionConflict` -> `409`
- `IdPoolBusy` -> `503`
- `InternalError` -> `500`

## 4. Endpoint 草案清单

## 4.1 `allocateNote`（`GET /`）

- 输入：
  - `GET /`
- 输出：
  - `302` + `Location: /{id}`
- 错误：
  - `503`（`ID_POOL_BUSY`）

Tapir 形态建议：

- `endpoint.get.in("")`
- `out(header[String]("Location")).out(statusCode(StatusCode.Found))`
- `errorOut(jsonBody[ApiErrorEnvelope])`

## 4.2 `getNotePage`（`GET /:id`）

- 输入：
  - Path `id`
- 输出：
  - `200 text/html`
- 错误：
  - `400`（`INVALID_ID`）

Tapir 形态建议：

- `endpoint.get.in(path[String]("id"))`
- `out(stringBody.and(header("Content-Type", "text/html; charset=utf-8")))`
- `errorOut(jsonBody[ApiErrorEnvelope])`

## 4.3 `saveNote`（`POST /:id`）

- 输入：
  - Path `id`
  - Form `t`
  - Form `version`
- 输出：
  - `200` + `SaveNoteResponse`
- 错误：
  - `400/409/413/429/500`

Tapir 形态建议：

- `endpoint.post.in(path[String]("id"))`
- `in(formBody[SaveNoteForm])`
- `out(jsonBody[SaveNoteResponse])`
- `errorOut(jsonBody[ApiErrorEnvelope])`

表单模型草案：

- `SaveNoteForm`
  - `t: String`
  - `version: Long`

## 5. Server Logic 映射草案

- `allocateNote` -> `idAllocator.allocate()` -> `Either[ApiError, String]`
- `getNotePage` -> `noteService.getOrEmpty(id)` -> `Either[ApiError, HtmlPage]`
- `saveNote` -> `noteService.save(id, content, version)` -> `Either[ApiError, SaveNoteResponse]`

说明：建议 endpoint 层只做解码与错误映射，不承载业务规则。

## 6. 编解码与校验草案

- `circe`：`SaveNoteResponse`、`ApiErrorEnvelope` 编解码
- 表单解码：`SaveNoteForm`（缺失字段直接映射 `400 INVALID_ARGUMENT`）
- `NoteId` 校验：在 endpoint 输入后第一层执行，失败返回 `INVALID_ID`
- `NoteContent` 校验：服务层执行长度校验，失败返回 `CONTENT_TOO_LARGE`

## 7. 实施 Checklist

- [ ] 定义 `ApiError` ADT 与状态码映射函数
- [ ] 定义 `ApiErrorEnvelope` 与 `SaveNoteResponse` codec
- [ ] 定义 `SaveNoteForm` 表单解码与参数校验
- [ ] 实现 `allocateNote` endpoint 草案
- [ ] 实现 `getNotePage` endpoint 草案
- [ ] 实现 `saveNote` endpoint 草案
- [ ] 完成 endpoint 到 service 的 server logic 绑定
- [ ] 完成接口级测试（200/302/400/409/413/429/503）

## 8. 与现有文档关系

- 协议来源： [note-ms-api-spec.md](./note-ms-api-spec.md)
- 架构来源： [note-ms-technical-design.md](./note-ms-technical-design.md)
- 产品边界： [note-ms-mvp-design.md](./note-ms-mvp-design.md)

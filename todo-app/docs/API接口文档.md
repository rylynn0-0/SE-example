# API 接口文档

待办清单示例的全部后端接口说明。本文档与代码同步维护；**在线可调试版**由 Knife4j 根据代码注解自动生成，见文末第八节。

---

## 一、基础信息

| 项 | 值 |
|---|---|
| 基础路径 | `http://localhost:8080/api` |
| 前端代理访问 | `http://localhost:8081/api`（经 Nginx 转发，同源，无跨域） |
| 开发模式访问 | `http://localhost:5173/api`（经 Vite 代理转发） |
| 数据格式 | 请求与响应均为 `application/json; charset=utf-8` |
| 字符编码 | UTF-8 |
| 时间格式 | 日期 `yyyy-MM-dd`，日期时间 `yyyy-MM-dd HH:mm:ss`（时区 Asia/Shanghai） |
| 鉴权 | 无（本示例未实现登录） |

### 1.1 统一响应结构

所有接口都返回如下三段式结构，由 `common/Result.java` 定义：

```json
{
  "code": 200,
  "message": "成功",
  "data": { }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | int | 业务状态码，`200` 表示成功 |
| `message` | string | 提示信息，失败时为具体原因 |
| `data` | any | 业务数据，失败时通常为 `null` |

### 1.2 关于 HTTP 状态码的重要说明

本项目采用「**HTTP 状态码恒为 200，业务结果看 body 里的 `code`**」的约定：

| 场景 | HTTP 状态 | body.code | 说明 |
|---|---|---|---|
| 查询/新增/更新/删除成功 | 200 | 200 | 正常返回 |
| 标题为空 | 200 | 400 | 由 `Result.fail(400, ...)` 返回 |
| 任务不存在 | 200 | 404 | 由 `Result.fail(404, ...)` 返回 |
| 未捕获的异常（如数据库断开） | 500 | — | 由 Spring Boot 默认错误处理返回 `{timestamp,status,error,path}`，**不是**上面的三段式结构 |

> 前端的 `axios` 响应拦截器（`api/request.js`）正是基于 `body.code === 200` 来判断成功的。
> 生产项目通常会再加一个全局异常处理器（`@RestControllerAdvice`），把第 4 种情况也统一成三段式结构；本示例为保持「最简」未引入。

### 1.3 字段字典

**priority 优先级**

| 值 | 含义 | 界面标签颜色 |
|---|---|---|
| 1 | 低 | 灰（info） |
| 2 | 中 | 橙（warning） |
| 3 | 高 | 红（danger） |

**finished 完成状态**

| 值 | 含义 |
|---|---|
| 0 | 未完成 |
| 1 | 已完成 |

**Task 对象**

| 字段 | 类型 | 说明 | 示例 |
|---|---|---|---|
| `id` | long | 主键，数据库自增 | `1` |
| `title` | string | 任务标题，必填，最长 100 字符 | `"完成软工课程设计开题报告"` |
| `description` | string\|null | 备注，最长 500 字符 | `"整理论文选题"` |
| `priority` | int | 优先级 1/2/3 | `3` |
| `finished` | int | 完成状态 0/1 | `0` |
| `dueDate` | string\|null | 截止日期 `yyyy-MM-dd` | `"2026-11-10"` |
| `createTime` | string | 创建时间，数据库自动生成 | `"2026-11-08 19:00:00"` |
| `updateTime` | string | 更新时间，数据库自动维护 | `"2026-11-08 19:30:00"` |

> `deleted` 字段带 `@JsonIgnore`，**不会**出现在响应中。

---

## 二、接口总览

| # | 方法 | 路径 | 说明 | 是否写操作 |
|---|---|---|---|---|
| 1 | GET | `/api/tasks` | 分页查询任务列表 | 否 |
| 2 | GET | `/api/tasks/stats` | 任务统计（Redis 缓存） | 否 |
| 3 | POST | `/api/tasks` | 新增任务 | 是（清缓存） |
| 4 | PUT | `/api/tasks/{id}` | 更新任务 | 是（清缓存） |
| 5 | PATCH | `/api/tasks/{id}/finished` | 切换完成状态 | 是（清缓存） |
| 6 | DELETE | `/api/tasks/{id}` | 逻辑删除任务 | 是（清缓存） |

---

## 三、GET /api/tasks —— 分页查询

按条件分页查询任务，支持标题模糊搜索与完成状态筛选。

### 3.1 请求参数（Query String）

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `pageNum` | long | 否 | 1 | 页码，从 1 开始 |
| `pageSize` | long | 否 | 5 | 每页条数 |
| `keyword` | string | 否 | — | 标题关键词，模糊匹配（`LIKE '%keyword%'`）；不传或空串表示不筛选 |
| `finished` | int | 否 | — | `0` 只看未完成，`1` 只看已完成；不传表示全部 |

### 3.2 排序规则

结果固定按三级排序，保证「最该做的事」排在最前：

1. `finished` 升序 —— 未完成（0）在前，已完成（1）在后
2. `priority` 降序 —— 高优先级在前
3. `id` 降序 —— 同优先级下新建的在前

### 3.3 请求示例

```
GET /api/tasks?pageNum=1&pageSize=5&keyword=报告&finished=0
```

```bash
curl "http://localhost:8080/api/tasks?pageNum=1&pageSize=5&keyword=报告&finished=0"
```

### 3.4 成功响应

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "records": [
      {
        "id": 1,
        "title": "完成软工课程设计开题报告",
        "description": "整理论文选题、技术选型与进度安排",
        "priority": 3,
        "finished": 0,
        "dueDate": "2026-11-10",
        "createTime": "2026-11-08 19:00:00",
        "updateTime": "2026-11-08 19:00:00"
      }
    ],
    "total": 1,
    "size": 5,
    "current": 1,
    "pages": 1
  }
}
```

`data` 是 MyBatis-Plus 的 `Page` 对象，各字段含义：

| 字段 | 说明 |
|---|---|
| `records` | 当前页的数据数组 |
| `total` | 满足条件的总条数（不是本页条数） |
| `size` | 每页条数 |
| `current` | 当前页码 |
| `pages` | 总页数 = ⌈total / size⌉ |

> 响应中还会附带 `orders`、`optimizeCountSql`、`searchCount`、`maxLimit` 等框架内部字段，属正常现象，前端可忽略。

### 3.5 空结果

查不到数据时 `records` 为 `[]`、`total` 为 0，仍返回 `code: 200`：

```json
{
  "code": 200,
  "message": "成功",
  "data": { "records": [], "total": 0, "size": 5, "current": 1, "pages": 0 }
}
```

---

## 四、GET /api/tasks/stats —— 任务统计（Redis 缓存）

返回任务总数、已完成数、未完成数。**这是演示 Redis 缓存的接口**：结果缓存在 Redis 的 `todo:stats` 键中，TTL 60 秒。

### 4.1 请求参数

无。

### 4.2 请求示例

```bash
curl "http://localhost:8080/api/tasks/stats"
```

### 4.3 成功响应

**首次请求（或缓存已失效）—— 回源数据库：**

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "total": 8,
    "finished": 3,
    "unfinished": 5,
    "fromCache": false,
    "ttl": 60
  }
}
```

**60 秒内再次请求 —— 命中缓存：**

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "total": 8,
    "finished": 3,
    "unfinished": 5,
    "fromCache": true,
    "ttl": 47
  }
}
```

### 4.4 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `total` | long | 任务总数（不含已逻辑删除的） |
| `finished` | long | 已完成数量 |
| `unfinished` | long | 未完成数量 = total − finished |
| `fromCache` | boolean | `true` 表示本次命中 Redis 缓存（没有查数据库）；`false` 表示回源数据库并已写入缓存 |
| `ttl` | long | 缓存剩余有效秒数。命中时为剩余时间，回源时为完整 TTL（60） |

### 4.5 缓存行为说明

| 操作 | 对缓存的影响 |
|---|---|
| 调用本接口（未命中） | 查库两次 `COUNT`，把结果写入 Redis，TTL 60 秒 |
| 调用本接口（命中） | 直接返回缓存，不查数据库 |
| 新增 / 更新 / 删除 / 切换状态 | 立即删除 `todo:stats`，下次读自然回源 |
| 60 秒过期 | Redis 自动删除，下次读回源 |
| Redis 不可用 | 接口**不报错**，降级为直接查数据库（`fromCache` 恒为 `false`） |

手动验证缓存：

```bash
# 查看缓存内容与剩余 TTL
docker exec -it todo-redis redis-cli GET todo:stats
docker exec -it todo-redis redis-cli TTL todo:stats

# 手动删除缓存（模拟失效）
docker exec -it todo-redis redis-cli DEL todo:stats
```

---

## 五、POST /api/tasks —— 新增任务

### 5.1 请求参数（JSON Body）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `title` | string | **是** | 任务标题，最长 100 字符 |
| `description` | string | 否 | 备注，最长 500 字符 |
| `priority` | int | 否 | 优先级 1/2/3，不传默认 **2（中）** |
| `dueDate` | string | 否 | 截止日期 `yyyy-MM-dd` |
| `finished` | int | 否 | 不传默认 **0（未完成）**。新任务一般不需要传 |

> `id`、`createTime`、`updateTime` 由服务端生成，传了也会被忽略。

### 5.2 请求示例

```bash
curl -X POST "http://localhost:8080/api/tasks" \
  -H "Content-Type: application/json" \
  -d '{
        "title": "梳理毕业设计进度节点",
        "description": "与导师确认中期检查时间",
        "priority": 3,
        "dueDate": "2026-11-20"
      }'
```

### 5.3 成功响应

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 9,
    "title": "梳理毕业设计进度节点",
    "description": "与导师确认中期检查时间",
    "priority": 3,
    "finished": 0,
    "dueDate": "2026-11-20",
    "createTime": "2026-11-08 19:45:12",
    "updateTime": "2026-11-08 19:45:12"
  }
}
```

> 返回的是**重新从数据库查出的对象**，因此 `id`、`createTime`、`updateTime` 都有值 —— 前端可以直接用返回的 `id` 做后续操作。

### 5.4 失败响应

```json
{ "code": 400, "message": "任务标题不能为空", "data": null }
```

---

## 六、PUT /api/tasks/{id} —— 更新任务

### 6.1 路径参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | long | 是 | 任务主键 |

### 6.2 请求体

字段与新增相同。**注意两点：**

1. `title` 仍然必填（不能通过更新接口把标题清空）；
2. `description` 与 `dueDate` 支持**清空**：显式传 `null` 会把数据库中的值改为 NULL（实体上标注了 `FieldStrategy.ALWAYS`）；
3. 未传的字段保持原值不变（MyBatis-Plus 默认跳过 null 字段）。

### 6.3 请求示例

```bash
curl -X PUT "http://localhost:8080/api/tasks/9" \
  -H "Content-Type: application/json" \
  -d '{
        "title": "梳理毕业设计进度节点（已改期）",
        "priority": 2,
        "dueDate": null
      }'
```

### 6.4 成功响应

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 9,
    "title": "梳理毕业设计进度节点（已改期）",
    "description": "与导师确认中期检查时间",
    "priority": 2,
    "finished": 0,
    "dueDate": null,
    "createTime": "2026-11-08 19:45:12",
    "updateTime": "2026-11-08 19:47:30"
  }
}
```

### 6.5 失败响应

```json
{ "code": 400, "message": "任务标题不能为空", "data": null }
```

```json
{ "code": 404, "message": "任务不存在", "data": null }
```

---

## 七、PATCH /api/tasks/{id}/finished —— 切换完成状态

把「未完成」改为「已完成」，或反之。对应列表页里的开关。

### 7.1 路径参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | long | 是 | 任务主键 |

### 7.2 请求示例

无请求体：

```bash
curl -X PATCH "http://localhost:8080/api/tasks/1/finished"
```

### 7.3 成功响应

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "title": "完成软工课程设计开题报告",
    "description": "整理论文选题、技术选型与进度安排",
    "priority": 3,
    "finished": 1,
    "dueDate": "2026-11-10",
    "createTime": "2026-11-08 19:00:00",
    "updateTime": "2026-11-08 19:50:02"
  }
}
```

### 7.4 失败响应

```json
{ "code": 404, "message": "任务不存在", "data": null }
```

> 同一接口连续调用两次会得到相反结果（`0 → 1 → 0`），因为它执行的是「取反」而不是「设为已完成」。

---

## 八、DELETE /api/tasks/{id} —— 逻辑删除任务

### 8.1 路径参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | long | 是 | 任务主键 |

### 8.2 请求示例

```bash
curl -X DELETE "http://localhost:8080/api/tasks/9"
```

### 8.3 成功响应

```json
{ "code": 200, "message": "成功", "data": null }
```

### 8.4 失败响应

```json
{ "code": 404, "message": "任务不存在", "data": null }
```

### 8.5 关于「逻辑删除」

数据库执行的是 `UPDATE t_task SET deleted = 1 WHERE id = ?`，**记录仍在表中**。删掉之后：

- 所有查询接口都不再返回它（MyBatis-Plus 自动追加 `deleted = 0`）；
- 统计数字会相应减少；
- 数据仍可用 SQL 直接查回，便于审计追溯。

---

## 九、错误码汇总

| body.code | 触发场景 | 示例 message |
|---|---|---|
| 200 | 成功 | `成功` |
| 400 | 参数校验失败 | `任务标题不能为空` |
| 404 | 资源不存在或已被逻辑删除 | `任务不存在` |
| 500 | 服务端未预期异常（由 Spring Boot 默认错误处理返回，非三段式结构） | — |

**HTTP 层错误（非业务错误）**

| 现象 | 原因 |
|---|---|
| `404 Not Found`（HTML 或 Spring 错误页） | 路径拼错，或后端未启动 |
| `502 Bad Gateway` | Nginx 起来了但后端容器不可用 |
| `Network Error` / `ECONNREFUSED` | 后端未启动或端口不通 |
| 请求超时 | 数据库连接异常；前端 axios 超时为 10 秒 |

---

## 十、在线接口文档（Knife4j）

除本文档外，项目还提供**由代码注解自动生成**的在线文档，支持在线发请求调试。

### 10.1 访问地址

| 环境 | 地址 |
|---|---|
| 后端直连 | http://localhost:8080/doc.html |
| 经前端 Nginx 代理 | http://localhost:8081/doc.html |
| 前端 dev 模式 | http://localhost:5173/doc.html |
| OpenAPI 原始 JSON | http://localhost:8080/v3/api-docs |

### 10.2 使用方式

1. 打开 `/doc.html`，左侧按 `@Tag` 分组（本项目为「待办任务」）；
2. 展开某个接口，右侧显示参数表单（由 `@Schema` 生成）；
3. 填好参数点「发送」，直接在页面上看到真实响应；
4. 顶部「文档管理」可导出离线文档（Markdown / HTML / OpenAPI JSON）。

### 10.3 文档与代码的对应关系

| 文档元素 | 代码来源 |
|---|---|
| 文档标题、版本、描述 | `config/Knife4jConfig.java` 的 `OpenAPI` Bean |
| 左侧分组名 | Controller 上的 `@Tag(name = ...)` |
| 接口标题与说明 | 方法上的 `@Operation(summary, description)` |
| 路径参数说明 | `@Parameter(description = ...)` |
| 字段名、类型、示例 | 实体 / DTO 字段上的 `@Schema(description, example)` |

> 注解只影响文档展示，**不影响接口运行**。

---

## 十一、推荐调用顺序（冒烟测试）

按顺序执行可验证全部功能与缓存行为：

```bash
# 1) 分页查询（应返回 8 条中的前 5 条）
curl "http://localhost:8080/api/tasks?pageNum=1&pageSize=5"

# 2) 统计（第一次：fromCache=false，ttl=60）
curl "http://localhost:8080/api/tasks/stats"

# 3) 再查一次统计（应命中缓存：fromCache=true，ttl 变小）
curl "http://localhost:8080/api/tasks/stats"

# 4) 新增（应返回自增 id 与时间）
curl -X POST "http://localhost:8080/api/tasks" \
  -H "Content-Type: application/json" \
  -d '{"title":"测试任务","priority":1}'

# 5) 再查统计（缓存已被删除，应回到 fromCache=false，total 变为 9）
curl "http://localhost:8080/api/tasks/stats"

# 6) 切换完成状态（以新增的 id 为准，假设为 9）
curl -X PATCH "http://localhost:8080/api/tasks/9/finished"

# 7) 更新
curl -X PUT "http://localhost:8080/api/tasks/9" \
  -H "Content-Type: application/json" \
  -d '{"title":"测试任务（已修改）","priority":3}'

# 8) 删除
curl -X DELETE "http://localhost:8080/api/tasks/9"

# 9) 确认已删除（再删一次应返回 404）
curl -X DELETE "http://localhost:8080/api/tasks/9"
```

第 2、3、5 步的结果差异，就是 Redis 缓存在起作用的直接证据。

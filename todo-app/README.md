# 待办清单 · 最简全栈示例

一个用 **Vue3 + Element Plus + Spring Boot + MyBatis-Plus + MySQL + Redis + Knife4j + Docker** 实现的单表 CRUD 项目，代码量小、每层都能讲清楚，适合作为软件工程课程设计的骨架或全栈入门示例。

**它演示了什么**

- 前端表格的分页、关键词搜索、状态筛选
- 任务的增 / 删 / 改 / 查 + 完成状态切换（逻辑删除）
- Redis 缓存：统计数字缓存 60 秒，页面把「命中 / 回源」直接显示出来
- Knife4j 在线接口文档，可在网页上直接调试接口
- Docker Compose 四容器一键启动（MySQL / Redis / 后端 / 前端）

---

## 一、技术栈与代码位置的对应关系

| 技术 | 在本项目中的职责 | 主要代码位置 |
|---|---|---|
| **Vue 3** | 页面渲染、响应式状态、组件逻辑 | `frontend/src/views/TodoView.vue`、`frontend/src/App.vue` |
| **Element Plus** | 表格 / 分页 / 弹窗 / 表单 / 标签等 UI 组件 | `frontend/src/main.js`（注册）、`TodoView.vue`（使用） |
| **Vite** | 开发服务器、热更新、生产构建、接口代理 | `frontend/vite.config.js`、`frontend/package.json` |
| **Vue Router** | 路由与页面切换 | `frontend/src/router/index.js` |
| **Axios** | HTTP 请求、统一响应处理、错误提示 | `frontend/src/api/request.js`、`frontend/src/api/task.js` |
| **Spring Boot** | Web 容器、依赖注入、配置管理、REST 接口 | `backend/src/main/java/com/example/todo/TodoApplication.java`、`controller/` |
| **MyBatis-Plus** | ORM、免写 SQL 的 CRUD、分页、逻辑删除 | `entity/Task.java`、`mapper/TaskMapper.java`、`service/impl/TaskServiceImpl.java`、`config/MybatisPlusConfig.java` |
| **Knife4j** | 根据注解生成在线接口文档 | `config/Knife4jConfig.java` + 各 Controller 上的 `@Tag/@Operation` |
| **MySQL** | 业务数据持久化 | `backend/src/main/resources/db/init.sql`、`application.yml` 数据源 |
| **Redis** | 统计信息缓存（Cache-Aside） | `service/impl/TaskServiceImpl.java` 的 `stats()` 与 `clearStatsCache()` |
| **Docker** | 镜像构建与四容器编排 | `docker-compose.yml`、`backend/Dockerfile`、`frontend/Dockerfile`、`frontend/nginx.conf` |

> 想按「技术 → 代码片段」逐条对照阅读，请看 **[docs/技术栈代码对照.md](docs/技术栈代码对照.md)**。
> 想查每个接口的请求参数和返回结构，请看 **[docs/API接口文档.md](docs/API接口文档.md)**。
> 想按「前端 → 后端 → 数据库 → 两处耦合 → 必要技术」系统入门，请看 **[docs/tutorials/00-开始这里.md](docs/tutorials/00-开始这里.md)**。共 10 篇：总览 + 六章概念 + 三份模板手册（[07 前端](docs/tutorials/07-前端模板.md)、[08 后端](docs/tutorials/08-后端文件模板.md)、[09 数据库](docs/tutorials/09-数据库模板.md)），模板与语法写在一起。

---

## 二、整体架构与请求链路

```
┌──────────────────────────────────────────────────────────────┐
│  浏览器  http://localhost:8081                                │
│    Vue3 + Element Plus 页面（TodoView.vue）                   │
└───────────────────────────┬──────────────────────────────────┘
                            │ axios 请求 /api/tasks
                            ▼
┌──────────────────────────────────────────────────────────────┐
│  frontend 容器（Nginx）                                       │
│   · 托管 Vue 构建出的静态文件（dist/）                        │
│   · proxy_pass /api/*  →  http://backend:8080                 │
└───────────────────────────┬──────────────────────────────────┘
                            ▼
┌──────────────────────────────────────────────────────────────┐
│  backend 容器（Spring Boot，端口 8080）                       │
│                                                              │
│   TaskController ── 收参数、校验、包装 Result                 │
│        │                                                     │
│        ▼                                                     │
│   TaskServiceImpl ── 业务规则 + 事务 + 缓存失效               │
│        │                    │                                │
│        │ 缓存命中/回源       │ MyBatis-Plus                   │
│        ▼                    ▼                                │
│   ┌─────────┐        ┌──────────────┐                        │
│   │  Redis  │        │     MySQL    │                        │
│   │ 容器    │        │    容器      │                        │
│   └─────────┘        └──────────────┘                        │
│        ▲                    ▲                                │
│        └── Knife4j 读取代码注解，生成 /doc.html 文档 ──┘      │
└──────────────────────────────────────────────────────────────┘
```

**一条查询请求的完整路径**（从点击「查询」到表格刷新）：

1. `TodoView.vue` 里 `handleSearch()` 被触发
2. 调用 `api/task.js` 的 `pageTasks(query)`
3. `api/request.js` 的 axios 实例发出 `GET /api/tasks?pageNum=1&pageSize=5&keyword=报告`
4. 开发环境由 Vite `server.proxy` 转发，Docker 环境由 Nginx `proxy_pass` 转发 —— 都到 `backend:8080`
5. `TaskController.page()` 接收 `TaskQuery`
6. `TaskServiceImpl.pageTasks()` 用 `LambdaQueryWrapper` 拼条件，`page()` 执行分页查询
7. MyBatis-Plus 的分页拦截器把 SQL 改写成 `... WHERE title LIKE ? AND deleted=0 LIMIT 0,5`，并另发一条 `COUNT(*)`
8. 结果逐层包装成 `Result<IPage<Task>>` → JSON
9. axios 响应拦截器拆掉 `{code,message,data}` 外壳，把 `data` 交给页面
10. `list.value = data.records`，表格刷新

---

## 三、目录结构

```
todo-app/
├── docker-compose.yml              # 四容器编排（见 docs/tutorials/06-必要技术.md 第四节）
├── README.md
├── docs/
│   ├── 技术栈代码对照.md            # 每项技术 → 对应代码片段
│   ├── API接口文档.md               # 接口清单、参数、返回示例、错误码
│   └── tutorials/                  # 分层教程（总览 + 六章 + 三份模板）
│       ├── 00-开始这里.md           # 建议从这里开始
│       ├── 01-前端.md              # Vue3 / Element Plus / Vite
│       ├── 02-后端.md              # Spring Boot / 分层 / 统一响应
│       ├── 03-数据库.md            # MySQL / Redis
│       ├── 04-前端与后端耦合.md     # 五份契约 / 代理 / 联调
│       ├── 05-后端与数据库耦合.md   # MyBatis-Plus / 事务 / 缓存
│       ├── 06-必要技术.md           # Knife4j / Nginx / Docker
│       ├── 07-前端模板.md           # 文件与页面区块拆解 + Vue/JS 语法
│       ├── 08-后端文件模板.md       # 文件与接口/方法拆解 + Java/Spring
│       └── 09-数据库模板.md         # 建表脚本逐块拆解 + SQL/Redis
├── backend/                        # Spring Boot 后端
│   ├── pom.xml                     # 依赖：web / mybatis-plus / mysql / redis / knife4j / lombok
│   ├── settings.xml                # Maven 阿里云镜像（只在 Docker 构建时用 -s 指定）
│   ├── Dockerfile                  # 多阶段构建：maven 编译 → jre 运行
│   └── src/main/
│       ├── java/com/example/todo/
│       │   ├── TodoApplication.java        # 启动类（@SpringBootApplication + @MapperScan）
│       │   ├── common/Result.java          # 统一响应体 {code, message, data}
│       │   ├── config/
│       │   │   ├── MybatisPlusConfig.java  # 分页拦截器
│       │   │   ├── Knife4jConfig.java      # 文档封面信息
│       │   │   └── WebConfig.java          # 跨域 CORS
│       │   ├── controller/TaskController.java   # REST 接口
│       │   ├── service/
│       │   │   ├── TaskService.java             # 业务接口
│       │   │   └── impl/TaskServiceImpl.java    # 业务实现（Redis 缓存在这里）
│       │   ├── mapper/TaskMapper.java      # 继承 BaseMapper，零 SQL
│       │   ├── entity/Task.java            # 表映射实体
│       │   └── dto/
│       │       ├── TaskQuery.java          # 分页查询参数
│       │       └── TaskStats.java          # 统计返回结构
│       └── resources/
│           ├── application.yml             # 数据源 / Redis / MyBatis-Plus / 文档配置
│           └── db/init.sql                 # 建库建表 + 8 条演示数据
└── frontend/                       # Vue3 前端
    ├── package.json                # 依赖与脚本
    ├── vite.config.js              # 开发服务器 + /api 代理 + 构建配置
    ├── index.html                  # HTML 模板（挂载点 #app）
    ├── Dockerfile                  # 多阶段构建：node 打包 → nginx 托管
    ├── nginx.conf                  # 静态托管 + /api 反向代理
    └── src/
        ├── main.js                 # 入口：创建应用、注册 Element Plus 与路由
        ├── App.vue                 # 根组件：页头 + <router-view />
        ├── router/index.js         # 路由表
        ├── api/
        │   ├── request.js          # axios 实例 + 请求/响应拦截器
        │   └── task.js             # 各接口的方法封装
        ├── styles/global.css       # 全局基础样式
        └── views/TodoView.vue      # 主页面：统计卡片 + 表格 + 分页 + 弹窗
```

---

## 四、每项技术在项目里是如何组织的

这一章回答的是「这些技术各自装在哪一层、怎么串起来」，是理解项目的关键。

### 4.1 Vue 3 —— 从入口到页面的组织方式

组织顺序是一条单向链：

```
index.html 的 <div id="app">
   └─ main.js：createApp(App) → app.mount('#app')
        └─ App.vue：只做布局（页头 + <router-view />）
             └─ router/index.js：'/' → TodoView.vue
                  └─ TodoView.vue：真正干活的页面
```

- **`main.js`** 是唯一入口，只有 3 个动作：创建应用、装插件（Element Plus、Router）、挂载。
- **`App.vue`** 刻意保持极简，不写业务，只提供外壳。以后加页面时它完全不用改。
- **`TodoView.vue`** 用 `<script setup>` + Composition API：
  - `ref` 管简单值（`loading`、`total`、`list`）
  - `reactive` 管对象（`query`、`stats`、`dialog`、`form`）
  - 方法按「数据加载 → 交互动作 → 初始化」分区排列，并用 JSDoc 写明每个方法的入参和职责
- **状态全部放在页面组件内**，本示例没有引入 Pinia —— 因为只有一个页面，再抽一层状态库属于过度设计。加登录功能后，用户信息才值得放进 Pinia。

### 4.2 Element Plus —— 组件注册与使用

- **注册**：`main.js` 里 `app.use(ElementPlus, { locale: zhCn })`，全量注册组件并加载中文语言包。全量引入配置最少（适合「最简」），代价是打包体积偏大；生产项目通常用 `unplugin-vue-components` 做按需引入。
- **样式**：`import 'element-plus/dist/index.css'`，一行引入全部主题样式。
- **使用**：全部集中在 `TodoView.vue` 的模板里，按页面结构分成三块：
  - 统计区：`el-row` / `el-col` / `el-card`
  - 主操作区：`el-input`（搜索）、`el-select`（状态筛选）、`el-button`、`el-tag`（优先级与缓存状态）、`el-table`（含自定义列 `template #default`）、`el-switch`（状态开关）、`el-pagination`（分页）
  - 表单区：`el-dialog` + `el-form` + `el-form-item` + `el-radio-group` + `el-date-picker`
- **表单校验**由 `el-form` 的 `rules` 声明式完成，不需要手写 if 判断；但后端仍然保留「标题非空」校验 —— 前端校验只是体验优化，不能当作安全边界。

### 4.3 Vite —— 开发与构建

- **开发**：`npm run dev` 启动 5173，改动 `.vue` 文件毫秒级热更新，不刷新页面。
- **代理**（`vite.config.js` 的 `server.proxy`）：把 `/api`、`/doc.html`、`/v3/api-docs`、`/webjars` 转发到 `http://localhost:8080`。
  - 这样前端代码里统一写相对路径 `/api/tasks`，**换环境不用改地址**，也顺手解决了跨域。
- **构建**：`npm run build` 输出 `dist/`（纯静态 html+js+css），Node 在此之后就完全不参与运行了 —— 这正是 Dockerfile 第二阶段能换成 Nginx 的原因。

### 4.4 Spring Boot —— 分层组织

后端严格分四层，依赖方向单向向下，不允许跨层反调：

```
Controller  ← 只做「收参数 / 校验 / 包装返回」
    ↓
Service     ← 业务规则、事务边界、缓存失效（唯一的业务层）
    ↓
Mapper      ← 只和数据库打交道
    ↓
Entity      ← 表结构映射
```

| 层 | 文件 | 关键点 |
|---|---|---|
| 启动 | `TodoApplication.java` | `@SpringBootApplication` 自动扫描本包；`@MapperScan` 注册 Mapper 代理 |
| 配置 | `config/*.java` | 分页拦截器、Knife4j 元信息、CORS |
| 接口 | `controller/TaskController.java` | `@RestController` + `@RequestMapping("/api/tasks")`，RESTful 风格 |
| 业务 | `service/impl/TaskServiceImpl.java` | 继承 `ServiceImpl` 拿到通用 CRUD，注入 `StringRedisTemplate` 做缓存 |
| 数据 | `mapper/TaskMapper.java` | 继承 `BaseMapper<Task>`，一行 SQL 不写就有 17 个方法 |
| 传输 | `dto/TaskQuery.java`、`dto/TaskStats.java` | 请求参数与统计返回的专用结构 |
| 配置 | `resources/application.yml` | 用 `${ENV:默认值}` 占位符，让同一份 jar 在本地和容器里都能跑 |

**依赖注入方式**：全部用构造器注入（Lombok 的 `@RequiredArgsConstructor` + `final` 字段），而不是字段注入。好处是依赖不可变、不会为 null、单元测试时可以直接 `new` 出来传 mock。

**统一响应**：所有接口返回 `Result<T>{code, message, data}`，前端据此只判断 `code === 200`，简化了前端的错误处理。

### 4.5 MyBatis-Plus —— 三个配置点与一个实体

只有三处需要额外配置，就能享受「零 SQL」：

1. **实体映射**（`entity/Task.java`）
   - `@TableName("t_task")` 指定表名
   - `@TableId(type = IdType.AUTO)` 指定主键自增
   - `@TableLogic` 标注 `deleted` 字段 → 自动逻辑删除（查询自动加 `deleted=0`，删除自动改成 `UPDATE ... SET deleted=1`）
   - `@TableField(updateStrategy = ALWAYS)` 让 `description`、`dueDate` 可以被清空（默认策略会忽略 null）
2. **分页拦截器**（`config/MybatisPlusConfig.java`）：注册 `PaginationInnerInterceptor(DbType.MYSQL)`。**不注册它，`page()` 会退化为查全表**，这是最常见的坑。
3. **全局配置**（`application.yml`）：`map-underscore-to-camel-case`（`due_date` ↔ `dueDate`）、逻辑删除的字段名与取值、SQL 打印。

**条件构造器**：`TaskServiceImpl.pageTasks()` 用 `LambdaQueryWrapper` 拼条件，`Task::getTitle` 是方法引用，写错字段名编译期就报错；每个条件的第一个布尔参数是开关，为 false 时该条件不拼接，避免一堆 if-else。

### 4.6 Knife4j —— 注解即文档

- **原理**：springdoc 启动时扫描所有 `@RestController`，把 `@Tag / @Operation / @Parameter / @Schema` 读成一份 OpenAPI 3 JSON（`/v3/api-docs`），Knife4j 再把它渲染成中文页面 `/doc.html`。
- **组织方式**：
  - 全局封面（标题、版本、描述）写在 `config/Knife4jConfig.java`
  - 接口分组用 Controller 上的 `@Tag`
  - 每个方法用 `@Operation(summary, description)`
  - 字段说明写在实体与 DTO 的 `@Schema(description, example)` 上
- **访问入口**：`http://localhost:8080/doc.html`（后端直连）或 `http://localhost:8081/doc.html`（经前端 Nginx 代理）。
- **特点**：注解只影响文档，不影响运行逻辑 —— 删掉注解接口照样能跑。

### 4.7 MySQL —— 数据与初始化

- **建表脚本** `db/init.sql` 由 Docker 挂载到 `/docker-entrypoint-initdb.d/`，MySQL 容器**首次**启动时自动执行建库、建表、插入 8 条演示数据。
- **表设计**：`create_time`/`update_time` 用数据库默认值与 `ON UPDATE CURRENT_TIMESTAMP` 自动维护 —— 无论从哪个入口写入，时间都准确，业务代码不用手写。
- **索引**：`finished`（状态筛选/排序）、`title`（搜索）。
- **连接**：`application.yml` 里 `jdbc:mysql://${MYSQL_HOST:localhost}:...`，容器里读到 `mysql`，本地读到 `localhost`。
- **逻辑删除**：删除不物理删数据，配合 `@TableLogic` 实现可追溯。

### 4.8 Redis —— Cache-Aside 缓存

Redis 在整个项目里只有**一个落点**，就是统计接口，刻意做得很小但很完整（`TaskServiceImpl.stats()`）：

```
读：GET todo:stats
      ├─ 命中 → 反序列化 → 返回（fromCache = true，附带剩余 TTL）
      └─ 未命中 → 查 MySQL 两次 COUNT → 序列化写回（TTL 60s）→ 返回（fromCache = false）

写：任何增删改 → DEL todo:stats → 下一次读自然回源
```

- **为什么用「删除」而不是「更新」缓存**：并发写时「更新缓存」容易出现旧值覆盖新值；「删除」让下次读强制回源，更不容易出错。
- **为什么所有 Redis 操作都包了 try/catch**：缓存是加速层不是数据源，Redis 挂了应该降级为直接查库，而不是让接口 500。
- **为什么能让用户看见**：`TaskStats` 多带了 `fromCache` 和 `ttl`，页面工具栏据此显示「Redis 命中 · 剩余 42s」还是「Redis 回源」，答辩演示时缓存效果一目了然。
- **已知简化**：缓存在事务提交前就被删除了，极端并发下可能有短暂不一致。生产做法是用 `TransactionSynchronizationManager` 注册 `afterCommit` 回调 —— 代码里已注释说明。

### 4.9 Docker —— 镜像与编排

**两个 Dockerfile 都是多阶段构建**，目的只有一个：让最终镜像里只留运行必需品。

| 镜像 | 阶段 1（构建） | 阶段 2（运行） | 体积 |
|---|---|---|---|
| backend | `maven:3.9-eclipse-temurin-17` 编译成 jar | `eclipse-temurin:17-jre` 只放 jar | ~250MB |
| frontend | `node:20-alpine` 执行 `npm run build` | `nginx:alpine` 托管 `dist/` | ~50MB |

**`docker-compose.yml` 的组织要点**：

- **网络**：四个服务都在自定义桥接网络 `todo-net` 上，容器间可以直接用**服务名**互相访问（`backend` 连 `mysql:3306`、`redis:6379`；`frontend` 连 `backend:8080`）。这就是 `application.yml` 里 `MYSQL_HOST` 默认写 `localhost`、而容器里要注入 `mysql` 的原因。
- **启动顺序**：`depends_on` + `condition: service_healthy`。MySQL 的健康检查用**业务账号** `mysqladmin ping`，真正验证「库已可用」，而不只是进程起来了 —— 否则后端会因连不上库而启动失败。
- **数据持久化**：`mysql-data`、`redis-data` 两个命名卷，`down` 不丢数据，`down -v` 才彻底重置。
- **配置外置**：数据库地址、密码全部通过 `environment` 注入，**不改一行代码**就能换环境。
- **端口映射**：`8081→80`（前端）、`8080→8080`（后端，直接访问 doc.html）、`3306`、`6379`（方便用客户端工具连）。

---

## 五、快速开始

### 方式一：Docker 一键启动（推荐）

前置要求：已安装 Docker 与 Docker Compose。

```bash
cd todo-app
docker compose up -d --build
```

首次启动需要下载镜像并编译，约 2~5 分钟。等待全部健康后访问：

| 入口 | 地址 |
|---|---|
| 应用页面 | http://localhost:8081 |
| 接口文档（Knife4j） | http://localhost:8081/doc.html |
| 接口文档（直连后端） | http://localhost:8080/doc.html |
| 后端 API 根路径 | http://localhost:8080/api/tasks |

查看状态与日志：

```bash
docker compose ps                 # 查看容器状态
docker compose logs -f backend    # 跟踪后端日志（能看到 SQL 与缓存命中日志）
```

停止与重置：

```bash
docker compose down               # 停止，保留数据
docker compose down -v            # 停止并删除数据卷（init.sql 会重新执行）
```

### 方式二：本地开发模式（改代码即时生效）

前置要求：JDK 17、Maven、Node 18+、Docker（只用来跑 MySQL 和 Redis）。

```bash
cd todo-app

# 1) 只启动 MySQL 和 Redis
docker compose up -d mysql redis

# 2) 启动后端（前后端分离时后端直连 localhost:3306 / localhost:6379）
cd backend
mvn spring-boot:run

# 3) 另开终端启动前端，Vite 会把 /api 代理到 8080
cd frontend
npm install
npm run dev
```

打开 http://localhost:5173 即可。前端热更新、后端可用 `mvn spring-boot:run` 配合 IDE 断点调试。

---

## 六、演示路径（建议按这个顺序操作）

1. **看数据**：打开首页，8 条任务已分页显示，未完成的排在前面、优先级高的更靠前。
2. **看 Redis 回源**：工具栏显示「Redis 回源（已写入缓存 60s）」，刷新页面后变成「Redis 命中 · 剩余 xx s」，数字每秒递减。
3. **搜索**：输入「报告」查询，只剩标题含该关键词的记录；页码自动回到第 1 页。
4. **筛选**：状态下拉选「已完成」，只剩 3 条。
5. **新增**：填标题（必填）、优先级、截止日期 → 保存，列表与统计数字同时变化，缓存标签回到「回源」。
6. **编辑 / 删除**：删除有二次确认；删掉本页最后一条时会自动回退一页。
7. **切换状态**：点表格里的开关，统计卡片实时变化 —— 证明缓存被正确失效。
8. **看接口文档**：点页头右上角「接口文档 Knife4j ↗」，直接在网页上发请求调试。

---

## 七、接口一览

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/tasks` | 分页查询，支持 `pageNum`、`pageSize`、`keyword`、`finished` |
| GET | `/api/tasks/stats` | 统计总数/已完成/未完成（Redis 缓存 60s） |
| POST | `/api/tasks` | 新增任务 |
| PUT | `/api/tasks/{id}` | 更新任务 |
| PATCH | `/api/tasks/{id}/finished` | 切换完成状态 |
| DELETE | `/api/tasks/{id}` | 逻辑删除 |

完整的请求参数、返回示例、错误码与 curl 命令见 **[docs/API接口文档.md](docs/API接口文档.md)**。

---

## 八、常见问题

**Q1：访问 `http://localhost:8081` 是白屏或 404？**
先 `docker compose ps` 确认 `todo-frontend` 是 Up。若刚启动完，等 10 秒再刷新（Nginx 起来有延迟）。

**Q2：页面能打开，但表格一直报「网络异常」？**
后端没起来或没连上 MySQL。执行 `docker compose logs -f backend`，若看到 `Communications link failure`，说明 MySQL 还没健康，稍等或重启后端容器。Vite 开发模式下则确认后端已在 8080 端口运行。

**Q3：统计数字不更新？**
这是缓存还没过期（60 秒）。正常写操作会主动删缓存并立即回源；若你直接改了数据库（绕过接口），需要等缓存过期或手动 `docker exec todo-redis redis-cli DEL todo:stats`。

**Q4：`/doc.html` 打不开？**
确认后端已启动，且访问的是 `8080` 或经前端代理的 `8081`。若前端是本地 5173 开发模式，Vite 已代理 `/doc.html`，也可直接访问。

**Q5：改了 `init.sql` 但数据没变？**
初始化脚本只在数据卷为空时执行一次。执行 `docker compose down -v` 后再 `up`。

**Q6：想换数据库密码？**
改 `docker-compose.yml` 里 mysql 的 `MYSQL_*` 与 backend 的 `MYSQL_*` 保持一致即可，代码无需改动（配置已外置）。

**Q7：后端构建卡在 `dependency:go-offline` 很久？**
默认走 Maven Central，国内网络可能极慢。项目已内置 `backend/settings.xml` 指向阿里云公共仓库，Dockerfile 里通过 `mvn -s settings.xml` 生效。若仍慢，检查 `docker run --rm alpine nslookup maven.aliyun.com` 能否解析。
本地开发（不走 Docker）想同样加速，可把该文件复制到 `~/.m2/settings.xml`。

**Q8：容器内域名解析失败（`Temporary failure in name resolution`）？**
这是 Docker 守护进程的 DNS 配置问题，不是项目问题。检查 `/etc/docker/daemon.json` 里的 `dns` 字段是否指向**本机真实可用**的 DNS：

```bash
cat /etc/resolv.conf              # 看宿主机用的是哪个 DNS
docker run --rm alpine cat /etc/resolv.conf   # 看容器里实际用的是哪个
```

两者不一致时，把宿主机的 nameserver 填进 `daemon.json` 的 `dns` 数组，然后 `sudo systemctl restart docker`（会重启所有容器）。

**Q9：MySQL 显示 healthy，但所有接口返回 500，日志报 `Table 'todo_db.t_task' doesn't exist`？**

这是 `init.sql` **没有成功执行**，最常见的两个原因：

**原因一：数据卷已存在，初始化被跳过**（MySQL 官方镜像只在数据目录为空时执行初始化）

```bash
docker volume ls | grep todo        # 如果还有 todo-app_mysql-data，就是它
docker compose down -v              # -v 是关键，删除数据卷
docker compose up -d
```

**原因二：`init.sql` 文件权限过严**（entrypoint 会切换到 `mysql` 用户执行脚本，读不到 600 权限的文件）

日志里的特征：

```
[Note] [Entrypoint]: running /docker-entrypoint-initdb.d/init.sql
docker-entrypoint.sh: line 75: /docker-entrypoint-initdb.d/init.sql: Permission denied
```

注意此时 MySQL **仍会正常启动**，容器显示 healthy，所以健康检查发现不了问题。

```bash
chmod 644 backend/src/main/resources/db/init.sql
docker compose down -v && docker compose up -d
```

**判断初始化是否真的成功**，看这两行日志：

```bash
docker compose logs mysql | grep -iE "running /docker-entrypoint-initdb.d|init process done|denied"
```

成功时会看到 `running /docker-entrypoint-initdb.d/init.sql` 紧跟着 `MySQL init process done.`，且**没有** `Permission denied`。

---

## 九、可以继续扩展的方向

- **加登录**：引入 `spring-boot-starter-security` 或 JWT + 拦截器，前端加路由守卫；用户信息放 Pinia。
- **加角色权限**：`@PreAuthorize` 或自定义注解 + AOP 做接口级鉴权。
- **缓存升级**：改用 Spring Cache 的 `@Cacheable/@CacheEvict`，或在 `afterCommit` 里删缓存。
- **接口规范化**：引入 `spring-boot-starter-validation` 的 `@Valid` + 全局异常处理器，统一处理参数错误。
- **多表关联**：把任务挂到「项目 / 分类」下，演示联表查询与 `@TableField(exist = false)`。
- **单元测试**：`@SpringBootTest` + MockMvc 测 Controller，`@MockBean` 测 Service。

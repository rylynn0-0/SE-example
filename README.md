# SE-example · 软件工程课程设计示例

一个**能跑起来、能讲清楚**的软件工程示例仓库，主体是单表 CRUD 的「待办清单」全栈项目：
**Vue 3 + Element Plus + Spring Boot + MyBatis-Plus + MySQL + Redis + Knife4j + Docker Compose**。

代码量刻意压小，每一层都有对应的讲解文档，适合当作课程设计骨架、答辩演示，或全栈入门的第一份可运行范例。

---

## 一、仓库结构

```
SE-example/
├── README.md                       # 本文件：仓库总览
├── .gitignore                      # 仓库级忽略规则
├── todo-app/                       # 示例主体（全栈待办清单）
│   ├── README.md                   # 项目说明：架构、技术栈对照、快速开始、FAQ
│   ├── docker-compose.yml          # 四容器编排：MySQL / Redis / 后端 / 前端
│   ├── docs/
│   │   ├── 技术栈代码对照.md        # 每项技术 → 对应代码位置
│   │   ├── API接口文档.md           # 接口清单、参数、返回示例、错误码
│   │   └── tutorials/              # 分层教程：00 总览 + 六章
│   ├── backend/                    # Spring Boot 3 + MyBatis-Plus（JDK 17）
│   └── frontend/                   # Vue 3 + Element Plus + Vite
├── .dns-test/                      # 容器 DNS 排查用的最小镜像（可选工具）
└── .research/                      # 本地研究记录备份（与示例代码无关）
```

> 只关心示例本身的话，直接读 [todo-app/README.md](todo-app/README.md) 即可，本页只做仓库级导航。

---

## 二、快速开始

前置：Docker 与 Docker Compose（本地开发另需 JDK 17、Maven、Node 18+）。

```bash
git clone https://github.com/rylynn0-0/SE-example.git
cd SE-example/todo-app
docker compose up -d --build
```

首次启动要下载镜像并编译，约 2~5 分钟。全部健康后：

| 入口 | 地址 |
|---|---|
| 应用页面 | http://localhost:8081 |
| 接口文档 Knife4j | http://localhost:8081/doc.html |
| 后端 API | http://localhost:8080/api/tasks |

停止与重置：

```bash
docker compose down      # 停止，保留数据
docker compose down -v   # 停止并删数据卷（init.sql 会重新执行）
```

不装 Docker 的本地开发方式（MySQL/Redis 用容器、前后端跑在宿主机）见
[todo-app/README.md 第五节](todo-app/README.md#五快速开始)。

---

## 三、这个示例演示了什么

- **完整的前后端分离链路**：Vue 页面 → axios → Vite/Nginx 代理 → Controller → Service → MyBatis-Plus → MySQL
- **分页 + 关键词搜索 + 状态筛选**，以及任务增删改查、完成状态切换（逻辑删除）
- **Redis Cache-Aside**：统计接口缓存 60 秒，页面直接显示「命中 / 回源」与剩余 TTL，效果看得见
- **Knife4j 在线接口文档**：注解即文档，可在网页上直接调试接口
- **Docker Compose 一键启动**：健康检查、服务名互访、配置外置、多阶段构建
- **分层文档**：从「前端 → 后端 → 数据库 → 两层耦合 → 必要技术」逐层讲清

---

## 四、文档导航

| 想了解 | 去哪里 |
|---|---|
| 项目总览、架构图、请求链路、目录说明 | [todo-app/README.md](todo-app/README.md) |
| 某段代码对应哪项技术 | [docs/技术栈代码对照.md](todo-app/docs/技术栈代码对照.md) |
| 每个接口的参数与返回结构 | [docs/API接口文档.md](todo-app/docs/API接口文档.md) |
| 系统入门（建议从这章开始） | [docs/tutorials/00-开始这里.md](todo-app/docs/tutorials/00-开始这里.md) |
| 常见报错与排查（白屏、连不上库、表不存在等） | [todo-app/README.md 第八节](todo-app/README.md#八常见问题) |

教程共 7 篇：`00` 总览、`01` 前端、`02` 后端、`03` 数据库、`04` 前后端耦合、`05` 后端与数据库耦合、`06` 必要技术（Knife4j / Nginx / Docker）。

---

## 五、环境要求

| 组件 | 版本 | 用途 |
|---|---|---|
| JDK | 17+ | 后端编译运行（Spring Boot 3.x 要求） |
| Maven | 3.9+ | 后端依赖与打包 |
| Node.js | 18+ | 前端构建 |
| Docker + Compose | 24+ | 四容器一键启动 |

后端已在 [backend/settings.xml](todo-app/backend/settings.xml) 内置阿里云 Maven 镜像，Docker 构建时通过 `mvn -s settings.xml` 生效。

---

## 六、其他目录说明

- **`.dns-test/`**：一个只有 3 行的 `alpine` 镜像，用来验证容器内域名解析（`nslookup repo.maven.apache.org`）。排 Docker 构建拉不到依赖时才会用到：

  ```bash
  docker build -t dns-test .dns-test
  docker run --rm dns-test
  ```

- **`.research/`**：本地工作区的研究记录备份（JSON），随仓库保留，但**与示例代码无关**，可以忽略或自行删除。

---

## 七、许可与用途

本仓库用于教学与示例演示，代码可自由取用与修改；其中的数据库密码、端口等均为本地演示配置，请勿直接用于生产环境。

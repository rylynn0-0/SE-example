# 17 · Windows 装 Docker 与本项目配置速查

> 一句话：09 讲的是「Docker 为什么这么配」，这一章讲「在你的 Windows 电脑上怎么装、装完怎么验证、四个配置文件分别改哪里」。

---

## 本章你会

- 在 Windows 10/11 上装好 Docker Desktop，并把 WSL2 后端跑通
- 用 4 条命令验证「装好了」而不是「装上了图标」
- 看懂本项目 4 个 Docker 配置文件（外加 2 个 `.dockerignore`）各自管什么、什么时候要动它
- 改端口、改密码、改时区时知道**要改哪几处、漏了会怎样**
- 排查 Windows 特有的坑：端口被本机 MySQL 占用、Docker 没启动、WSL 吃内存、拉镜像超时

> 原理（镜像/容器/分层/多阶段构建/Compose 四个关键配置）在 [09 必要技术 §四](09-必要技术.md)，
> 这里不重复推导，只讲落地。

---

## 一、先分清三件事

Windows 上装 Docker 之所以容易卡住，是因为它其实是三个东西叠在一起：

```
Docker Desktop（Windows 上的桌面程序，提供 docker / docker compose 命令）
    └── 依赖 WSL2（轻量 Linux 虚拟机，容器实际跑在这里）
            └── 依赖 BIOS 虚拟化（CPU 层面的开关，没开 WSL2 起不来）
```

三者缺一个，症状都不一样：

| 缺什么 | 典型症状 |
|---|---|
| BIOS 虚拟化没开 | WSL2 报 `0x80370102`，Docker 装完也起不来 |
| WSL2 没装/没更新 | `WSL 2 installation is incomplete`、Docker 一直 starting |
| Docker Desktop 没启动 | `error during connect: ... The system cannot find the file specified` |
| Desktop 装了但命令不认 | `docker : 无法将"docker"项识别为 cmdlet...` |

**版本要求**（低于这个版本装不上或跑不动）：

| 项目 | 要求 |
|---|---|
| Windows | Windows 11，或 Windows 10 22H2（build 19045）及以上，64 位 |
| 内存 | 建议 8GB 以上（Docker 自己会占 2～4GB） |
| 磁盘 | 预留 20GB 以上（镜像 + 数据卷 + WSL 虚拟磁盘） |
| CPU | 支持并已在 BIOS 开启虚拟化（VT-x / AMD-V） |

> Windows 10 家庭版也能用 —— 走 WSL2 后端不需要 Hyper-V。

---

## 二、安装前检查

先确认虚拟化和系统版本，别等装到一半才发现要进 BIOS。

```powershell
# ① 系统版本（图形窗口，看"版本"和"OS 内部版本"）
winver

# ② CPU 虚拟化是否已开启：True 才算开
Get-CimInstance Win32_Processor | Select-Object Name, VirtualizationFirmwareEnabled

# ③ 当前 WSL 状态（没装会提示"尚未安装"）
wsl --status
wsl --list --verbose
```

如果 ② 是 `False`：先别急着改 BIOS，用任务管理器交叉验证一次 ——
性能 → CPU → 右下角「虚拟化」。那里显示「已启用」就是好的
（部分主板/虚拟化嵌套环境下 `VirtualizationFirmwareEnabled` 会误报 `False`）。

确认真的没开，再重启进 BIOS（开机按 F2 / Del / F10，视主板而定），
打开 `Intel Virtualization Technology` / `SVM Mode` / `AMD-V`，保存退出。

---

## 三、步骤一：装 WSL2

**Windows 10 2004（build 19041）及以上 / Windows 11**，用一条命令：

```powershell
# 以管理员身份打开 PowerShell
wsl --install
```

这条命令会做三件事：启用「适用于 Linux 的 Windows 子系统」和「虚拟机平台」两个功能、
安装 WSL2 内核、装一个默认发行版（Ubuntu）。**装完必须重启。**

重启后再收尾：

```powershell
wsl --update                      # 更新 WSL 到最新版
wsl --set-default-version 2       # 新装的发行版默认用 WSL2
wsl --status                      # 应显示"默认版本: 2"
wsl --list --verbose              # 发行版列表，VERSION 列必须是 2
```

**老系统（build 19041 以下）没有 `wsl --install`**，手动开关：

```powershell
# 管理员 PowerShell，两条命令都要执行，然后重启
dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart
dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart
```

重启后下载安装 WSL2 内核更新包（微软官方的 `wsl_update_x64.msi`），再执行
`wsl --set-default-version 2`。

> 判断 WSL2 是否真的可用：`wsl --status` 里能看到「默认版本: 2」。
> 只要这里不是 2，Docker Desktop 一定起不来，先解决它再往下走。

---

## 四、步骤二：装 Docker Desktop

### 4.1 两种安装方式

**方式 A：winget（推荐，一条命令）**

```powershell
winget install --id Docker.DockerDesktop -e --source winget
```

**方式 B：官网下载 exe**

到 [Docker Desktop for Windows](https://docs.docker.com/desktop/setup/install/windows-install/)
下载 `Docker Desktop Installer.exe`，双击安装。

安装向导里两个勾选项：

| 选项 | 怎么选 | 为什么 |
|---|---|---|
| Use WSL 2 instead of Hyper-V (recommended) | **勾上** | 本项目跑的是 Linux 容器，WSL2 后端是官方推荐路径 |
| Add shortcut to desktop | 随意 | 只是快捷方式 |

安装完成后一般会提示注销/重启。**注销重登一次**，否则当前终端里 `docker` 命令可能还不可用
（安装程序把当前用户加进了 `docker-users` 组，组成员身份要重新登录才生效）。

### 4.2 首次启动与必要设置

从开始菜单启动 **Docker Desktop**，接受协议，登录页可以点 **Skip** 跳过（本项目不需要账号）。

首次启动会初始化 WSL 虚拟机，等左下角鲸鱼图标变绿、状态显示 **Engine running** 再操作。

打开 **Settings**（右上角齿轮），按需确认这几项：

| 位置 | 设置 | 建议 |
|---|---|---|
| General | Start Docker Desktop when you sign in | 勾上，省得每次手动开 |
| General | Use the WSL 2 based engine | 保持勾选 |
| Resources → Advanced | Disk image location | C 盘紧张就改到 D 盘（改动会迁移现有镜像，耗时） |
| Resources → Advanced | CPUs / Memory | 默认即可；机器只有 8GB 内存就压到 2～3GB |
| Resources → WSL integration | 启用 `docker-desktop` | 默认已开，装了别的发行版可勾选 |
| Docker Engine | JSON 配置 | 见第五节（镜像加速） |

### 4.3 验证：四条命令

```powershell
docker version                          # 客户端 + 服务端两段都出来，才算通
docker compose version                  # 应有 v2.x
docker info --format '{{.OSType}}'      # 必须输出 linux
docker run --rm hello-world             # 能拉镜像、能跑容器
```

四条全过，说明「引擎 + 网络 + 拉镜像 + 跑容器」整条链路都正常。
任何一条失败，对照第九节排查，不要急着 `docker compose up`。

> 注意 `docker info` 显示 `OSType: windows` 时是 Windows 容器模式，本项目跑不了。
> 右键托盘鲸鱼图标 → **Switch to Linux containers**。

---

## 五、步骤三：配镜像加速（可选，但国内基本必需）

`docker compose up --build` 要拉 `mysql:8.0`、`redis:7-alpine`、`maven:3.9-eclipse-temurin-17`、
`eclipse-temurin:17-jre`、`node:22-alpine`、`nginx:alpine` 六个镜像，直连 Docker Hub 常常
`TLS handshake timeout`。

**配置入口**：Settings → **Docker Engine**，把 JSON 改成：

```json
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://docker.1ms.run"
  ]
}
```

点 **Apply & Restart**，等引擎重启完，验证：

```powershell
docker info | Select-String -Pattern "Registry Mirrors" -Context 0,3
```

能看到配置的地址就生效了。

> 加速器地址的可用性变化很快，失效是常态。配了仍然超时，就把这一项删掉走直连，
> 或者换一个当前可用的源；**不要照抄网上过期的列表**。
> 本项目镜像都是常见官方镜像，任何一个可用源基本都能拉到。

---

## 六、步骤四：在 Windows 上跑起本项目

```powershell
cd D:\code\SE-example\todo-app     # 换成你自己的路径
docker compose up -d --build
docker compose ps
```

首次 2～5 分钟（下载 + Maven 编译 + npm 构建）。`ps` 里 `todo-mysql` / `todo-redis`
应显示 `healthy`，`todo-backend` / `todo-frontend` 为 `Up`。

验证四个地址：

| # | 地址 | 应该看到 |
|---|---|---|
| 1 | http://localhost:8081 | 待办列表，8 条演示数据 |
| 2 | http://localhost:8081/doc.html | Knife4j 文档页 |
| 3 | http://localhost:8080/api/tasks | 原始 JSON |
| 4 | http://localhost:8080/doc.html | 直连后端的文档 |

### 6.1 PowerShell 与 bash 的四点差异

教程里其他章节的 shell 示例是 bash 写法，在 PowerShell 里要注意：

| 场景 | bash 写法 | PowerShell 写法 |
|---|---|---|
| 调接口 | `curl http://localhost:8080/api/tasks` | `curl.exe http://localhost:8080/api/tasks` |
| 看环境变量 | `echo $TZ` | `$env:TZ` |
| 管道过滤 | `docker ps \| grep todo` | `docker ps \| Select-String todo` |
| 路径分隔符 | `/` | 两种都行，推荐 `\` |

> **第一条最容易踩**：Windows PowerShell 5.1 里 `curl` 是 `Invoke-WebRequest` 的别名，
> 输出的是对象不是响应体。要真 curl 就写 `curl.exe`（PowerShell 7 已去掉这个别名，但 `curl.exe` 同样适用）。

### 6.2 目录路径建议

- 项目路径**尽量用纯英文**（`D:\code\SE-example`）。中文路径在绝大多数场景没问题，
  但个别工具链在容器内处理非 ASCII 路径时会出奇怪错误，而错误信息完全指不到路径上。
- 仓库**没有 `.gitattributes`**，Windows 默认的 `core.autocrlf=true` 会把 `.sql`、`.conf`
  改成 CRLF。本项目不受影响（MySQL 和 Nginx 都能处理 CRLF），但如果你以后加了
  `.sh` 脚本，记得在仓库里建 `.gitattributes` 把它们固定成 LF。

---

## 七、本项目 Docker 配置速查

仓库里一共四个 Docker 配置文件，外加两个 `.dockerignore`（用来缩小构建上下文）：

| 文件 | 管什么 | 什么时候要改 |
|---|---|---|
| `todo-app/docker-compose.yml` | 四个容器、端口、数据卷、环境变量、启动顺序、网络 | 改端口 / 改密码 / 加服务 |
| `todo-app/backend/Dockerfile` | 后端镜像：Maven 编译 → JRE 运行 | 升 JDK 版本、加构建参数 |
| `todo-app/frontend/Dockerfile` | 前端镜像：Node 构建 → Nginx 托管 | 升 Node 版本、改构建命令 |
| `todo-app/frontend/nginx.conf` | 静态托管 + `/api` 反代 + gzip + 缓存 | 改代理规则、加 HTTPS |
| `todo-app/backend/.dockerignore`、`frontend/.dockerignore` | 排除 `target/`、`node_modules/`、`.git/` | 构建上下文过大时 |

### 7.1 四个服务一眼看懂

| 服务 | 镜像 / 构建 | 宿主机端口 | 容器内端口 | 数据卷 | 关键环境变量 | 健康检查 |
|---|---|---|---|---|---|---|
| `mysql` | `mysql:8.0` | 3306 | 3306 | `mysql-data` + `init.sql`（只读） | `MYSQL_*`、`TZ`、`LANG` | `mysqladmin ping`（用业务账号） |
| `redis` | `redis:7-alpine` | 6379 | 6379 | `redis-data` | — | `redis-cli ping` |
| `backend` | `build: ./backend` | 8080 | 8080 | — | `MYSQL_HOST=mysql` 等 | 无（靠 `depends_on` 等别人） |
| `frontend` | `build: ./frontend` | 8081 | 80 | — | — | 无 |

### 7.2 环境变量：从 compose 到 `application.yml`

| compose 里注入 | `application.yml` 里的占位符 | 容器内取值 | 本地开发时取值 |
|---|---|---|---|
| `MYSQL_HOST` | `${MYSQL_HOST:localhost}` | `mysql` | `localhost` |
| `MYSQL_PORT` | `${MYSQL_PORT:3306}` | `3306` | `3306` |
| `MYSQL_DB` | `${MYSQL_DB:todo_db}` | `todo_db` | `todo_db` |
| `MYSQL_USER` / `MYSQL_PASSWORD` | 同名占位符 | `todo` / `todo123456` | 同左 |
| `REDIS_HOST` / `REDIS_PORT` | 同名占位符 | `redis` / `6379` | `localhost` / `6379` |
| `TZ` | — | `Asia/Shanghai` | 由系统决定 |

这就是同一份 jar 既能跑容器又能跑本机的原因：**代码里没有写死地址，全部由环境变量兜底**。

### 7.3 改宿主机端口：只动冒号左边

```yaml
ports:
  - "8081:80"      # 左边 = 宿主机；右边 = 容器
```

| 想解决 | 改法 | 连带影响 |
|---|---|---|
| 8080 被占用 | `"8082:8080"` | 浏览器改访问 `http://localhost:8082/...`；Nginx 里的 `backend:8080` **不用改**（容器内网不变） |
| 3306 被本机 MySQL 占用 | 在 `todo-app/.env` 写一行 `MYSQL_HOST_PORT=13306`（compose 里已写成 `${MYSQL_HOST_PORT:-3306}:3306`）；也可以直接把映射改成 `"3307:3306"` | Navicat 连 `localhost:13306`（或 3307）；后端仍用 `mysql:3306`，代码不用动 |
| 8081 被占用 | `"8082:80"` | 注意别和上一条撞成同一个端口 |

**关键认知**：容器之间靠**服务名 + 容器内端口**通信，宿主机映射改多少都不影响它们。
所以 `backend` 的 `MYSQL_HOST: mysql` 永远不变。

> Windows 上最常见的冲突是 3306：本机装了 MySQL（服务名通常叫 `MySQL80` / `MySQL96`），
> 它随开机自启，`docker compose up` 就会报
> `Error response from daemon: ports are not available: exposing port TCP 0.0.0.0:3306`。
> 两种解法：① 停掉本机服务（`services.msc` 里改手动）；
> ② 保留本机 MySQL，让容器换宿主机端口 —— 在 `todo-app/.env` 里写 `MYSQL_HOST_PORT=13306` 即可，
> 该文件已被 `.gitignore` 忽略，不影响别人克隆后的默认 3306。
>
> 改完用 `docker compose config` 检查最终值：`published` 应该是 `"13306"`。

### 7.4 改密码：要改三处，漏一处必挂

以业务账号 `todo` 的密码为例：

```yaml
mysql:
  environment:
    MYSQL_PASSWORD: todo123456          # ① 建账号时用的密码
  healthcheck:
    test: ["CMD", "mysqladmin", "ping", "-h", "127.0.0.1", "-utodo", "-ptodo123456"]
                                            # ② 健康检查用同一个密码登录
backend:
  environment:
    MYSQL_PASSWORD: todo123456          # ③ 后端连库用的密码
```

漏掉 ② 的现象很有迷惑性：MySQL 其实好好的，但容器一直 `unhealthy`，
后端因为 `depends_on: service_healthy` 永远不启动，看起来像「后端起不来」。

改完还要**删数据卷**才生效：

```powershell
docker compose down -v
docker compose up -d
```

因为 `MYSQL_PASSWORD` 只在**数据目录为空时**生效，已有卷里密码还是旧的。

> `MYSQL_ROOT_PASSWORD` 只影响 root 登录，改它不影响业务账号。

### 7.5 动过 `init.sql` 之后

| 操作 | `init.sql` 会执行吗 |
|---|---|
| 首次 `up` | 会 |
| 再次 `up` / `restart` | 不会 |
| `down` 后 `up` | 不会（卷还在） |
| `down -v` 后 `up` | 会 |

只看一句话：**初始化脚本绑定的是「卷为空」，不是「容器重建」**。

### 7.6 常用命令（PowerShell）

```powershell
docker compose ps                        # 状态
docker compose config                    # 变量替换后的最终配置（查端口最准）
docker compose logs -f backend           # 跟踪后端日志（能看到 SQL）
docker compose restart backend           # 只重启后端
docker compose down                      # 停止，保留数据
docker compose down -v                   # 停止并删卷（init.sql 重跑）
docker compose build --no-cache backend  # 强制重建后端镜像
docker exec -it todo-mysql mysql -utodo -ptodo123456 todo_db   # 进库
docker system df                         # 看空间占用
```

---

## 八、安装完成后建议做的两件事

### 8.1 限制 WSL2 的内存（8GB 机器必做）

WSL2 默认会吃掉最多一半物理内存，且不会主动还。新建 `C:\Users\你的用户名\.wslconfig`：

```ini
[wsl2]
memory=4GB
processors=4
swap=2GB
```

保存后执行 `wsl --shutdown`（会关掉 Docker 引擎），再重新启动 Docker Desktop 生效。

### 8.2 定期回收磁盘

Docker 的镜像层、停止的容器、悬空卷都会占空间，`docker system df` 能看到明细：

```powershell
docker system prune -a      # 删除所有未使用的镜像、容器、网络（不含命名卷）
docker volume prune         # 删未使用的卷 —— 本项目会删掉 mysql-data，慎用
```

---

## 九、Windows 常见问题

| 现象 | 原因 | 处理 |
|---|---|---|
| `docker : 无法将"docker"项识别为 cmdlet` | PATH 未生效 | 关掉终端重开；仍不行检查 `C:\Program Files\Docker\Docker\resources\bin` 是否在 PATH |
| `error during connect: ... The system cannot find the file specified` | Docker Desktop 没启动 | 启动 Desktop，等鲸鱼图标变绿再执行 |
| `WSL 2 installation is incomplete` | WSL2 内核缺失/过旧 | `wsl --update`；仍报错就重装内核更新包 |
| `0x80370102` / `Please enable the Virtual Machine Platform` | BIOS 虚拟化未开，或「虚拟机平台」功能未启用 | 进 BIOS 开 VT-x/AMD-V；管理员执行 `dism` 启用 `VirtualMachinePlatform` 后重启 |
| `Ports are not available: ... bind: ... 3306` | 本机装了 MySQL 服务占着 3306 | `netstat -ano \| findstr :3306` 找到 PID；`services.msc` 停掉 `MySQL80`，或把映射改成 `"3307:3306"` |
| 拉镜像 `TLS handshake timeout` / `i/o timeout` | 直连 Docker Hub 不通 | 配 registry-mirrors（第五节）；确认代理软件没有拦截 |
| `no configuration file provided: not found` | 不在 `todo-app` 目录下执行 | `cd` 到有 `docker-compose.yml` 的目录 |
| `The compose file is invalid` | YAML 缩进错（用了 Tab） | 改成空格，注意 `services:` 下两格缩进 |
| 容器 `unhealthy` 但日志正常 | 健康检查命令里的密码/账号与 `environment` 不一致 | 对照 7.4 三处密码 |
| 后端 `Communications link failure` | MySQL 首次初始化还没完成 | 等 `todo-mysql` 变 `healthy`；`docker compose logs mysql` 看初始化日志 |
| 构建很慢，卡在 `dependency:go-offline` | 走 Maven Central | 项目已内置 `backend/settings.xml` 阿里云镜像，检查网络能否解析 `maven.aliyun.com` |
| 内存吃满、风扇狂转 | WSL2 未限内存 | 按 8.1 配 `.wslconfig` |
| C 盘空间暴涨 | 镜像 + WSL 虚拟磁盘 | 改 Disk image location；`docker system df` 后 `prune` |
| 与 VMware / 安卓模拟器冲突 | 虚拟化资源竞争 | 改用 WSL2 后端（默认即是）；同时只开一个虚拟化大户 |
| `chmod` 不生效 | Windows 挂载的文件系统没有 Unix 权限位 | 本项目不需要；确需执行就在 WSL 发行版里对 Linux 侧文件操作 |

---

## 十、自测：能不能答上来

1. `docker version` 有输出，但 `docker compose up` 报连不上引擎，最可能是什么没做？
2. 为什么 `docker info` 显示 `OSType: windows` 时本项目跑不起来？
3. 把前端映射从 `8081:80` 改成 `8091:80` 后，还需要改哪里？
4. 改了 `MYSQL_PASSWORD` 但登录还是旧密码，为什么？
5. 本机 3306 已被 MySQL 服务占用，怎么让项目继续用 3306 以外的端口跑起来？

<details>
<summary>参考答案</summary>

1. Docker Desktop 没启动（或没等引擎就绪）。命令行工具在，引擎不在。
2. Windows 容器模式跑的是 Windows 镜像，本项目所有镜像（mysql/redis/maven/jre/node/nginx）都是 Linux 镜像，需要切到 Linux containers。
3. 只需要浏览器改用 `http://localhost:8091`。`nginx.conf` 里 `proxy_pass http://backend:8080` 走的是容器内网，不受宿主机映射影响。
4. `MYSQL_PASSWORD` 只在数据目录为空时初始化账号；已有 `mysql-data` 卷里仍是旧密码，要 `down -v` 重建。
5. 把端口映射改成 `"3307:3306"`；如果本机 MySQL 不需要，也可以停掉它的服务。后端 `MYSQL_HOST: mysql` 不用改。

</details>

---

## 十一、下一步

- Docker 的概念与四个关键配置（网络、卷、健康检查、端口）→ [09 必要技术 §四](09-必要技术.md)
- 想在本机改代码、只把 MySQL/Redis 交给 Docker → [README 第五节「方式二」](../../README.md#五快速开始)
- 启动后白屏 / 502 / 表不存在等运行期问题 → [09 §七](09-必要技术.md) 与 [README 第八节](../../README.md#八常见问题)

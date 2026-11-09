# ============================================================
# 前端镜像：多阶段构建
#   阶段 1（build）：用 Node 镜像执行 npm ci + npm run build，
#                    把 Vue 源码编译成纯静态文件（html/js/css）
#   阶段 2（运行）：用 Nginx 托管这些静态文件，并把 /api 转发给后端
# 好处：最终镜像里没有 Node、没有 node_modules，体积约 50MB
# ============================================================

# ---------- 阶段 1：构建 ----------
# 用 Node 22 而不是 20：Element Plus 的传递依赖 @vueuse/core@15 声明了 engines.node >= 22。
# 用 20 只会出现 EBADENGINE 警告、不影响构建，但保持版本对齐更干净。
FROM node:22-alpine AS build
WORKDIR /app

# 先拷贝依赖清单 + lock 文件，利用 Docker 层缓存。
# 把 package-lock.json 一起拷进来，配合 npm ci：
#   1. 容器内安装的版本与本地完全一致（可复现，不会因为"今天又发了新小版本"而变）
#   2. 比 npm install 更快（跳过版本解析）
COPY package*.json ./
RUN npm ci --no-audit --no-fund

# 再拷贝源码并打包，产物输出到 /app/dist
COPY . .
RUN npm run build

# ---------- 阶段 2：托管 ----------
FROM nginx:alpine

# 用自定义配置替换 Nginx 默认站点配置（默认配置不会转发 /api）
RUN rm -f /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 把构建产物复制到 Nginx 的静态资源目录
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
ENV TZ=Asia/Shanghai

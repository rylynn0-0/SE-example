import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端地址：本地开发时后端跑在 8080
const BACKEND = 'http://localhost:8080'

/**
 * Vite 配置。
 *
 * 核心是 server.proxy：把浏览器的请求转发给后端。
 *
 * 为什么要代理，而不是让前端直接请求 http://localhost:8080？
 *  1. 浏览器同源策略会拦截「端口不同」的请求，产生跨域问题；
 *  2. 走代理后，浏览器看到的是同源请求（都发往 5173），不存在跨域；
 *  3. 接口地址可以统一写成 /api/tasks 这样的相对路径，
 *     换环境（本地 / Docker / 线上）时前端代码一个字都不用改。
 *
 * 注意：proxy 只在 `npm run dev` 时生效。
 * 生产/Docker 环境下由 Nginx 承担同样的转发职责（见 frontend/nginx.conf）。
 */
export default defineConfig({
  plugins: [vue()],

  server: {
    host: '0.0.0.0', // 允许局域网内其它设备访问（默认只监听 127.0.0.1）
    port: 5173,
    open: false,
    proxy: {
      // 业务接口
      '/api': {
        target: BACKEND,
        changeOrigin: true // 把 Host 头改成目标地址，某些后端会校验
      },
      // Knife4j 文档页面及其依赖的静态资源/规范 JSON
      '/doc.html': { target: BACKEND, changeOrigin: true },
      '/webjars': { target: BACKEND, changeOrigin: true },
      '/v3/api-docs': { target: BACKEND, changeOrigin: true },
      '/swagger-ui': { target: BACKEND, changeOrigin: true }
    }
  },

  build: {
    outDir: 'dist',
    // 生产构建时不再生成 sourcemap，减小体积
    sourcemap: false
  }
})

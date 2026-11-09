import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * axios 实例配置。
 *
 * baseURL 设为 '/api'（相对路径），所以调用 request.get('/tasks')
 * 实际请求的是 /api/tasks。
 *
 * 为什么用相对路径而不是 http://localhost:8080/api？
 *   - 开发环境：Vite 的 server.proxy 会把 /api 转发给 8080；
 *   - Docker 环境：Nginx 的 proxy_pass 会把 /api 转发给 backend:8080。
 * 前端代码前后两种环境完全一致，不需要改任何地址，也天然没有跨域问题。
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 10000 // 10 秒超时，避免请求一直挂着
})

/**
 * 请求拦截器。
 *
 * 在请求真正发出前执行，适合统一加认证信息、埋点、loading 等。
 * 本示例没有登录功能，因此保持原样返回；
 * 加上 JWT 鉴权时只需在这里补一行 config.headers.Authorization。
 */
request.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error)
)

/**
 * 响应拦截器（成功分支）。
 *
 * 后端所有接口都返回 {code, message, data} 三段式结构。
 * 这里把它「拆开」：业务代码拿到的直接是 data，
 * 不用每次都写 res.data.data，调用处更干净。
 *
 * @param {object} response axios 的响应对象
 * @returns {*} 成功时返回后端的 data 字段；业务失败时抛错
 */
request.interceptors.response.use(
  (response) => {
    const body = response.data

    // 极少数非统一格式的响应（比如指向后端的静态资源），原样返回
    if (body === null || typeof body !== 'object' || body.code === undefined) {
      return body
    }

    // code === 200 表示业务成功，把 data 交给调用方
    if (body.code === 200) {
      return body.data
    }

    // 业务失败（例如「任务标题不能为空」）：弹提示并中断调用链
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message || '请求失败'))
  },

  /**
   * 响应拦截器（失败分支）。
   *
   * 处理网络层错误：后端没启动（ECONNREFUSED）、超时、HTTP 4xx/5xx 等。
   * 给出的是「人话」提示，而不是让用户在控制台看 Network Error。
   *
   * @param {object} error axios 的错误对象
   */
  (error) => {
    const message =
      error.response?.data?.message || '网络异常，请确认后端服务已启动（默认 8080 端口）'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request

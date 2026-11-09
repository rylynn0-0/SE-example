import request from './request'

/**
 * 待办任务的接口封装层。
 *
 * 为什么要单独抽一个 task.js，而不是在页面里直接写 axios？
 *  1. 接口路径集中管理：后端改路径时只改这一个文件；
 *  2. 页面只关心「调哪个方法」，不关心 HTTP 方法和 URL 细节；
 *  3. 每个方法都能写清楚参数和返回类型，便于复用与测试。
 *
 * 注意：由于 request.js 的响应拦截器已经拆掉了 {code,message,data} 外壳，
 * 这里每个方法 resolve 出来的就已经是业务数据本身。
 */

/**
 * 分页查询任务列表（支持标题模糊搜索 + 完成状态筛选）。
 *
 * 对应后端：GET /api/tasks?pageNum=1&pageSize=5&keyword=xx&finished=0
 *
 * @param {object} params 查询参数
 * @param {number} params.pageNum   页码，从 1 开始
 * @param {number} params.pageSize  每页条数
 * @param {string} [params.keyword] 标题关键词，为空则不筛选
 * @param {number|null} [params.finished] 0=未完成，1=已完成，null=全部
 * @returns {Promise<{records: Array, total: number, current: number, pages: number}>}
 */
export function pageTasks(params) {
  // get 的第二个参数是 config，params 会被 axios 自动拼成查询字符串
  return request.get('/tasks', { params })
}

/**
 * 新增任务。
 *
 * 对应后端：POST /api/tasks
 *
 * @param {object} data 任务数据，至少包含 title
 * @returns {Promise<object>} 新增后的完整任务对象（含自增 ID）
 */
export function createTask(data) {
  return request.post('/tasks', data)
}

/**
 * 按 ID 更新任务。
 *
 * 对应后端：PUT /api/tasks/{id}
 *
 * @param {number} id   任务主键
 * @param {object} data 要更新的字段
 * @returns {Promise<object>} 更新后的完整任务对象
 */
export function updateTask(id, data) {
  return request.put(`/tasks/${id}`, data)
}

/**
 * 切换任务的完成状态（未完成 ↔ 已完成）。
 *
 * 对应后端：PATCH /api/tasks/{id}/finished
 *
 * @param {number} id 任务主键
 * @returns {Promise<object>} 切换后的任务对象
 */
export function toggleTask(id) {
  return request.patch(`/tasks/${id}/finished`)
}

/**
 * 删除任务（后端执行的是逻辑删除）。
 *
 * 对应后端：DELETE /api/tasks/{id}
 *
 * @param {number} id 任务主键
 * @returns {Promise<void>}
 */
export function deleteTask(id) {
  return request.delete(`/tasks/${id}`)
}

/**
 * 查询任务统计信息。
 *
 * 对应后端：GET /api/tasks/stats
 * 这是观察 Redis 缓存的窗口：
 * 第一次调用返回 fromCache=false，60 秒内再次调用返回 fromCache=true 且 ttl 递减。
 *
 * @returns {Promise<{total:number, finished:number, unfinished:number, fromCache:boolean, ttl:number}>}
 */
export function getStats() {
  return request.get('/tasks/stats')
}

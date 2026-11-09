import { createRouter, createWebHistory } from 'vue-router'
import TodoView from '../views/TodoView.vue'

/**
 * 路由表。
 *
 * 本示例只有一个页面，但仍然引入路由，原因是：
 *  1. 结构上更接近真实项目，后续加「统计报表」「系统设置」只需在这里加一条；
 *  2. 页面组件的加载与 URL 解耦，页面内部不用关心自己在哪个地址。
 */
const routes = [
  {
    path: '/',
    name: 'todo',
    component: TodoView,
    meta: { title: '待办清单' }
  },
  // 兜底路由：访问不存在的地址时回到首页，避免白屏
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  /**
   * history 模式：URL 形如 http://localhost:8081/，没有 # 号。
   * 代价是刷新页面时服务器必须能把 / 返回 index.html，
   * 这就是 nginx.conf 里 try_files $uri $uri/ /index.html 的作用。
   */
  history: createWebHistory(),
  routes
})

/**
 * 全局后置钩子：每次路由跳转结束后更新浏览器标签页标题。
 *
 * @param {object} to 即将进入的路由对象
 */
router.afterEach((to) => {
  document.title = to.meta?.title ? `${to.meta.title} · 待办清单` : '待办清单'
})

export default router

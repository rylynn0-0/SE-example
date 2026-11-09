import { createApp } from 'vue'

// Element Plus 及其默认样式（必须引入，否则组件没有外观）
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 中文语言包：让分页的「条/页」、日期面板的星期等都显示中文
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import router from './router'
import './styles/global.css'

/**
 * 前端应用入口（被 index.html 以 ES Module 方式加载）。
 *
 * 这里完成三件事：
 *  1. 创建 Vue 应用实例，并挂载到 <div id="app">；
 *  2. 注册 Element Plus 插件（注册全部组件 + 全局样式 + 中文语言包）；
 *  3. 注册 Vue Router，让 <router-view /> 能渲染对应页面。
 */
const app = createApp(App)

// 安装 Element Plus。第二个参数是全局配置，这里只配置了语言包。
// 若追求更小的打包体积，也可以改成按需引入（unplugin-vue-components），
// 本示例为「最简」选择了全量引入，配置最少。
app.use(ElementPlus, { locale: zhCn })

// 安装路由
app.use(router)

// 挂载：Vue 会把 App 组件渲染进 index.html 里的 #app 节点
app.mount('#app')

<!--
  根组件 App.vue
  只负责整体布局：顶部页头 + 内容区。
  具体页面由 <router-view /> 根据当前路由渲染（本示例只有 TodoView 一个页面）。
-->
<script setup>
/**
 * 根组件不需要额外的响应式状态，
 * 只保留模板与样式即可，因此 script 中暂无逻辑。
 */
</script>

<template>
  <el-container class="layout">
    <!-- 页头：标题栏 + 接口文档入口 -->
    <el-header class="app-header" height="72px">
      <div class="brand">
        <div class="logo">✓</div>
        <div class="brand-text">
          <h1>待办清单</h1>
          <p>Vue3 + Element Plus · Spring Boot + MyBatis-Plus · MySQL · Redis</p>
        </div>
      </div>

      <div class="header-actions">
        <!-- 相对路径 /doc.html 会被 Vite(开发) 或 Nginx(Docker) 代理到后端，
             与前端页面同源，所以不会出现跨域或端口写死的问题 -->
        <el-link href="/doc.html" target="_blank" type="primary">
          接口文档 Knife4j ↗
        </el-link>
      </div>
    </el-header>

    <!-- 内容区：路由页面渲染在这里 -->
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped>
.layout {
  min-height: 100%;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  padding: 0 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-text h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.brand-text p {
  margin: 2px 0 0;
  font-size: 12px;
  color: #909399;
}

.app-main {
  padding: 20px 24px 40px;
}
</style>

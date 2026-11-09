package com.example.todo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 全局配置类（目前只做跨域 CORS）。
 *
 * <p><b>为什么需要它？</b>
 * 开发时前端跑在 http://localhost:5173，后端跑在 http://localhost:8080，
 * 浏览器同源策略会把这种「端口不同」的请求判定为跨域并拦截。
 *
 * <p><b>说明：</b>本项目实际上更推荐用代理解决跨域——
 * 前端 Vite 的 proxy（开发环境）和 Nginx 的 proxy_pass（生产/Docker 环境）
 * 都把 /api 转发给后端，此时浏览器看到的是同源请求。
 * 这里的 CORS 配置是「第二道保险」，方便你直接换端口裸调接口时不被拦。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 注册全局跨域规则。
     *
     * @param registry Spring MVC 提供的跨域注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")            // 对所有路径生效
                .allowedOriginPatterns("*")   // 允许任意来源（配合 allowCredentials 必须用 Pattern 形式）
                .allowedMethods("*")          // 允许 GET/POST/PUT/DELETE/PATCH 等全部方法
                .allowedHeaders("*")          // 允许任意请求头
                .allowCredentials(true)       // 允许携带 Cookie
                .maxAge(3600);                // 预检请求（OPTIONS）结果缓存 1 小时，减少重复预检
    }
}

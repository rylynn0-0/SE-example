package com.example.todo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j（OpenAPI 3）接口文档配置类。
 *
 * <p>工作方式：springdoc 在启动时扫描所有 {@code @RestController}，
 * 读取 {@code @Tag} / {@code @Operation} / {@code @Schema} 注解，
 * 生成一份符合 OpenAPI 3 规范的 JSON（默认地址 /v3/api-docs），
 * Knife4j 再把这个 JSON 渲染成可读、可调试的中文文档页面 /doc.html。
 *
 * <p>本类只负责「文档的封面信息」（标题、版本、联系人、描述）；
 * 具体的接口条目由 Controller 上的注解自动生成。
 */
@Configuration
public class Knife4jConfig {

    /**
     * 构建 OpenAPI 文档的全局元信息。
     *
     * @return OpenAPI 对象，springdoc 会把它合并进最终的 /v3/api-docs 响应中
     */
    @Bean
    public OpenAPI todoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        // 文档标题
                        .title("待办清单 Todo API")
                        // 文档描述：把整套技术栈写出来，答辩时一眼可见
                        .description("Vue3 + Element Plus 前端 · Spring Boot + MyBatis-Plus 后端 · "
                                + "MySQL 持久化 · Redis 缓存 · Knife4j 文档")
                        .version("1.0.0")
                        .contact(new Contact().name("软工示例").url("http://localhost:8081")));
    }
}

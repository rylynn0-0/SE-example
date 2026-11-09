package com.example.todo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目启动类（程序入口）。
 *
 * <p>三个注解的作用：
 * <ul>
 *   <li>{@code @SpringBootApplication} —— 组合注解，等价于
 *       {@code @Configuration + @EnableAutoConfiguration + @ComponentScan}。
 *       它会自动扫描本包（com.example.todo）及其子包下的
 *       @Component / @Service / @RestController / @Configuration 并注册为 Bean。</li>
 *   <li>{@code @MapperScan} —— 告诉 MyBatis 去哪个包下查找 Mapper 接口，
 *       并为每个接口自动生成动态代理实现（我们不需要写实现类）。</li>
 * </ul>
 *
 * <p>启动后：
 * <ul>
 *   <li>接口基地址：http://localhost:8080/api</li>
 *   <li>Knife4j 文档：http://localhost:8080/doc.html</li>
 * </ul>
 */
@SpringBootApplication
@MapperScan("com.example.todo.mapper")
public class TodoApplication {

    /**
     * 程序入口方法。
     *
     * <p>{@link SpringApplication#run} 会完成一整套启动流程：
     * 创建 IoC 容器 → 加载 application.yml → 初始化数据源与 Redis 连接池 →
     * 启动内嵌 Tomcat → 注册所有 Controller 路由。
     *
     * @param args 命令行参数，例如 --server.port=9090 可覆盖配置端口
     */
    public static void main(String[] args) {
        SpringApplication.run(TodoApplication.class, args);
    }
}

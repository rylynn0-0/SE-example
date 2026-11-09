package com.example.todo.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类。
 *
 * <p>MyBatis-Plus 的分页不是「查全表再内存截取」，
 * 而是通过拦截器改写 SQL，自动拼上 {@code LIMIT offset, size}
 * 并额外执行一条 {@code SELECT COUNT(*)}。
 * 不注册这个拦截器，{@code page()} 会退化成查全部数据，分页参数完全失效。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 核心拦截器，并挂上 MySQL 方言的分页插件。
     *
     * <p>返回值必须是 {@link MybatisPlusInterceptor}，MyBatis-Plus 会自动把它
     * 织入 MyBatis 的 Executor 责任链中，对每条 SQL 生效。
     *
     * @return 已装载分页内部拦截器的插件对象
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 指定 MySQL 方言：不同数据库的分页语法不同（MySQL 用 LIMIT，Oracle 用 ROWNUM）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}

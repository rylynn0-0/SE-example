package com.example.todo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.todo.entity.Task;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务表的数据访问层（DAO）。
 *
 * <p><b>这个接口一行 SQL 都没写，为什么就能用？</b>
 * 因为它继承了 MyBatis-Plus 的 {@link BaseMapper}，该接口已经内置了
 * 17 个通用方法：insert、deleteById、updateById、selectById、
 * selectList、selectPage、selectCount 等。
 *
 * <p>启动时 MyBatis 会为这个接口生成动态代理对象（MapperProxy），
 * 代理对象根据方法名和泛型参数 &lt;Task&gt; 自动拼出对应的 SQL 语句。
 * 复杂查询（多表联查、复杂聚合）仍然可以在这里用 {@code @Select} 注解或 XML 手写。
 *
 * <p>{@code @Mapper} 与启动类上的 {@code @MapperScan} 二选一即可，
 * 这里两个都有，属于双保险（不影响运行）。
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    // 无需声明任何方法：单表增删改查全部由 BaseMapper 提供。
    // 需要自定义 SQL 时，在此添加方法并在 XML 或注解中给出对应语句。
}

package com.example.todo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.todo.dto.TaskQuery;
import com.example.todo.dto.TaskStats;
import com.example.todo.entity.Task;

/**
 * 任务业务逻辑层接口。
 *
 * <p><b>为什么 Controller 不直接调 Mapper？</b>
 * 三层架构的职责划分：
 * <ul>
 *   <li>Controller —— 只做「收参数、校验、返回统一格式」，不含业务逻辑；</li>
 *   <li>Service —— 业务规则 + 事务边界 + <b>缓存失效</b>，本项目的核心；</li>
 *   <li>Mapper —— 只管和数据库打交道。</li>
 * </ul>
 * 好处是：写操作要清缓存这件事只写在 Service 里一处，
 * 不会因为将来新增一个入口（比如定时任务、消息队列消费）就漏掉。
 *
 * <p>继承 {@link IService} 后自动拥有 save / getById / updateById /
 * removeById / count / page 等通用 CRUD 能力。
 */
public interface TaskService extends IService<Task> {

    /**
     * 分页查询任务列表，支持标题模糊搜索与完成状态筛选。
     *
     * <p>排序规则：未完成优先 → 优先级高的优先 → 新建的优先。
     * 这样用户打开页面第一眼看到的就是「最该做的事」。
     *
     * @param query 分页与筛选条件
     * @return 分页结果，包含当前页数据 records 和总数 total
     */
    IPage<Task> pageTasks(TaskQuery query);

    /**
     * 查询任务统计信息，优先读 Redis 缓存。
     *
     * <p>采用 Cache-Aside（旁路缓存）模式：
     * 先查缓存 → 命中直接返回 → 未命中则查库并回写缓存（60 秒过期）。
     * 这是 Redis 最经典的用法，也是本示例中 Redis 的唯一落点。
     *
     * @return 总数 / 已完成 / 未完成，以及本次是否命中缓存
     */
    TaskStats stats();

    /**
     * 新增任务，并让统计缓存失效。
     *
     * @param task 待新增的任务，title 必填；priority 不传默认 2（中）
     * @return 新增成功后从数据库重新查出的完整任务（含自增 ID 与创建时间）
     */
    Task createTask(Task task);

    /**
     * 按 ID 更新任务，并让统计缓存失效。
     *
     * @param id   任务主键
     * @param task 要更新的字段（null 字段默认不更新）
     * @return 更新后从数据库重新查出的任务
     */
    Task updateTask(Long id, Task task);

    /**
     * 按 ID 删除任务（逻辑删除），并让统计缓存失效。
     *
     * @param id 任务主键
     * @return true 表示删除成功；false 表示该 ID 不存在或已被删除
     */
    boolean removeTask(Long id);

    /**
     * 切换任务的完成状态（未完成 ↔ 已完成），并让统计缓存失效。
     *
     * @param id 任务主键
     * @return 切换后的任务；任务不存在时返回 null
     */
    Task toggleFinished(Long id);
}

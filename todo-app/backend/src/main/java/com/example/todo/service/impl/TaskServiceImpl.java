package com.example.todo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.todo.dto.TaskQuery;
import com.example.todo.dto.TaskStats;
import com.example.todo.entity.Task;
import com.example.todo.mapper.TaskMapper;
import com.example.todo.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 任务业务逻辑实现类 —— <b>本项目技术栈的交汇中心</b>。
 *
 * <p>这一个类同时用到了三样东西：
 * <ol>
 *   <li>继承 {@link ServiceImpl} → 获得 MyBatis-Plus 的全部通用 CRUD（操作 MySQL）；</li>
 *   <li>注入 {@link StringRedisTemplate} → 读写 Redis 缓存；</li>
 *   <li>注入 {@link ObjectMapper} → 把对象序列化成 JSON 字符串存进 Redis。</li>
 * </ol>
 *
 * <p><b>缓存策略：Cache-Aside（旁路缓存）</b>
 * <pre>
 *   读：查 Redis → 命中则返回 ────────────────► 结束
 *              └→ 未命中 → 查 MySQL → 写回 Redis（60s）→ 返回
 *   写：改 MySQL → 删除 Redis 中相关 key（下次读自然回源）
 * </pre>
 * 选择「删除」而不是「更新」缓存，是为了避免并发写导致缓存与数据库不一致。
 *
 * <p><b>为什么所有 Redis 操作都套了 try/catch？</b>
 * 缓存是「加速层」，不是「数据源」。Redis 挂掉时系统应该降级为直接查数据库，
 * 而不是让整个接口报 500。这是生产环境的基本要求，也是示例里刻意保留的写法。
 *
 * <p>{@code @RequiredArgsConstructor}（Lombok）会为所有 final 字段生成构造器，
 * 配合 Spring 的构造器注入，final 字段因此天然不可变、不会为 null。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    /** Redis 中存统计数据的 key。命名规范建议用「业务:对象」的形式，便于排查 */
    public static final String STATS_KEY = "todo:stats";

    /** 统计缓存的过期时间（秒）。设短一点便于演示「过期后重新回源」 */
    private static final long STATS_TTL_SECONDS = 60L;

    /** Redis 操作模板：key 和 value 都用字符串，最直观，适合教学示例 */
    private final StringRedisTemplate redisTemplate;

    /** Jackson 的 JSON 序列化器，Spring Boot 已自动配置好，直接注入即可 */
    private final ObjectMapper objectMapper;

    // ==================================================================
    //  一、查询
    // ==================================================================

    /**
     * 分页查询任务列表。
     *
     * <p>用 {@link LambdaQueryWrapper} 拼查询条件的好处：
     * {@code Task::getTitle} 是方法引用，字段名写错会在编译期报错，
     * 而传统字符串写法 {@code "title"} 只能等到运行时报错。
     *
     * <p>每个条件的第一个参数是「布尔开关」，为 false 时该条件不会被拼进 SQL，
     * 因此不需要写一堆 if-else 来判断参数是否为空。
     *
     * @param query 分页参数与筛选条件
     * @return 分页结果对象（records 是当前页数据，total 是总条数）
     */
    @Override
    public IPage<Task> pageTasks(TaskQuery query) {
        // 1. 构造分页对象：第 pageNum 页，每页 pageSize 条
        Page<Task> page = new Page<>(query.getPageNum(), query.getPageSize());

        // 2. 构造查询条件
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                // 标题模糊搜索：仅当关键词非空时才拼接 LIKE '%keyword%'
                .like(StringUtils.hasText(query.getKeyword()), Task::getTitle, query.getKeyword())
                // 状态筛选：仅当传入 finished 时才拼接 finished = ?
                .eq(query.getFinished() != null, Task::getFinished, query.getFinished())
                // 排序：未完成的排在前面（0 < 1）
                .orderByAsc(Task::getFinished)
                // 优先级高的排前面（3 > 2 > 1）
                .orderByDesc(Task::getPriority)
                // 同优先级时，新建的任务排前面
                .orderByDesc(Task::getId);

        // 3. page() 由 MyBatis-Plus 提供：
        //    自动先执行 SELECT COUNT(*) 得到总数，再执行带 LIMIT 的数据查询
        return this.page(page, wrapper);
    }

    /**
     * 查询任务统计（总数 / 已完成 / 未完成），优先走 Redis 缓存。
     *
     * <p>这是本示例中 Redis 的唯一读入口，完整演示了 Cache-Aside 的读取流程：
     * 先查缓存，命中就用；未命中才查数据库，并把结果写回缓存。
     *
     * @return 统计结果；其中 fromCache 告诉前端这次是否命中了缓存
     */
    @Override
    public TaskStats stats() {
        // ---------- 第 1 步：尝试读缓存 ----------
        try {
            String cachedJson = redisTemplate.opsForValue().get(STATS_KEY);
            if (cachedJson != null) {
                // 反序列化成对象
                TaskStats cached = objectMapper.readValue(cachedJson, TaskStats.class);
                // 查询该 key 的剩余存活秒数（-1 表示永不过期，-2 表示 key 不存在）
                Long ttl = redisTemplate.getExpire(STATS_KEY, TimeUnit.SECONDS);
                cached.setFromCache(true);
                cached.setTtl(ttl == null ? 0L : ttl);
                log.debug("统计信息命中 Redis 缓存，剩余 {} 秒", cached.getTtl());
                return cached;
            }
        } catch (Exception e) {
            // Redis 不可用时不中断业务，降级为直接查数据库
            log.warn("读取 Redis 失败，降级查询数据库：{}", e.getMessage());
        }

        // ---------- 第 2 步：缓存未命中，查询数据库 ----------
        // count() 由 MyBatis-Plus 提供，会自动带上 deleted = 0 的逻辑删除条件
        long total = this.count();
        long finished = this.count(new LambdaQueryWrapper<Task>().eq(Task::getFinished, 1));

        TaskStats stats = new TaskStats();
        stats.setTotal(total);
        stats.setFinished(finished);
        stats.setUnfinished(total - finished);
        stats.setFromCache(false);
        stats.setTtl(STATS_TTL_SECONDS);

        // ---------- 第 3 步：把结果写回缓存，设置 60 秒过期 ----------
        try {
            String json = objectMapper.writeValueAsString(stats);
            // set(key, value, timeout, unit)：60 秒后自动删除，防止数据长期不刷新
            redisTemplate.opsForValue().set(STATS_KEY, json, STATS_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("统计信息已写入 Redis 缓存，TTL={} 秒", STATS_TTL_SECONDS);
        } catch (Exception e) {
            log.warn("写入 Redis 失败（不影响本次返回）：{}", e.getMessage());
        }

        return stats;
    }

    // ==================================================================
    //  二、写操作（每次写完都要让缓存失效）
    // ==================================================================

    /**
     * 新增任务。
     *
     * <p>处理了三个细节：
     * <ol>
     *   <li>优先级没传时给默认值 2（中），避免前端漏传导致列表排序异常；</li>
     *   <li>写完立刻删除统计缓存，否则统计卡片会继续显示旧数字最长 60 秒；</li>
     *   <li>返回「重新查出的对象」，因为自增 ID 和创建时间由数据库生成，
     *       不重新查的话返回给前端的这两个字段是 null。</li>
     * </ol>
     *
     * @param task 前端提交的任务数据
     * @return 落库后的完整任务对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task createTask(Task task) {
        // 优先级兜底：没传就是「中」
        if (task.getPriority() == null) {
            task.setPriority(2);
        }
        // 新任务默认未完成
        if (task.getFinished() == null) {
            task.setFinished(0);
        }
        // save() 来自 ServiceImpl → 实际执行 INSERT，并把自增主键回填到 task.id
        this.save(task);

        // 统计数据变了，删除缓存让下次读取重新回源
        clearStatsCache();

        // 重新查询，补齐 create_time / update_time 等数据库生成的字段
        return this.getById(task.getId());
    }

    /**
     * 更新任务。
     *
     * <p>只更新前端传了值的字段（MyBatis-Plus 默认跳过 null 字段），
     * 但 description 和 dueDate 例外 —— 它们被标注了 {@code FieldStrategy.ALWAYS}，
     * 因为用户需要能把备注和日期「清空」。
     *
     * @param id   任务主键，以 URL 路径参数为准（防止请求体里的 id 被篡改）
     * @param task 要更新的字段
     * @return 更新后的完整任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task updateTask(Long id, Task task) {
        // 以路径中的 id 为准，忽略请求体里可能伪造的 id
        task.setId(id);

        // 更新时不应通过这个接口修改「逻辑删除标记」
        task.setDeleted(null);

        // updateById 会生成 UPDATE t_task SET ... WHERE id = ? AND deleted = 0
        this.updateById(task);

        // 状态/优先级都可能影响统计，保守起见直接失效缓存
        clearStatsCache();

        return this.getById(id);
    }

    /**
     * 删除任务（逻辑删除）。
     *
     * <p>因为实体上有 {@code @TableLogic}，这里调用的 removeById
     * 实际执行的是 {@code UPDATE t_task SET deleted = 1 WHERE id = ?}，
     * 数据仍在表里，只是查询时自动被过滤掉。
     *
     * @param id 任务主键
     * @return true=删除成功，false=该任务不存在（或已删除）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTask(Long id) {
        boolean removed = this.removeById(id);
        // 无论是否真的删到了，都清一次缓存最安全（代价极低）
        clearStatsCache();
        return removed;
    }

    /**
     * 切换完成状态。
     *
     * <p>典型的两步操作：先按 ID 查出当前值，再取反后更新。
     * 用 {@code task.getFinished() == 1 ? 0 : 1} 而不是 {@code 1 - finished}，
     * 是为了兼容 finished 为 null 的脏数据。
     *
     * @param id 任务主键
     * @return 切换后的任务；任务不存在时返回 null
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task toggleFinished(Long id) {
        Task task = this.getById(id);
        // 任务不存在（或已被逻辑删除）时直接返回 null，由 Controller 转成 404
        if (task == null) {
            return null;
        }

        // 取反：1 → 0，其它（0/null）→ 1
        task.setFinished(task.getFinished() != null && task.getFinished() == 1 ? 0 : 1);

        // 只更新这一条记录的 finished 字段
        this.updateById(task);

        // 完成数量变了，统计缓存必须失效
        clearStatsCache();

        return this.getById(id);
    }

    // ==================================================================
    //  三、私有辅助方法
    // ==================================================================

    /**
     * 删除 Redis 中的统计数据缓存。
     *
     * <p>所有写操作（新增/更新/删除/切换状态）结束后都要调用它，
     * 否则统计卡片会显示过期数字。集中成一个私有方法，避免各处漏写。
     *
     * <p><b>工程上的诚实说明：</b>这里是在事务提交前删除缓存的。
     * 极端并发下可能出现「缓存已被删、事务还没提交、另一个请求又读到了旧值写回缓存」的情况。
     * 生产环境更严谨的做法是用 {@code TransactionSynchronizationManager}
     * 注册 afterCommit 回调，在事务真正提交后再删缓存。本示例为保持简洁做了省略。
     */
    private void clearStatsCache() {
        try {
            redisTemplate.delete(STATS_KEY);
            log.debug("已清除 Redis 统计缓存：{}", STATS_KEY);
        } catch (Exception e) {
            // 缓存删不掉也不能影响业务，等 60 秒 TTL 自然过期即可
            log.warn("清除 Redis 缓存失败：{}", e.getMessage());
        }
    }
}

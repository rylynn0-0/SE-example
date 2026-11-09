package com.example.todo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.todo.common.Result;
import com.example.todo.dto.TaskQuery;
import com.example.todo.dto.TaskStats;
import com.example.todo.entity.Task;
import com.example.todo.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务接口层（Controller）。
 *
 * <p><b>职责边界：</b>只做三件事——
 * 接收 HTTP 请求、做最基础的参数校验、把结果包装成统一格式返回。
 * 业务规则和缓存逻辑一律放在 Service 里。
 *
 * <p><b>RESTful 风格约定：</b>
 * <ul>
 *   <li>用 URL 表示资源：{@code /api/tasks} 表示任务集合，{@code /api/tasks/{id}} 表示单个任务；</li>
 *   <li>用 HTTP 方法表示动作：
 *       GET 查、POST 增、PUT 改、DELETE 删、PATCH 局部改；</li>
 *   <li>接口本身不出现动词（不写 /getTaskList 这种），动作由 HTTP 方法表达。</li>
 * </ul>
 *
 * <p><b>注解说明：</b>
 * <ul>
 *   <li>{@code @RestController} = {@code @Controller + @ResponseBody}，
 *       方法返回值会被 Jackson 自动转成 JSON；</li>
 *   <li>{@code @Tag} / {@code @Operation} / {@code @Parameter} 是 Knife4j（OpenAPI）注解，
 *       只影响文档展示，不影响运行，删掉接口照样能跑。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "待办任务", description = "任务的增删改查、分页搜索与统计")
public class TaskController {

    /** 业务层接口。用构造器注入（final + @RequiredArgsConstructor），便于单元测试时替换 */
    private final TaskService taskService;

    /**
     * 分页查询任务列表。
     *
     * <p>请求示例：{@code GET /api/tasks?pageNum=1&pageSize=5&keyword=报告&finished=0}
     *
     * <p>参数为什么没有 {@code @RequestParam} 注解也能绑定？
     * 因为参数是一个 POJO（{@link TaskQuery}），Spring MVC 默认会按字段名
     * 从 URL 查询参数中取值并调用 setter 注入，这叫做「对象参数绑定」。
     *
     * @param query 分页与筛选条件，全部可选
     * @return 分页结果：records（当前页数据）+ total（总数）+ pages（总页数）等
     */
    @GetMapping
    @Operation(summary = "分页查询任务", description = "支持标题模糊搜索与完成状态筛选，未完成的任务排在前面")
    public Result<IPage<Task>> page(TaskQuery query) {
        return Result.ok(taskService.pageTasks(query));
    }

    /**
     * 查询任务统计信息。
     *
     * <p>这个接口是演示 Redis 缓存的窗口：
     * 第一次请求返回 fromCache=false（查了数据库并写入缓存），
     * 60 秒内再次请求返回 fromCache=true 且 ttl 递减。
     * 任何写操作都会删除缓存，使下一次请求重新回源。
     *
     * @return 总数 / 已完成 / 未完成 + 缓存命中标记
     */
    @GetMapping("/stats")
    @Operation(summary = "任务统计", description = "返回总数、已完成、未完成；结果在 Redis 中缓存 60 秒")
    public Result<TaskStats> stats() {
        return Result.ok(taskService.stats());
    }

    /**
     * 新增任务。
     *
     * <p>{@code @RequestBody} 表示把请求体里的 JSON 反序列化成 Task 对象。
     *
     * <p>这里做了最基础的「标题非空」校验。生产项目通常会用
     * {@code spring-boot-starter-validation} 的 {@code @NotBlank} 配合全局异常处理器，
     * 本示例为减少依赖，用几行 if 手动校验。
     *
     * @param task 任务数据（JSON 请求体）
     * @return 新增成功后的完整任务对象（含自增 ID 与创建时间）
     */
    @PostMapping
    @Operation(summary = "新增任务", description = "标题必填；优先级不传默认 2（中），新任务默认未完成")
    public Result<Task> create(@RequestBody Task task) {
        if (!StringUtils.hasText(task.getTitle())) {
            return Result.fail(400, "任务标题不能为空");
        }
        return Result.ok(taskService.createTask(task));
    }

    /**
     * 更新任务。
     *
     * <p>先判断任务是否存在，再执行更新——否则会返回「成功」但实际什么都没改，
     * 前端会误以为保存生效了。
     *
     * @param id   任务主键（URL 路径参数）
     * @param task 要更新的字段（JSON 请求体）
     * @return 更新后的完整任务对象
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新任务", description = "按 ID 更新标题、备注、优先级、截止日期")
    public Result<Task> update(
            @Parameter(description = "任务 ID", example = "1") @PathVariable Long id,
            @RequestBody Task task) {

        if (!StringUtils.hasText(task.getTitle())) {
            return Result.fail(400, "任务标题不能为空");
        }
        // 不存在则返回 404，避免「假成功」
        if (taskService.getById(id) == null) {
            return Result.fail(404, "任务不存在");
        }
        return Result.ok(taskService.updateTask(id, task));
    }

    /**
     * 切换任务的完成状态（未完成 ↔ 已完成）。
     *
     * <p>为什么用 PATCH 而不是 PUT？
     * PUT 的语义是「用请求体整体替换资源」，而这里只改 finished 一个字段，
     * 属于「局部更新」，PATCH 更符合 REST 语义，也让接口意图更明确。
     *
     * @param id 任务主键
     * @return 切换后的任务对象
     */
    @PatchMapping("/{id}/finished")
    @Operation(summary = "切换完成状态", description = "未完成与已完成互相切换，对应列表里的开关")
    public Result<Task> toggle(
            @Parameter(description = "任务 ID", example = "1") @PathVariable Long id) {

        Task task = taskService.toggleFinished(id);
        if (task == null) {
            return Result.fail(404, "任务不存在");
        }
        return Result.ok(task);
    }

    /**
     * 删除任务（逻辑删除）。
     *
     * <p>数据库里执行的是 {@code UPDATE t_task SET deleted = 1}，
     * 记录并没有真正消失，只是后续查询会自动过滤掉。
     *
     * @param id 任务主键
     * @return 成功时 data 为 null
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除任务", description = "逻辑删除：只把 deleted 标记置为 1，数据可追溯")
    public Result<Void> delete(
            @Parameter(description = "任务 ID", example = "1") @PathVariable Long id) {

        if (!taskService.removeTask(id)) {
            return Result.fail(404, "任务不存在");
        }
        return Result.ok();
    }
}

package com.example.todo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 任务统计数据对象，供首页三张统计卡片使用。
 *
 * <p><b>为什么要单独定义它，而不是让前端自己数？</b>
 * 前端只拿到了「当前页」的 5 条数据，无法知道全表有多少条。
 * 所以统计必须由后端执行 COUNT 查询得来。
 *
 * <p><b>为什么还要 fromCache 和 ttl 两个字段？</b>
 * 这是本示例用来「让 Redis 缓存可见」的设计：
 * 前端根据 fromCache 显示「Redis 命中」还是「回源数据库」，
 * 根据 ttl 显示缓存还剩多少秒过期。答辩演示时一目了然。
 */
@Data
@Schema(description = "任务统计信息")
public class TaskStats {

    /** 任务总数（不含已逻辑删除的） */
    @Schema(description = "任务总数", example = "8")
    private long total;

    /** 已完成数量 */
    @Schema(description = "已完成数量", example = "3")
    private long finished;

    /** 未完成数量 */
    @Schema(description = "未完成数量", example = "5")
    private long unfinished;

    /**
     * 本次数据是否直接来自 Redis 缓存。
     * true = 命中缓存（没有查数据库）；false = 回源数据库后刚写入缓存。
     */
    @Schema(description = "是否命中 Redis 缓存", example = "true")
    private boolean fromCache;

    /** 缓存的剩余有效期（秒）。命中时为剩余时间，回源时为完整 TTL */
    @Schema(description = "缓存剩余有效秒数", example = "42")
    private long ttl;
}

package com.example.todo.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务实体，对应数据库表 {@code t_task}。
 *
 * <p><b>这是「技术栈交汇点」最关键的一个类：</b>
 * <ul>
 *   <li>MyBatis-Plus 靠 {@code @TableName/@TableId/@TableField} 把它映射成表结构；</li>
 *   <li>Jackson 靠 {@code @JsonFormat} 决定日期怎么序列化成 JSON 给前端；</li>
 *   <li>Knife4j 靠 {@code @Schema} 生成接口文档里的字段说明；</li>
 *   <li>Lombok 的 {@code @Data} 在编译期生成 getter/setter/toString。</li>
 * </ul>
 *
 * <p>注意：这里的字段是「驼峰」，数据库列是「下划线」，
 * 由 application.yml 中的 {@code map-underscore-to-camel-case: true} 自动完成映射。
 */
@Data
@TableName("t_task")
@Schema(description = "待办任务")
public class Task {

    /**
     * 主键 ID。
     * {@code IdType.AUTO} 表示由数据库自增生成，插入时不用自己赋值。
     */
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键 ID", example = "1")
    private Long id;

    /**
     * 任务标题，必填，最长 100 字符。
     * 列表页的关键词搜索就是对这一列做 LIKE 模糊匹配。
     */
    @Schema(description = "任务标题", example = "完成软工课程设计开题报告")
    private String title;

    /**
     * 任务备注/详细描述，可为空。
     *
     * <p>{@code updateStrategy = FieldStrategy.ALWAYS} 表示「更新时即使值为 null 也写入 SQL」。
     * 默认策略会忽略 null 字段，导致用户清空备注后数据库里的旧值删不掉；加上它才能清空。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "任务备注", example = "整理论文选题、技术选型与进度安排")
    private String description;

    /** 优先级：1=低，2=中，3=高。列表按它倒序排列，让重要任务排前面 */
    @Schema(description = "优先级：1低 2中 3高", example = "3")
    private Integer priority;

    /** 完成状态：0=未完成，1=已完成。由列表里的开关切换 */
    @Schema(description = "完成状态：0未完成 1已完成", example = "0")
    private Integer finished;

    /**
     * 截止日期（只有年月日）。
     *
     * <p>{@code @JsonFormat} 保证传给前端的格式是 "2026-11-10"，
     * 而不是 Jackson 默认的数组 [2026,11,10]；同时 {@code ALWAYS} 策略支持把它清空。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "截止日期", example = "2026-11-10")
    private LocalDate dueDate;

    /**
     * 创建时间。
     * 插入时不赋值（保持 null），由数据库列默认值 {@code CURRENT_TIMESTAMP} 填上，
     * 这样开发者在业务代码里不用手写时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "2026-11-08 20:00:00")
    private LocalDateTime createTime;

    /** 更新时间，由数据库的 {@code ON UPDATE CURRENT_TIMESTAMP} 自动维护 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "2026-11-08 20:30:00")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记：0=正常，1=已删除。
     *
     * <p>{@code @TableLogic} 让 MyBatis-Plus 自动改写 SQL：
     * 查询时追加 {@code AND deleted = 0}；
     * 删除时执行 {@code UPDATE ... SET deleted = 1} 而不是真正 DELETE。
     * 好处是数据可追溯，坏处是要记得统计时也带上这个条件（框架已自动处理）。
     *
     * <p>{@code @JsonIgnore} 让它不出现在接口响应里，前端无需关心。
     */
    @TableLogic
    @JsonIgnore
    private Integer deleted;
}

package com.example.todo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分页查询参数对象（DTO = Data Transfer Object，数据传输对象）。
 *
 * <p>为什么不直接把参数写在方法签名里？
 * 因为参数一多，{@code page(Long, Long, String, Integer)} 这种签名
 * 调用方很容易传错顺序。封装成对象后：
 * <ul>
 *   <li>Spring MVC 会把 URL 上的同名查询参数自动 set 进来，例如
 *       {@code GET /api/tasks?pageNum=1&pageSize=5&keyword=报告}；</li>
 *   <li>Knife4j 能把它渲染成一组带说明的表单参数。</li>
 * </ul>
 *
 * <p>字段初始值就是「默认值」：Spring 先 new 出对象，再覆盖 URL 上传来的参数，
 * 所以不传 pageNum 时仍然是 1。
 */
@Data
@Schema(description = "任务分页查询参数")
public class TaskQuery {

    /** 当前页码，从 1 开始 */
    @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
    private Long pageNum = 1L;

    /** 每页条数 */
    @Schema(description = "每页条数", example = "5", defaultValue = "5")
    private Long pageSize = 5L;

    /** 标题关键词，模糊匹配；为空表示不筛选 */
    @Schema(description = "标题关键词（模糊搜索）", example = "报告")
    private String keyword;

    /** 完成状态筛选：0 只看未完成，1 只看已完成，null 表示全部 */
    @Schema(description = "完成状态：0未完成 1已完成，不传表示全部", example = "0")
    private Integer finished;
}

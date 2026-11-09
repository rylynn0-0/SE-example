package com.example.todo.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统一响应结果包装类。
 *
 * <p>为什么要包一层？如果 Controller 直接返回 List&lt;Task&gt;，
 * 前端就无法区分「请求成功但数据为空」和「请求失败」。
 * 统一用 {code, message, data} 三段式后：
 * <ul>
 *   <li>前端只在 code == 200 时取 data；</li>
 *   <li>失败时直接展示 message，不用去猜 HTTP 状态码的含义；</li>
 *   <li>所有接口响应结构一致，前端 axios 拦截器可以统一处理。</li>
 * </ul>
 *
 * @param <T> 业务数据的类型，例如 Task、IPage&lt;Task&gt;、Void
 */
@Data
@Schema(description = "统一响应结果")
public class Result<T> {

    /** 业务状态码：200 表示成功，其它值表示失败 */
    @Schema(description = "业务状态码，200 为成功", example = "200")
    private Integer code;

    /** 提示信息：成功时为「成功」，失败时为具体原因 */
    @Schema(description = "提示信息", example = "成功")
    private String message;

    /** 业务数据：失败时通常为 null */
    @Schema(description = "业务数据")
    private T data;

    /**
     * 构造一个成功响应（不带数据），用于删除这类没有返回值的操作。
     *
     * @param <T> 数据类型
     * @return code=200、message="成功"、data=null 的结果对象
     */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /**
     * 构造一个成功响应并携带业务数据。
     *
     * @param data 需要返回给前端的数据
     * @param <T>  数据类型
     * @return code=200、message="成功"、data=data 的结果对象
     */
    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("成功");
        result.setData(data);
        return result;
    }

    /**
     * 构造一个失败响应，默认业务状态码为 500。
     *
     * @param message 给用户看的失败原因，例如「任务标题不能为空」
     * @param <T>     数据类型
     * @return code=500 的结果对象
     */
    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }

    /**
     * 构造一个自定义状态码的失败响应。
     *
     * @param code    业务状态码，例如 404 表示资源不存在
     * @param message 失败原因
     * @param <T>     数据类型
     * @return 指定 code 的结果对象
     */
    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}

-- ============================================================
-- 待办清单示例 · 数据库初始化脚本
--
-- 执行方式：
--   1) Docker：本文件被挂载到容器 /docker-entrypoint-initdb.d/ 下，
--      MySQL 容器「首次」启动（数据目录为空）时会自动执行；
--   2) 本地 MySQL：mysql -uroot -p < init.sql
--
-- 注意：Docker 下只有第一次创建数据卷时才会执行。
--       若想重跑，先执行 docker compose down -v 删除数据卷。
-- ============================================================

-- 显式声明客户端连接字符集。
-- 为什么必须有这一行：MySQL 容器内执行本脚本时用的 mysql 客户端，
-- 若环境变量 LANG 未设置，客户端会退回 latin1，导致 UTF-8 中文被当成
-- latin1 解读，存进库后变成「å®Œæˆ」这种双重编码乱码。
-- SET NAMES utf8mb4 让本次会话的 客户端/连接/结果 全部使用 utf8mb4。
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS todo_db
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE todo_db;

-- ------------------------------------------------------------
-- 任务表
-- 设计要点：
--   1. 主键自增，由 MySQL 生成，业务代码不关心
--   2. create_time / update_time 用数据库默认值与 ON UPDATE 自动维护，
--      这样无论从哪个入口写数据，时间都准确
--   3. deleted 是逻辑删除标记，配合 MyBatis-Plus 的 @TableLogic 使用
--   4. 给 finished 和 title 建索引：前者用于状态筛选与排序，
--      后者用于关键词模糊搜索（前缀匹配时才能用上）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_task;
CREATE TABLE t_task
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    title       VARCHAR(100)    NOT NULL COMMENT '任务标题',
    description VARCHAR(500)             DEFAULT NULL COMMENT '任务备注',
    priority    TINYINT         NOT NULL DEFAULT 1 COMMENT '优先级：1低 2中 3高',
    finished    TINYINT         NOT NULL DEFAULT 0 COMMENT '完成状态：0未完成 1已完成',
    due_date    DATE                     DEFAULT NULL COMMENT '截止日期',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (id),
    KEY idx_finished (finished),
    KEY idx_title (title)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='待办任务表';

-- ------------------------------------------------------------
-- 演示数据：8 条任务，其中 3 条已完成、5 条未完成
-- 用 CURDATE() 相对当前日期生成截止日，保证任何时候打开都有「紧迫感」
-- ------------------------------------------------------------
INSERT INTO t_task (title, description, priority, finished, due_date)
VALUES ('完成软工课程设计开题报告', '整理论文选题、技术选型与进度安排', 3, 0, DATE_ADD(CURDATE(), INTERVAL 2 DAY)),
       ('搭建 Spring Boot 后端工程骨架', '引入 Web、MyBatis-Plus、Redis、Knife4j 依赖', 3, 1, DATE_SUB(CURDATE(), INTERVAL 3 DAY)),
       ('设计 t_task 数据表结构', '确定字段、索引与逻辑删除方案', 2, 1, DATE_SUB(CURDATE(), INTERVAL 2 DAY)),
       ('联调任务分页与关键词搜索接口', '验证分页拦截器与 LambdaQueryWrapper 条件拼接', 2, 1, DATE_SUB(CURDATE(), INTERVAL 1 DAY)),
       ('接入 Redis 缓存统计接口', '实现 Cache-Aside：读缓存、回源、写操作失效', 2, 0, DATE_ADD(CURDATE(), INTERVAL 1 DAY)),
       ('用 Knife4j 补齐接口文档注解', '为每个接口补充 @Tag、@Operation 与 @Schema', 1, 0, DATE_ADD(CURDATE(), INTERVAL 3 DAY)),
       ('编写 Dockerfile 与 docker-compose', '四容器编排：mysql/redis/backend/frontend', 2, 0, DATE_ADD(CURDATE(), INTERVAL 4 DAY)),
       ('准备答辩演示脚本', '演示增删改查、分页搜索、缓存命中与接口文档', 1, 0, DATE_ADD(CURDATE(), INTERVAL 7 DAY));

-- 创建XXL-JOB数据库
CREATE DATABASE xxl_job 
WITH ENCODING 'UTF8' 
OWNER = postgres;

-- 连接到xxl_job数据库
\c xxl_job;

-- 执行XXL-JOB表结构（使用之前创建的xxl-job-pg.sql）
-- 这里可以包含完整的表创建脚本

-- 验证数据库创建
SELECT current_database();

-- 显示所有表
\dt

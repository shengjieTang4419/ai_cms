-- XXL-JOB PostgreSQL 数据库脚本
-- 用于存储任务信息、执行器信息、调度日志等

-- ----------------------------
-- 1、存储Job每个任务的属性信息
-- ----------------------------
CREATE TABLE xxl_job_info (
  id SERIAL PRIMARY KEY,
  job_group INTEGER NOT NULL, -- 执行器主键ID
  job_desc VARCHAR(255) NOT NULL,
  job_cron VARCHAR(128) NOT NULL, -- CRON表达式
  job_class VARCHAR(255) DEFAULT NULL, -- GLUE模式的Java类名
  job_param VARCHAR(512) DEFAULT NULL, -- 执行器参数
  job_status SMALLINT NOT NULL DEFAULT 0, -- 任务状态：0-停止，1-运行
  author VARCHAR(64) DEFAULT NULL, -- 负责人
  alarm_email VARCHAR(255) DEFAULT NULL, -- 报警邮件
  executor_route_strategy VARCHAR(50) DEFAULT 'FIRST', -- 执行器路由策略
  executor_handler VARCHAR(255) DEFAULT NULL, -- 执行器任务handler
  executor_param VARCHAR(512) DEFAULT NULL, -- 执行器任务参数
  executor_block_strategy VARCHAR(50) DEFAULT 'SERIAL_EXECUTION', -- 阻塞处理策略
  executor_timeout INTEGER DEFAULT 0, -- 任务执行超时时间，单位秒
  executor_fail_retry_count INTEGER DEFAULT 0, -- 失败重试次数
  glue_type VARCHAR(50) NOT NULL, -- GLUE类型
  glue_source TEXT, -- GLUE源代码
  glue_remark VARCHAR(128) DEFAULT NULL, -- GLUE备注
  glue_updatetime TIMESTAMP DEFAULT NULL, -- GLUE更新时间
  child_jobid VARCHAR(255) DEFAULT NULL, -- 子任务ID，多个逗号分隔
  trigger_status SMALLINT NOT NULL DEFAULT 0, -- 调度状态：0-停止，1-运行
  trigger_last_time BIGINT NOT NULL DEFAULT 0, -- 上次调度时间
  trigger_next_time BIGINT NOT NULL DEFAULT 0, -- 下次调度时间
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  delete_flag SMALLINT NOT NULL DEFAULT 0 -- 是否删除：0-正常，1-删除
);

-- 创建索引
CREATE INDEX idx_job_group ON xxl_job_info(job_group);
CREATE INDEX idx_trigger_status ON xxl_job_info(trigger_status);
CREATE INDEX idx_next_time ON xxl_job_info(trigger_next_time);

-- 添加注释
COMMENT ON TABLE xxl_job_info IS '任务信息表';
COMMENT ON COLUMN xxl_job_info.id IS '主键ID';
COMMENT ON COLUMN xxl_job_info.job_group IS '执行器主键ID';
COMMENT ON COLUMN xxl_job_info.job_desc IS '任务描述';
COMMENT ON COLUMN xxl_job_info.job_cron IS 'CRON表达式';
COMMENT ON COLUMN xxl_job_info.job_class IS 'GLUE模式的Java类名';
COMMENT ON COLUMN xxl_job_info.job_param IS '执行器参数';
COMMENT ON COLUMN xxl_job_info.job_status IS '任务状态：0-停止，1-运行';
COMMENT ON COLUMN xxl_job_info.author IS '负责人';
COMMENT ON COLUMN xxl_job_info.alarm_email IS '报警邮件';
COMMENT ON COLUMN xxl_job_info.executor_route_strategy IS '执行器路由策略';
COMMENT ON COLUMN xxl_job_info.executor_handler IS '执行器任务handler';
COMMENT ON COLUMN xxl_job_info.executor_param IS '执行器任务参数';
COMMENT ON COLUMN xxl_job_info.executor_block_strategy IS '阻塞处理策略';
COMMENT ON COLUMN xxl_job_info.executor_timeout IS '任务执行超时时间，单位秒';
COMMENT ON COLUMN xxl_job_info.executor_fail_retry_count IS '失败重试次数';
COMMENT ON COLUMN xxl_job_info.glue_type IS 'GLUE类型';
COMMENT ON COLUMN xxl_job_info.glue_source IS 'GLUE源代码';
COMMENT ON COLUMN xxl_job_info.glue_remark IS 'GLUE备注';
COMMENT ON COLUMN xxl_job_info.glue_updatetime IS 'GLUE更新时间';
COMMENT ON COLUMN xxl_job_info.child_jobid IS '子任务ID，多个逗号分隔';
COMMENT ON COLUMN xxl_job_info.trigger_status IS '调度状态：0-停止，1-运行';
COMMENT ON COLUMN xxl_job_info.trigger_last_time IS '上次调度时间';
COMMENT ON COLUMN xxl_job_info.trigger_next_time IS '下次调度时间';
COMMENT ON COLUMN xxl_job_info.create_time IS '创建时间';
COMMENT ON COLUMN xxl_job_info.update_time IS '更新时间';
COMMENT ON COLUMN xxl_job_info.delete_flag IS '是否删除：0-正常，1-删除';

-- ----------------------------
-- 2、存储Job执行器信息
-- ----------------------------
CREATE TABLE xxl_job_jobgroup (
  id SERIAL PRIMARY KEY,
  app_name VARCHAR(64) NOT NULL, -- 执行器AppName
  title VARCHAR(12) NOT NULL, -- 执行器名称
  address_type SMALLINT NOT NULL DEFAULT 0, -- 执行器地址类型：0=自动注册、1=手动录入
  address_list VARCHAR(512) DEFAULT NULL, -- 执行器地址列表，多地址逗号分隔
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_app_name ON xxl_job_jobgroup(app_name);

-- 添加注释
COMMENT ON TABLE xxl_job_jobgroup IS '执行器信息表';
COMMENT ON COLUMN xxl_job_jobgroup.id IS '主键ID';
COMMENT ON COLUMN xxl_job_jobgroup.app_name IS '执行器AppName';
COMMENT ON COLUMN xxl_job_jobgroup.title IS '执行器名称';
COMMENT ON COLUMN xxl_job_jobgroup.address_type IS '执行器地址类型：0=自动注册、1=手动录入';
COMMENT ON COLUMN xxl_job_jobgroup.address_list IS '执行器地址列表，多地址逗号分隔';
COMMENT ON COLUMN xxl_job_jobgroup.update_time IS '更新时间';

-- ----------------------------
-- 3、存储Job执行日志
-- ----------------------------
CREATE TABLE xxl_job_log (
  id BIGSERIAL PRIMARY KEY,
  job_group INTEGER NOT NULL, -- 执行器主键ID
  job_id INTEGER NOT NULL, -- 任务主键ID
  executor_address VARCHAR(255) DEFAULT NULL, -- 执行器地址，本次执行的地址
  executor_handler VARCHAR(255) DEFAULT NULL, -- 执行器任务handler
  executor_param TEXT, -- 执行器任务参数
  executor_sharding_param VARCHAR(20) DEFAULT NULL, -- 执行器任务分片参数，格式如 1/2
  executor_fail_retry_count INTEGER DEFAULT 0, -- 失败重试次数
  trigger_time TIMESTAMP DEFAULT NULL, -- 调度-时间
  trigger_code INTEGER DEFAULT NULL, -- 调度-结果
  trigger_msg TEXT, -- 调度-日志
  handle_time TIMESTAMP DEFAULT NULL, -- 执行-时间
  handle_code INTEGER DEFAULT NULL, -- 执行-状态
  handle_msg TEXT, -- 执行-日志
  alarm_status SMALLINT NOT NULL DEFAULT 0 -- 告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败
);

-- 创建索引
CREATE INDEX idx_job_id ON xxl_job_log(job_id);
CREATE INDEX idx_trigger_time ON xxl_job_log(trigger_time);
CREATE INDEX idx_handle_code ON xxl_job_log(handle_code);

-- 添加注释
COMMENT ON TABLE xxl_job_log IS '任务日志表';
COMMENT ON COLUMN xxl_job_log.id IS '主键ID';
COMMENT ON COLUMN xxl_job_log.job_group IS '执行器主键ID';
COMMENT ON COLUMN xxl_job_log.job_id IS '任务主键ID';
COMMENT ON COLUMN xxl_job_log.executor_address IS '执行器地址，本次执行的地址';
COMMENT ON COLUMN xxl_job_log.executor_handler IS '执行器任务handler';
COMMENT ON COLUMN xxl_job_log.executor_param IS '执行器任务参数';
COMMENT ON COLUMN xxl_job_log.executor_sharding_param IS '执行器任务分片参数，格式如 1/2';
COMMENT ON COLUMN xxl_job_log.executor_fail_retry_count IS '失败重试次数';
COMMENT ON COLUMN xxl_job_log.trigger_time IS '调度-时间';
COMMENT ON COLUMN xxl_job_log.trigger_code IS '调度-结果';
COMMENT ON COLUMN xxl_job_log.trigger_msg IS '调度-日志';
COMMENT ON COLUMN xxl_job_log.handle_time IS '执行-时间';
COMMENT ON COLUMN xxl_job_log.handle_code IS '执行-状态';
COMMENT ON COLUMN xxl_job_log.handle_msg IS '执行-日志';
COMMENT ON COLUMN xxl_job_log.alarm_status IS '告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败';

-- ----------------------------
-- 4、存储Job日志报告
-- ----------------------------
CREATE TABLE xxl_job_log_report (
  id SERIAL PRIMARY KEY,
  trigger_day TIMESTAMP DEFAULT NULL, -- 调度-时间
  running_count INTEGER NOT NULL DEFAULT 0, -- 运行中-日志数量
  suc_count INTEGER NOT NULL DEFAULT 0, -- 执行成功-日志数量
  fail_count INTEGER NOT NULL DEFAULT 0, -- 执行失败-日志数量
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_trigger_day UNIQUE (trigger_day)
);

-- 添加注释
COMMENT ON TABLE xxl_job_log_report IS '任务日志报告表';
COMMENT ON COLUMN xxl_job_log_report.id IS '主键ID';
COMMENT ON COLUMN xxl_job_log_report.trigger_day IS '调度-时间';
COMMENT ON COLUMN xxl_job_log_report.running_count IS '运行中-日志数量';
COMMENT ON COLUMN xxl_job_log_report.suc_count IS '执行成功-日志数量';
COMMENT ON COLUMN xxl_job_log_report.fail_count IS '执行失败-日志数量';
COMMENT ON COLUMN xxl_job_log_report.update_time IS '更新时间';

-- ----------------------------
-- 5、存储Job注册信息
-- ----------------------------
CREATE TABLE xxl_job_registry (
  id SERIAL PRIMARY KEY,
  registry_group VARCHAR(50) NOT NULL,
  registry_key VARCHAR(255) NOT NULL,
  registry_value VARCHAR(255) NOT NULL,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_group_key ON xxl_job_registry(registry_group, registry_key);

-- 添加注释
COMMENT ON TABLE xxl_job_registry IS '执行器注册表';
COMMENT ON COLUMN xxl_job_registry.id IS '主键ID';
COMMENT ON COLUMN xxl_job_registry.registry_group IS '注册分组';
COMMENT ON COLUMN xxl_job_registry.registry_key IS '注册key';
COMMENT ON COLUMN xxl_job_registry.registry_value IS '注册value';
COMMENT ON COLUMN xxl_job_registry.update_time IS '更新时间';

-- ----------------------------
-- 6、存储Job锁信息
-- ----------------------------
CREATE TABLE xxl_job_lock (
  lock_name VARCHAR(50) PRIMARY KEY
);

-- 添加注释
COMMENT ON TABLE xxl_job_lock IS '任务锁表';
COMMENT ON COLUMN xxl_job_lock.lock_name IS '锁名称';

-- ----------------------------
-- 创建自动更新时间函数
-- ----------------------------
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- ----------------------------
-- 为需要的表创建触发器
-- ----------------------------
CREATE TRIGGER update_xxl_job_info_modtime 
BEFORE UPDATE ON xxl_job_info 
FOR EACH ROW EXECUTE FUNCTION update_modified_column();

CREATE TRIGGER update_xxl_job_jobgroup_modtime 
BEFORE UPDATE ON xxl_job_jobgroup 
FOR EACH ROW EXECUTE FUNCTION update_modified_column();

CREATE TRIGGER update_xxl_job_registry_modtime 
BEFORE UPDATE ON xxl_job_registry 
FOR EACH ROW EXECUTE FUNCTION update_modified_column();

CREATE TRIGGER update_xxl_job_log_report_modtime 
BEFORE UPDATE ON xxl_job_log_report 
FOR EACH ROW EXECUTE FUNCTION update_modified_column();

-- ----------------------------
-- 初始化数据
-- ----------------------------
INSERT INTO xxl_job_jobgroup(app_name, title, address_type, address_list, update_time) 
VALUES ('cloud-job-executor', 'CMS任务执行器', 0, NULL, CURRENT_TIMESTAMP);

INSERT INTO xxl_job_lock(lock_name) 
VALUES ('schedule_lock');

-- ----------------------------
-- 创建序列（如果需要手动控制ID）
-- ----------------------------
CREATE SEQUENCE IF NOT EXISTS xxl_job_info_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS xxl_job_jobgroup_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS xxl_job_log_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS xxl_job_log_report_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS xxl_job_registry_id_seq START 1;

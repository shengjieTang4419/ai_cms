-- 职业信息表
CREATE TABLE IF NOT EXISTS occupations (
    id BIGSERIAL PRIMARY KEY,
    code INTEGER NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    tags TEXT[],
    status INTEGER DEFAULT 1,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_occupation_code ON occupations(code);
CREATE INDEX IF NOT EXISTS idx_occupation_status ON occupations(status);

-- 插入初始数据（如果不存在）
INSERT INTO occupations (code, name, tags, status, sort_order) 
SELECT 1, '程序员', ARRAY['编程', '技术', '软件开发'], 1, 1
WHERE NOT EXISTS (SELECT 1 FROM occupations WHERE code = 1);

INSERT INTO occupations (code, name, tags, status, sort_order) 
SELECT 2, '设计师', ARRAY['设计', '创意', '视觉'], 1, 2
WHERE NOT EXISTS (SELECT 1 FROM occupations WHERE code = 2);

INSERT INTO occupations (code, name, tags, status, sort_order) 
SELECT 3, '教师', ARRAY['教育', '学习', '知识分享'], 1, 3
WHERE NOT EXISTS (SELECT 1 FROM occupations WHERE code = 3);

INSERT INTO occupations (code, name, tags, status, sort_order) 
SELECT 4, '医生', ARRAY['健康', '医学', '养生'], 1, 4
WHERE NOT EXISTS (SELECT 1 FROM occupations WHERE code = 4);

INSERT INTO occupations (code, name, tags, status, sort_order) 
SELECT 5, '销售', ARRAY['商务', '沟通', '市场'], 1, 5
WHERE NOT EXISTS (SELECT 1 FROM occupations WHERE code = 5);

INSERT INTO occupations (code, name, tags, status, sort_order) 
SELECT 6, '金融', ARRAY['金融', '投资', '理财'], 1, 6
WHERE NOT EXISTS (SELECT 1 FROM occupations WHERE code = 6);

INSERT INTO occupations (code, name, tags, status, sort_order) 
SELECT 7, '媒体', ARRAY['媒体', '传播', '内容'], 1, 7
WHERE NOT EXISTS (SELECT 1 FROM occupations WHERE code = 7);

INSERT INTO occupations (code, name, tags, status, sort_order) 
SELECT 8, '法律', ARRAY['法律', '合规', '咨询'], 1, 8
WHERE NOT EXISTS (SELECT 1 FROM occupations WHERE code = 8);

-- 添加表和字段注释
COMMENT ON TABLE occupations IS '职业信息表';
COMMENT ON COLUMN occupations.id IS '主键ID';
COMMENT ON COLUMN occupations.code IS '职业代码';
COMMENT ON COLUMN occupations.name IS '职业名称';
COMMENT ON COLUMN occupations.tags IS '职业相关标签（数组）';
COMMENT ON COLUMN occupations.status IS '状态：1-启用，0-禁用';
COMMENT ON COLUMN occupations.sort_order IS '排序顺序';
COMMENT ON COLUMN occupations.created_at IS '创建时间';
COMMENT ON COLUMN occupations.updated_at IS '更新时间';


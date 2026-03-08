-- 刻板印象模板表
-- @author shengjie.tang
-- @date 2026/2/5

CREATE TABLE IF NOT EXISTS stereotype_templates (
    id BIGSERIAL PRIMARY KEY,
    template_key VARCHAR(100) NOT NULL UNIQUE,
    template_name VARCHAR(100),
    gender VARCHAR(10),
    age_range VARCHAR(20),
    occupation_code INTEGER,
    preset_tags JSONB,
    tag_weights JSONB,
    description VARCHAR(500),
    source VARCHAR(20) DEFAULT 'SYSTEM',
    status INTEGER DEFAULT 1,
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_template_key ON stereotype_templates(template_key);
CREATE INDEX IF NOT EXISTS idx_gender_age_occupation ON stereotype_templates(gender, age_range, occupation_code);
CREATE INDEX IF NOT EXISTS idx_status_priority ON stereotype_templates(status, priority DESC);

-- 注释
COMMENT ON TABLE stereotype_templates IS '刻板印象模板表';
COMMENT ON COLUMN stereotype_templates.id IS '主键ID';
COMMENT ON COLUMN stereotype_templates.template_key IS '模板唯一标识，格式: {gender}_{ageRange}_{occupationCode}';
COMMENT ON COLUMN stereotype_templates.template_name IS '模板名称（人类可读）';
COMMENT ON COLUMN stereotype_templates.gender IS '性别：MALE/FEMALE/OTHER';
COMMENT ON COLUMN stereotype_templates.age_range IS '年龄段：CHILD/TEENAGER/YOUNG_ADULT/ADULT/MIDDLE_AGED/SENIOR/ELDERLY';
COMMENT ON COLUMN stereotype_templates.occupation_code IS '职业代码（关联occupations表）';
COMMENT ON COLUMN stereotype_templates.preset_tags IS '预设标签列表（JSON数组）';
COMMENT ON COLUMN stereotype_templates.tag_weights IS '标签权重映射（JSON对象）';
COMMENT ON COLUMN stereotype_templates.description IS '模板描述';
COMMENT ON COLUMN stereotype_templates.source IS '模板来源：SYSTEM-系统内置, AI-AI生成';
COMMENT ON COLUMN stereotype_templates.status IS '状态：1-启用，0-禁用';
COMMENT ON COLUMN stereotype_templates.priority IS '匹配优先级（数值越大优先级越高）';
COMMENT ON COLUMN stereotype_templates.created_at IS '创建时间';
COMMENT ON COLUMN stereotype_templates.updated_at IS '更新时间';

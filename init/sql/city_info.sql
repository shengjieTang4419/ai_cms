-- 城市信息表
-- 用于存储城市编码和行政区划信息，支持天气查询的城市编码匹配
-- PostgreSQL版本

CREATE TABLE IF NOT EXISTS city_info (
    id BIGSERIAL PRIMARY KEY, -- PostgreSQL使用BIGSERIAL自动递增
    name VARCHAR(100) NOT NULL,
    province VARCHAR(50),
    city VARCHAR(50),
    district VARCHAR(50),
    amap_city_code VARCHAR(10),
    admin_code VARCHAR(20),
    level INTEGER DEFAULT 1,
    parent_code VARCHAR(20),
    full_name VARCHAR(200),
    longitude DECIMAL(10, 7),
    latitude DECIMAL(10, 7),
    enabled SMALLINT DEFAULT 1,
    remark VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引（PostgreSQL语法）
CREATE INDEX IF NOT EXISTS idx_city_name ON city_info (name);
CREATE INDEX IF NOT EXISTS idx_city_province ON city_info (province);
CREATE INDEX IF NOT EXISTS idx_city_city ON city_info (city);
CREATE INDEX IF NOT EXISTS idx_city_district ON city_info (district);
CREATE INDEX IF NOT EXISTS idx_city_amap_code ON city_info (amap_city_code);
CREATE INDEX IF NOT EXISTS idx_city_admin_code ON city_info (admin_code);
CREATE INDEX IF NOT EXISTS idx_city_full_name ON city_info (full_name);
CREATE INDEX IF NOT EXISTS idx_city_enabled ON city_info (enabled);
CREATE INDEX IF NOT EXISTS idx_city_level ON city_info (level);

-- 为update_time添加触发器，确保更新时自动更新时间戳
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_city_info_updated_at BEFORE UPDATE ON city_info
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 插入一些常用城市数据作为示例
INSERT INTO city_info (name, province, city, district, amap_city_code, admin_code, level, full_name, enabled) VALUES
-- 直辖市
('北京市', '北京市', '北京市', NULL, '110000', '110000', 2, '北京市', 1),
('上海市', '上海市', '上海市', NULL, '310000', '310000', 2, '上海市', 1),
('天津市', '天津市', '天津市', NULL, '120000', '120000', 2, '天津市', 1),
('重庆市', '重庆市', '重庆市', NULL, '500000', '500000', 2, '重庆市', 1),

-- 主要城市（省会城市）
('广州市', '广东省', '广州市', NULL, '440100', '440100', 3, '广东省广州市', 1),
('深圳市', '广东省', '深圳市', NULL, '440300', '440300', 3, '广东省深圳市', 1),
('杭州市', '浙江省', '杭州市', NULL, '330100', '330100', 3, '浙江省杭州市', 1),
('南京市', '江苏省', '南京市', NULL, '320100', '320100', 3, '江苏省南京市', 1),
('苏州市', '江苏省', '苏州市', NULL, '320500', '320500', 3, '江苏省苏州市', 1),
('成都市', '四川省', '成都市', NULL, '510100', '510100', 3, '四川省成都市', 1),
('武汉市', '湖北省', '武汉市', NULL, '420100', '420100', 3, '湖北省武汉市', 1),
('西安市', '陕西省', '西安市', NULL, '610100', '610100', 3, '陕西省西安市', 1),
('郑州市', '河南省', '郑州市', NULL, '410100', '410100', 3, '河南省郑州市', 1),
('长沙市', '湖南省', '长沙市', NULL, '430100', '430100', 3, '湖南省长沙市', 1),

-- 区县级别城市（直辖市辖区）
('浦东新区', '上海市', '上海市', '浦东新区', '310115', '310115', 4, '上海市浦东新区', 1),
('朝阳区', '北京市', '北京市', '朝阳区', '110105', '110105', 4, '北京市朝阳区', 1),
('海淀区', '北京市', '北京市', '海淀区', '110108', '110108', 4, '北京市海淀区', 1),
('黄浦区', '上海市', '上海市', '黄浦区', '310101', '310101', 4, '上海市黄浦区', 1),
('徐汇区', '上海市', '上海市', '徐汇区', '310104', '310104', 4, '上海市徐汇区', 1),

-- 其他重要城市
('青岛市', '山东省', '青岛市', NULL, '370200', '370200', 3, '山东省青岛市', 1),
('大连市', '辽宁省', '大连市', NULL, '210200', '210200', 3, '辽宁省大连市', 1),
('宁波市', '浙江省', '宁波市', NULL, '330200', '330200', 3, '浙江省宁波市', 1),
('厦门市', '福建省', '厦门市', NULL, '350200', '350200', 3, '福建省厦门市', 1),
('东莞市', '广东省', '东莞市', NULL, '441900', '441900', 3, '广东省东莞市', 1),
('佛山市', '广东省', '佛山市', NULL, '440600', '440600', 3, '广东省佛山市', 1);
-- 初始化角色和用户数据
-- 创建默认角色
INSERT INTO sys_role (role_code, role_name, description, sort, status, created_at, updated_at) VALUES
('ADMIN', '管理员', '系统管理员，拥有所有权限', 1, 1, NOW(), NOW()),
('USER', '普通用户', '普通用户，基础权限', 2, 1, NOW(), NOW()),
('MANAGER', '经理', '部门经理，管理权限', 3, 1, NOW(), NOW())
ON CONFLICT (role_code) DO NOTHING;

-- 创建默认权限
INSERT INTO sys_permission (permission_code, permission_name, description, resource, method, sort, status, created_at, updated_at) VALUES
('USER_MANAGE', '用户管理', '用户增删改查权限', '/api/users/**', 'ALL', 1, 1, NOW(), NOW()),
('ROLE_MANAGE', '角色管理', '角色增删改查权限', '/api/roles/**', 'ALL', 2, 1, NOW(), NOW()),
('PERMISSION_MANAGE', '权限管理', '权限增删改查权限', '/api/permissions/**', 'ALL', 3, 1, NOW(), NOW()),
('ADMIN_TOKEN', '管理员Token', '获取其他用户Token的权限', '/api/auth/admin/**', 'POST', 4, 1, NOW(), NOW()),
('CHAT_SEND', '发送消息', '发送聊天消息权限', '/api/chat/**', 'POST', 5, 1, NOW(), NOW()),
('CHAT_READ', '查看消息', '查看聊天消息权限', '/api/chat/**', 'GET', 6, 1, NOW(), NOW())
ON CONFLICT (permission_code) DO NOTHING;

-- 为ADMIN角色分配所有权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'ADMIN'
ON CONFLICT DO NOTHING;

-- 为USER角色分配基础权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'USER' 
AND p.permission_code IN ('CHAT_SEND', 'CHAT_READ')
ON CONFLICT DO NOTHING;

-- 为MANAGER角色分配管理权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'MANAGER' 
AND p.permission_code IN ('USER_MANAGE', 'CHAT_SEND', 'CHAT_READ')
ON CONFLICT DO NOTHING;

-- 创建默认管理员用户（密码：admin123）
INSERT INTO users (username, email, password, status, created_at) VALUES
('admin', 'admin@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 1, NOW())
ON CONFLICT (username) DO NOTHING;

-- 为管理员用户分配ADMIN角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM users u, sys_role r
WHERE u.username = 'admin' AND r.role_code = 'ADMIN'
ON CONFLICT DO NOTHING;

-- 创建测试用户（密码：test123）
INSERT INTO users (username, email, password, status, created_at) VALUES
('testuser', 'test@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 1, NOW())
ON CONFLICT (username) DO NOTHING;

-- 为测试用户分配USER角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM users u, sys_role r
WHERE u.username = 'testuser' AND r.role_code = 'USER'
ON CONFLICT DO NOTHING;

-- V2__Add_default_admin.sql
-- 添加默认管理员账户（密码: Admin123）

INSERT INTO `user` (`email`, `password`, `name`, `role`, `created_at`, `updated_at`)
VALUES (
    'admin@volcano.blog',
    '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW',
    '系统管理员',
    'ADMIN',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE 
    `password` = '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW';

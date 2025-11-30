-- V2__Add_default_admin.sql
-- 添加默认管理员账户（密码: Admin123）

INSERT INTO `user` (`email`, `password`, `name`, `role`, `created_at`, `updated_at`)
VALUES (
    'admin@volcano.blog',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6jdWG',
    '系统管理员',
    'ADMIN',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE `id` = `id`;

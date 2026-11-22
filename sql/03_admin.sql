-- 管理员演示账号（BCrypt 加密密码 admin123）
-- 密码哈希由 Spring 端 BCryptPasswordEncoder 生成后手工插入
USE anime_helper;

INSERT INTO `user` (`username`, `password`, `nickname`, `status`, `behavior_count`)
SELECT 'boss', '$2a$10$7EqJodLNqYMcdmbuIWXDAtU0QvV7jqHFB3xQKTFGHyXJgmI/aCAK', '管理员', 1, 99
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username`='boss');

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM `user` u, `role` r
WHERE u.username='boss' AND r.role_code='ADMIN'
  AND NOT EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_id=u.id AND ur.role_id=r.id);

-- beatu_user 增加密码哈希列（登录/自动注册）
-- 在部署 auth 接口前执行一次

ALTER TABLE `beatu_user`
  ADD COLUMN `passwordHash` VARCHAR(255) NULL COMMENT 'bcrypt 密码哈希' AFTER `userName`;

-- 审核队列增强：支持用户提交番剧补充建议（原案用户反馈闭环）
-- 用户提交想上架的番剧名称与跳转链接 → 管理员审核（可修改信息与链接）→ 上架
USE anime_helper;

ALTER TABLE `review_queue`
  ADD COLUMN `source_type`   VARCHAR(20)  NOT NULL DEFAULT 'IMPORT' COMMENT '来源：IMPORT数据导入 / USER用户提交',
  ADD COLUMN `submitter_id`  BIGINT       DEFAULT NULL COMMENT '提交用户ID（USER来源）',
  ADD COLUMN `submitter_note` VARCHAR(500) DEFAULT NULL COMMENT '用户备注/意见',
  ADD COLUMN `platform_name` VARCHAR(50)  DEFAULT NULL COMMENT '用户填写的平台名称',
  ADD COLUMN `platform_url`  VARCHAR(500) DEFAULT NULL COMMENT '用户填写的跳转链接';

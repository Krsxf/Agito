-- ============================================================
-- 《基于多元数据的个性化追番推荐与补番助手》数据库建库建表脚本
-- 数据库：anime_helper  字符集：utf8mb4（兼容日文原名与别名）
-- 说明：Bangumi 已停止服务，跨平台统一主键调整为站内自增 ID，
--       外部来源以 source / source_id 字段记录，供多源对齐使用。
-- ============================================================

CREATE DATABASE IF NOT EXISTS anime_helper DEFAULT CHARACTER SET utf8mb4 COLLATE=utf8mb4_unicode_ci;
USE anime_helper;

-- ------------------------------------------------------------
-- 1. 用户与权限（RBAC：用户-角色，权限由后端注解 + 路由拦截控制）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`        VARCHAR(50)  NOT NULL COMMENT '用户名',
  `password`        VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密存储）',
  `nickname`        VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
  `phone`           VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `email`           VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `avatar`          VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
  `behavior_count`  INT          NOT NULL DEFAULT 0 COMMENT '累计行为条数（评分/收藏/追番/跳转），用于冷启动判定',
  `last_login_time` DATETIME     DEFAULT NULL COMMENT '最近登录时间',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `role` (
  `id`        BIGINT      NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码：ADMIN / USER',
  `remark`    VARCHAR(200) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS `user_role` (
  `id`      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';

-- ------------------------------------------------------------
-- 2. 兴趣标签（注册时 12 类多选，冷启动初始画像）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `interest_tag` (
  `id`       BIGINT      NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名称',
  `tag_code` VARCHAR(50) NOT NULL COMMENT '标签编码',
  `sort_no`  INT         NOT NULL DEFAULT 0 COMMENT '排序号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_code` (`tag_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='兴趣标签字典表';

CREATE TABLE IF NOT EXISTS `user_interest` (
  `id`      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `tag_id`  BIGINT NOT NULL COMMENT '兴趣标签ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tag` (`user_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-兴趣标签关联表';

-- ------------------------------------------------------------
-- 3. 番剧主库（统一番剧信息库）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `anime` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '番剧ID（站内统一主键）',
  `title`             VARCHAR(200) NOT NULL COMMENT '主标题',
  `alias`             VARCHAR(500) DEFAULT NULL COMMENT '别名（日文原名/英文/简称，逗号分隔，供检索与对齐）',
  `cover_url`         VARCHAR(500) DEFAULT NULL COMMENT '封面URL',
  `year`              INT          DEFAULT NULL COMMENT '首播年份',
  `premiere_date`     DATE         DEFAULT NULL COMMENT '首播日期',
  `region`            VARCHAR(20)  DEFAULT NULL COMMENT '地区：日本/中国/欧美',
  `air_status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '播出状态：1连载中 2已完结 3待上线',
  `episode_count`     INT          DEFAULT NULL COMMENT '总集数',
  `duration`          VARCHAR(20)  DEFAULT NULL COMMENT '单集时长（如 24min）',
  `studio`            VARCHAR(100) DEFAULT NULL COMMENT '制作公司',
  `director`          VARCHAR(100) DEFAULT NULL COMMENT '导演',
  `voice_actor`       VARCHAR(500) DEFAULT NULL COMMENT '主要声优（逗号分隔）',
  `weekday`           TINYINT      DEFAULT NULL COMMENT '每周更新日：1周一 ~ 7周日（周视图日历用）',
  `description`       TEXT         DEFAULT NULL COMMENT '剧情简介',
  `source`            VARCHAR(30)  NOT NULL DEFAULT 'LOCAL_IMPORT' COMMENT '数据来源：LOCAL_IMPORT本地导入 / BILI / DOUBAN / MAL',
  `source_id`         VARCHAR(100) DEFAULT NULL COMMENT '来源侧条目ID（多源对齐用，Bangumi已停服不作主键）',
  `play_count`        BIGINT       NOT NULL DEFAULT 0 COMMENT '播放量（热度因子0.35）',
  `follow_count`      INT          NOT NULL DEFAULT 0 COMMENT '追番人数（热度因子0.30）',
  `rating_count`      INT          NOT NULL DEFAULT 0 COMMENT '评分人数（热度因子0.20）',
  `social_discussion` INT          NOT NULL DEFAULT 0 COMMENT '社交讨论量（热度因子0.15）',
  `total_score`       DECIMAL(3,1) DEFAULT NULL COMMENT '综合加权评分（多源10分制按置信度加权）',
  `heat_score`        DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '综合热度分（四因子归一化加权，榜单与热门补偿用）',
  `state`             TINYINT      NOT NULL DEFAULT 0 COMMENT '数据状态：0待审核 1已上线 2已下架',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_title` (`title`),
  KEY `idx_year` (`year`),
  KEY `idx_region` (`region`),
  KEY `idx_air_status` (`air_status`),
  KEY `idx_state` (`state`),
  KEY `idx_heat` (`heat_score`),
  KEY `idx_weekday` (`weekday`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='番剧主表';

CREATE TABLE IF NOT EXISTS `anime_tag` (
  `id`       BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `anime_id` BIGINT      NOT NULL COMMENT '番剧ID',
  `tag`      VARCHAR(50) NOT NULL COMMENT '类型标签（热血/恋爱/奇幻等）',
  PRIMARY KEY (`id`),
  KEY `idx_anime_id` (`anime_id`),
  KEY `idx_tag` (`tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='番剧-类型标签关联表';

-- ------------------------------------------------------------
-- 4. 多源评分与观看平台
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `anime_score` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `anime_id`     BIGINT       NOT NULL COMMENT '番剧ID',
  `source_name`  VARCHAR(30)  NOT NULL COMMENT '评分来源：DOUBAN豆瓣 / BILI / MAL / SITE站内',
  `score`        DECIMAL(3,1) NOT NULL COMMENT '归一化后评分（10分制）',
  `rating_count` INT          NOT NULL DEFAULT 0 COMMENT '该来源评分人数',
  `confidence`   DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT '数据源置信度权重（加权融合用）',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间',
  PRIMARY KEY (`id`),
  KEY `idx_anime_id` (`anime_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='多源评分表（10分制归一化）';

CREATE TABLE IF NOT EXISTS `anime_platform` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `anime_id`      BIGINT       NOT NULL COMMENT '番剧ID',
  `platform_name` VARCHAR(50)  NOT NULL COMMENT '平台名称：哔哩哔哩/腾讯视频/爱奇艺/优酷',
  `url`           VARCHAR(500) NOT NULL COMMENT '观看页面URL（仅外链，不承载视频）',
  `weight`        INT          NOT NULL DEFAULT 0 COMMENT '展示权重（越大越靠前，主推平台高亮）',
  `is_paid`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否付费：0免费 1付费',
  `region_limit`  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否限地区：0否 1是',
  `link_status`   TINYINT      NOT NULL DEFAULT 1 COMMENT '链接状态：1有效 0失效',
  `check_time`    DATETIME     DEFAULT NULL COMMENT '最近链接校验时间',
  PRIMARY KEY (`id`),
  KEY `idx_anime_id` (`anime_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='番剧观看平台外链表（合规边界：只跳转不承载）';

-- ------------------------------------------------------------
-- 5. 用户行为（评分 / 收藏 / 追番 / 补番 / 跳转日志）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `rating` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
  `anime_id`    BIGINT       NOT NULL COMMENT '番剧ID',
  `score`       DECIMAL(3,1) NOT NULL COMMENT '评分（1~10）',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_anime` (`user_id`, `anime_id`),
  KEY `idx_anime_id` (`anime_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户评分表';

CREATE TABLE IF NOT EXISTS `favorite` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
  `anime_id`    BIGINT   NOT NULL COMMENT '番剧ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_anime` (`user_id`, `anime_id`),
  KEY `idx_anime_id` (`anime_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

CREATE TABLE IF NOT EXISTS `follow` (
  `id`                BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`           BIGINT   NOT NULL COMMENT '用户ID',
  `anime_id`          BIGINT   NOT NULL COMMENT '番剧ID',
  `current_episode`   INT      NOT NULL DEFAULT 0 COMMENT '当前看到集数',
  `total_episode`     INT      DEFAULT NULL COMMENT '总集数（快照）',
  `progress`          INT      NOT NULL DEFAULT 0 COMMENT '观看进度百分比',
  `follow_status`     TINYINT  NOT NULL DEFAULT 1 COMMENT '追番状态：1追番中 2已看完 3弃番',
  `next_episode_time` DATETIME DEFAULT NULL COMMENT '下集预计更新时间',
  `create_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '追番时间',
  `update_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_anime` (`user_id`, `anime_id`),
  KEY `idx_anime_id` (`anime_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='追番表';

CREATE TABLE IF NOT EXISTS `catchup` (
  `id`             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`        BIGINT   NOT NULL COMMENT '用户ID',
  `anime_id`       BIGINT   NOT NULL COMMENT '番剧ID',
  `priority`       INT      NOT NULL DEFAULT 0 COMMENT '补番优先级（拖拽排序）',
  `catchup_status` TINYINT  NOT NULL DEFAULT 1 COMMENT '状态：1待看 2已完成 3已弃番（弃番不计入进度）',
  `complete_time`  DATETIME DEFAULT NULL COMMENT '完成时间',
  `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `update_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_anime` (`user_id`, `anime_id`),
  KEY `idx_anime_id` (`anime_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='补番清单表';

CREATE TABLE IF NOT EXISTS `jump_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`       BIGINT       DEFAULT NULL COMMENT '用户ID（游客为空）',
  `anime_id`      BIGINT       NOT NULL COMMENT '番剧ID',
  `platform_name` VARCHAR(50)  NOT NULL COMMENT '平台名称',
  `url`           VARCHAR(500) NOT NULL COMMENT '跳转URL',
  `is_valid`      TINYINT      NOT NULL DEFAULT 1 COMMENT '跳转时链接是否有效',
  `jump_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '跳转时间',
  PRIMARY KEY (`id`),
  KEY `idx_anime_id` (`anime_id`),
  KEY `idx_jump_time` (`jump_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网页跳转日志表（渠道热度与转化统计）';

-- ------------------------------------------------------------
-- 6. 数据管道（合规导入模式下的采集任务与人工审核）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `import_task` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `task_name`     VARCHAR(100) NOT NULL COMMENT '任务名称',
  `source`        VARCHAR(30)  NOT NULL DEFAULT 'LOCAL_EXCEL' COMMENT '数据来源：LOCAL_EXCEL / LOCAL_JSON / MANUAL',
  `file_name`     VARCHAR(255) DEFAULT NULL COMMENT '导入文件名',
  `total_count`   INT          NOT NULL DEFAULT 0 COMMENT '总记录数',
  `success_count` INT          NOT NULL DEFAULT 0 COMMENT '成功条数',
  `fail_count`    INT          NOT NULL DEFAULT 0 COMMENT '失败条数',
  `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0执行中 1成功 2部分成功 3失败',
  `detail`        TEXT         DEFAULT NULL COMMENT '执行日志摘要',
  `operator`      VARCHAR(50)  DEFAULT NULL COMMENT '操作人',
  `start_time`    DATETIME     DEFAULT NULL COMMENT '开始时间',
  `end_time`      DATETIME     DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据导入任务表（管道-过滤器：导入→清洗→归一化→审核→入库）';

CREATE TABLE IF NOT EXISTS `review_queue` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id`       BIGINT       DEFAULT NULL COMMENT '关联导入任务ID',
  `anime_payload` TEXT         DEFAULT NULL COMMENT '待审核番剧数据（JSON）',
  `reason`        VARCHAR(200) DEFAULT NULL COMMENT '进入队列原因（如无法自动匹配ID、字段缺失）',
  `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1通过 2驳回',
  `reviewer`      VARCHAR(50)  DEFAULT NULL COMMENT '审核人',
  `review_time`   DATETIME     DEFAULT NULL COMMENT '审核时间',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入队时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人工审核队列表';

-- ------------------------------------------------------------
-- 7. 推荐与榜单配置、结果
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `rec_config` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键（单行配置 id=1）',
  `w_profile`      DECIMAL(3,2) NOT NULL DEFAULT 0.40 COMMENT '画像匹配权重',
  `w_cf`           DECIMAL(3,2) NOT NULL DEFAULT 0.30 COMMENT '协同过滤权重',
  `w_hot`          DECIMAL(3,2) NOT NULL DEFAULT 0.20 COMMENT '热门补偿权重',
  `w_random`       DECIMAL(3,2) NOT NULL DEFAULT 0.10 COMMENT '随机探索权重',
  `home_size`      INT          NOT NULL DEFAULT 10 COMMENT '每栏目推荐数量',
  `cache_minutes`  INT          NOT NULL DEFAULT 30 COMMENT '推荐缓存周期（分钟）',
  `cold_threshold` INT          NOT NULL DEFAULT 20 COMMENT '冷启动阈值（行为条数）',
  `last_recalc`    DATETIME     DEFAULT NULL COMMENT '上次全量重算时间',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推荐参数配置表（后台四因子权重）';

CREATE TABLE IF NOT EXISTS `hot_rank` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `anime_id`    BIGINT       NOT NULL COMMENT '番剧ID',
  `rank_type`   TINYINT      NOT NULL COMMENT '榜单类型：1综合热度 2新番榜 3经典高分榜',
  `period`      TINYINT      NOT NULL DEFAULT 3 COMMENT '时间范围：1周榜 2月榜 3总榜',
  `rank_no`     INT          NOT NULL COMMENT '名次',
  `score`       DECIMAL(8,2) NOT NULL COMMENT '榜单得分',
  `stat_date`   DATE         NOT NULL COMMENT '统计日期',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  PRIMARY KEY (`id`),
  KEY `idx_type_period_no` (`rank_type`, `period`, `rank_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排行榜结果表（每小时缓存更新）';

-- ------------------------------------------------------------
-- 8. 提醒与关键操作日志
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `notification` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
  `anime_id`    BIGINT       DEFAULT NULL COMMENT '关联番剧ID',
  `content`     VARCHAR(500) NOT NULL COMMENT '提醒内容（如《XX》第8集已更新）',
  `is_read`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='更新提醒表（侧边菜单红点）';

CREATE TABLE IF NOT EXISTS `sys_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       DEFAULT NULL COMMENT '操作用户ID',
  `log_type`    VARCHAR(20)  NOT NULL COMMENT '日志类型：LOGIN登录 / OPER操作 / JUMP跳转 / IMPORT导入',
  `action`      VARCHAR(100) DEFAULT NULL COMMENT '操作动作',
  `detail`      VARCHAR(500) DEFAULT NULL COMMENT '详情',
  `ip`          VARCHAR(50)  DEFAULT NULL COMMENT '来源IP',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_type_time` (`log_type`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统关键操作日志表（文件日志由Logback落盘留存180天）';

CREATE TABLE IF NOT EXISTS app_user (
 user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
 user_name VARCHAR(40) NOT NULL UNIQUE,
 nick_name VARCHAR(80) NOT NULL,
 password_hash VARCHAR(100) NOT NULL,
 role VARCHAR(16) NOT NULL DEFAULT 'USER',
 enabled BOOLEAN NOT NULL DEFAULT TRUE,
 avatar VARCHAR(500),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS app_session (
 token_hash VARCHAR(64) PRIMARY KEY,
 user_id BIGINT NOT NULL,
 expires_at TIMESTAMP NOT NULL,
 INDEX app_session_idx_expires_at(expires_at),
 FOREIGN KEY(user_id) REFERENCES app_user(user_id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS `tl_ai_task`  (
  `task_id` bigint NOT NULL AUTO_INCREMENT,
  `worldline_id` bigint NULL DEFAULT NULL,
  `task_type` varchar(50) NOT NULL,
  `related_id` bigint NULL DEFAULT NULL,
  `input_content` longtext NULL,
  `prompt` longtext NULL,
  `result_content` longtext NULL,
  `task_status` char(1) NULL DEFAULT '0',
  `error_msg` text NULL,
  `create_by` varchar(64) NULL DEFAULT '',
  `create_time` datetime NULL DEFAULT NULL,
  `finish_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`task_id`),
  INDEX `tl_ai_task_idx_worldline_type`(`worldline_id` ASC, `task_type` ASC)
);

CREATE TABLE IF NOT EXISTS `tl_chapter`  (
  `chapter_id` bigint NOT NULL AUTO_INCREMENT,
  `worldline_id` bigint NOT NULL,
  `stage_id` bigint NULL DEFAULT NULL,
  `chapter_title` varchar(200) NOT NULL,
  `chapter_order` int NULL DEFAULT 0,
  `related_event_ids` varchar(1000) NULL DEFAULT NULL,
  `writing_style` varchar(100) NULL DEFAULT NULL,
  `content` longtext NULL,
  `ai_prompt` longtext NULL,
  `status` char(1) NULL DEFAULT '0',
  `cover_illustration_id` bigint NULL DEFAULT NULL,
  `create_by` varchar(64) NULL DEFAULT '',
  `create_time` datetime NULL DEFAULT NULL,
  `update_by` varchar(64) NULL DEFAULT '',
  `update_time` datetime NULL DEFAULT NULL,
  `remark` varchar(500) NULL DEFAULT NULL,
  PRIMARY KEY (`chapter_id`),
  INDEX `tl_chapter_idx_worldline_stage`(`worldline_id` ASC, `stage_id` ASC)
);

CREATE TABLE IF NOT EXISTS `tl_character`  (
  `character_id` bigint NOT NULL AUTO_INCREMENT,
  `worldline_id` bigint NOT NULL,
  `character_name` varchar(100) NOT NULL,
  `country` varchar(100) NULL DEFAULT NULL,
  `faction` varchar(100) NULL DEFAULT NULL,
  `identity_name` varchar(100) NULL DEFAULT NULL,
  `character_type` varchar(50) NULL DEFAULT NULL,
  `personality` varchar(500) NULL DEFAULT NULL,
  `biography` text NULL,
  `ai_generated` char(1) NULL DEFAULT '0',
  `portrait_url` varchar(500) NULL DEFAULT NULL,
  `create_by` varchar(64) NULL DEFAULT '',
  `create_time` datetime NULL DEFAULT NULL,
  `update_by` varchar(64) NULL DEFAULT '',
  `update_time` datetime NULL DEFAULT NULL,
  `remark` varchar(500) NULL DEFAULT NULL,
  PRIMARY KEY (`character_id`),
  INDEX `tl_character_idx_worldline_id`(`worldline_id` ASC)
);

CREATE TABLE IF NOT EXISTS `tl_event`  (
  `event_id` bigint NOT NULL AUTO_INCREMENT,
  `worldline_id` bigint NOT NULL,
  `screenshot_id` bigint NULL DEFAULT NULL,
  `stage_id` bigint NULL DEFAULT NULL,
  `event_title` varchar(200) NOT NULL,
  `event_date` varchar(50) NULL DEFAULT NULL,
  `event_year` int NULL DEFAULT NULL,
  `event_type` varchar(50) NULL DEFAULT NULL,
  `country` varchar(100) NULL DEFAULT NULL,
  `related_forces` varchar(500) NULL DEFAULT NULL,
  `importance_level` char(1) NULL DEFAULT '1',
  `divergence_flag` char(1) NULL DEFAULT '0',
  `summary` text NULL,
  `ai_description` text NULL,
  `impact_analysis` text NULL,
  `novel_potential` char(1) NULL DEFAULT '0',
  `create_by` varchar(64) NULL DEFAULT '',
  `create_time` datetime NULL DEFAULT NULL,
  `update_by` varchar(64) NULL DEFAULT '',
  `update_time` datetime NULL DEFAULT NULL,
  `remark` varchar(500) NULL DEFAULT NULL,
  PRIMARY KEY (`event_id`),
  INDEX `tl_event_idx_worldline_year`(`worldline_id` ASC, `event_year` ASC),
  INDEX `tl_event_idx_stage_id`(`stage_id` ASC)
);

CREATE TABLE IF NOT EXISTS `tl_event_relation`  (
  `relation_id` bigint NOT NULL AUTO_INCREMENT,
  `worldline_id` bigint NOT NULL,
  `source_event_id` bigint NOT NULL,
  `target_event_id` bigint NOT NULL,
  `relation_type` varchar(50) NULL DEFAULT 'cause',
  `relation_desc` text NULL,
  `ai_generated` char(1) NULL DEFAULT '0',
  `create_by` varchar(64) NULL DEFAULT '',
  `create_time` datetime NULL DEFAULT NULL,
  `update_by` varchar(64) NULL DEFAULT '',
  `update_time` datetime NULL DEFAULT NULL,
  `remark` varchar(500) NULL DEFAULT NULL,
  PRIMARY KEY (`relation_id`),
  INDEX `tl_event_relation_idx_worldline_id`(`worldline_id` ASC),
  INDEX `tl_event_relation_idx_source_target`(`source_event_id` ASC, `target_event_id` ASC)
);

CREATE TABLE IF NOT EXISTS `tl_illustration`  (
  `illustration_id` bigint NOT NULL AUTO_INCREMENT,
  `worldline_id` bigint NOT NULL,
  `stage_id` bigint NULL DEFAULT NULL,
  `event_id` bigint NULL DEFAULT NULL,
  `chapter_id` bigint NULL DEFAULT NULL,
  `character_id` bigint NULL DEFAULT NULL,
  `illustration_title` varchar(200) NULL DEFAULT NULL,
  `illustration_type` varchar(50) NULL DEFAULT NULL,
  `image_url` varchar(500) NULL DEFAULT NULL,
  `prompt` longtext NULL,
  `negative_prompt` text NULL,
  `style_type` varchar(50) NULL DEFAULT NULL,
  `generate_status` char(1) NULL DEFAULT '0',
  `ai_raw_result` longtext NULL,
  `scene_description` text NULL,
  `create_by` varchar(64) NULL DEFAULT '',
  `create_time` datetime NULL DEFAULT NULL,
  `update_by` varchar(64) NULL DEFAULT '',
  `update_time` datetime NULL DEFAULT NULL,
  `remark` varchar(500) NULL DEFAULT NULL,
  PRIMARY KEY (`illustration_id`),
  INDEX `tl_illustration_idx_worldline_id`(`worldline_id` ASC),
  INDEX `tl_illustration_idx_chapter_id`(`chapter_id` ASC)
);

CREATE TABLE IF NOT EXISTS `tl_nation_state`  (
  `state_id` bigint NOT NULL AUTO_INCREMENT,
  `worldline_id` bigint NOT NULL,
  `country_name` varchar(100) NOT NULL,
  `record_year` int NULL DEFAULT NULL,
  `government` varchar(100) NULL DEFAULT NULL,
  `economy_status` varchar(500) NULL DEFAULT NULL,
  `military_status` varchar(500) NULL DEFAULT NULL,
  `diplomacy_status` varchar(500) NULL DEFAULT NULL,
  `social_conflict` text NULL,
  `ai_summary` text NULL,
  `create_by` varchar(64) NULL DEFAULT '',
  `create_time` datetime NULL DEFAULT NULL,
  `update_by` varchar(64) NULL DEFAULT '',
  `update_time` datetime NULL DEFAULT NULL,
  `remark` varchar(500) NULL DEFAULT NULL,
  PRIMARY KEY (`state_id`),
  INDEX `tl_nation_state_idx_worldline_country_year`(`worldline_id` ASC, `country_name` ASC, `record_year` ASC)
);

CREATE TABLE IF NOT EXISTS `tl_public_action`  (
  `action_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL DEFAULT NULL,
  `target_type` varchar(50) NOT NULL,
  `target_id` bigint NOT NULL,
  `action_type` varchar(50) NOT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`action_id`),
  INDEX `tl_public_action_idx_target`(`target_type` ASC, `target_id` ASC),
  INDEX `tl_public_action_idx_user_action`(`user_id` ASC, `action_type` ASC)
);

CREATE TABLE IF NOT EXISTS `tl_screenshot`  (
  `screenshot_id` bigint NOT NULL AUTO_INCREMENT,
  `worldline_id` bigint NOT NULL,
  `image_url` varchar(500) NOT NULL,
  `original_name` varchar(255) NULL DEFAULT NULL,
  `ai_status` char(1) NULL DEFAULT '0',
  `ai_raw_result` longtext NULL,
  `draft_event_json` longtext NULL,
  `create_by` varchar(64) NULL DEFAULT '',
  `create_time` datetime NULL DEFAULT NULL,
  `update_by` varchar(64) NULL DEFAULT '',
  `update_time` datetime NULL DEFAULT NULL,
  `remark` varchar(500) NULL DEFAULT NULL,
  PRIMARY KEY (`screenshot_id`),
  INDEX `tl_screenshot_idx_worldline_id`(`worldline_id` ASC)
);

CREATE TABLE IF NOT EXISTS `tl_stage`  (
  `stage_id` bigint NOT NULL AUTO_INCREMENT,
  `worldline_id` bigint NOT NULL,
  `stage_name` varchar(100) NOT NULL,
  `start_year` int NULL DEFAULT NULL,
  `end_year` int NULL DEFAULT NULL,
  `stage_theme` varchar(100) NULL DEFAULT NULL,
  `stage_summary` text NULL,
  `novel_status` char(1) NULL DEFAULT '0',
  `image_status` char(1) NULL DEFAULT '0',
  `create_by` varchar(64) NULL DEFAULT '',
  `create_time` datetime NULL DEFAULT NULL,
  `update_by` varchar(64) NULL DEFAULT '',
  `update_time` datetime NULL DEFAULT NULL,
  `remark` varchar(500) NULL DEFAULT NULL,
  PRIMARY KEY (`stage_id`),
  INDEX `tl_stage_idx_worldline_id`(`worldline_id` ASC)
);

CREATE TABLE IF NOT EXISTS `tl_user_profile`  (
  `profile_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `nickname` varchar(100) NULL DEFAULT NULL,
  `avatar_url` varchar(500) NULL DEFAULT NULL,
  `bio` varchar(500) NULL DEFAULT NULL,
  `creator_title` varchar(100) NULL DEFAULT NULL,
  `homepage_cover_url` varchar(500) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`profile_id`),
  UNIQUE INDEX `tl_user_profile_uk_user_id`(`user_id` ASC)
);

CREATE TABLE IF NOT EXISTS `tl_worldline`  (
  `worldline_id` bigint NOT NULL AUTO_INCREMENT,
  `worldline_name` varchar(100) NOT NULL,
  `game_name` varchar(100) NULL DEFAULT '维多利亚3',
  `main_country` varchar(100) NULL DEFAULT NULL,
  `start_year` int NULL DEFAULT NULL,
  `current_year` int NULL DEFAULT NULL,
  `narrative_style` varchar(100) NULL DEFAULT NULL,
  `description` text NULL,
  `cover_url` varchar(500) NULL DEFAULT NULL,
  `visibility` char(1) NULL DEFAULT '0',
  `divergence_score` int NULL DEFAULT 0,
  `create_by` varchar(64) NULL DEFAULT '',
  `create_time` datetime NULL DEFAULT NULL,
  `update_by` varchar(64) NULL DEFAULT '',
  `update_time` datetime NULL DEFAULT NULL,
  `remark` varchar(500) NULL DEFAULT NULL,
  PRIMARY KEY (`worldline_id`)
);

CREATE TABLE IF NOT EXISTS app_media (
 url VARCHAR(500) PRIMARY KEY,
 owner_user_id BIGINT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(owner_user_id) REFERENCES app_user(user_id)
);

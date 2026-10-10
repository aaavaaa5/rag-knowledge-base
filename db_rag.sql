-- ======================================================
-- 数据库：kbs（Knowledge Base System）
-- 字符集：UTF-8
-- ======================================================

CREATE DATABASE IF NOT EXISTS `kbs`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `kbs`;

-- ------------------------------------------------------
-- 1. 知识库表 (kb)
-- ------------------------------------------------------
DROP TABLE IF EXISTS `kb`;

CREATE TABLE `kb` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `name` VARCHAR(100) NOT NULL COMMENT '知识库名称（唯一）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kb_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';


-- ------------------------------------------------------
-- 2. 文档表 (document)
-- ------------------------------------------------------
DROP TABLE IF EXISTS `document`;

CREATE TABLE `document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `kb_id` BIGINT NOT NULL COMMENT '所属知识库 ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '本地存储路径',
    `chunk_size` INT NOT NULL DEFAULT 500 COMMENT '切分长度（Token 数）',
    `overlap` INT NOT NULL DEFAULT 50 COMMENT '重叠长度（Token 数）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/processing/completed/failed',
    `size` BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_doc_kb_id` (`kb_id`),
    KEY `idx_doc_status` (`status`),
    KEY `idx_doc_kb_status` (`kb_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';


-- ------------------------------------------------------
-- 3. 会话表 (conversation)
-- ------------------------------------------------------
DROP TABLE IF EXISTS `conversation`;

CREATE TABLE `conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `kb_id` BIGINT NOT NULL COMMENT '所属知识库 ID',
    `title` VARCHAR(100) DEFAULT NULL COMMENT '会话标题',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_conv_kb_id` (`kb_id`),
    KEY `idx_conv_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';


-- ------------------------------------------------------
-- 4. 消息表 (message)
-- ------------------------------------------------------
DROP TABLE IF EXISTS `message`;

CREATE TABLE `message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `conversation_id` BIGINT NOT NULL COMMENT '所属会话 ID',
    `role` VARCHAR(10) NOT NULL COMMENT '角色：user / assistant',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `references` JSON DEFAULT NULL COMMENT '引用来源：格式 [{"doc_id": 1, "file_name": "xxx.txt", "snippet": "..."}]',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_msg_conversation_id` (`conversation_id`),
    KEY `idx_msg_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

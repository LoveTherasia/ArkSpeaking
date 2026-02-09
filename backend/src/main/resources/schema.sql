-- 修正后的建表语句（解决关键字+语法兼容问题）
CREATE TABLE IF NOT EXISTS `user` ( -- 表名加反引号，规避关键字冲突
                                      id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                                      nickname VARCHAR(50) DEFAULT '博士' COMMENT '用户昵称',
                                      avatar VARCHAR(255) DEFAULT 'http://localhost:8080/avatar/default.jpg' COMMENT '头像URL',
                                      signature VARCHAR(200) DEFAULT '与角色的日常' COMMENT '个性签名',
                                      PRIMARY KEY (id)
) COMMENT='用户信息表'; -- 删除DEFAULT CHARSET=utf8mb4

-- 插入默认用户（保留，反引号可选）
INSERT IGNORE INTO `user` (nickname, avatar, signature)
VALUES ('博士', 'http://localhost:8080/avatar/default.jpg', '与角色的日常');
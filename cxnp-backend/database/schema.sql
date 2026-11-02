CREATE TABLE `party_member` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '党员ID',
                                `name` varchar(50) NOT NULL COMMENT '党员姓名',
                                `gender` varchar(10) DEFAULT NULL COMMENT '性别',
                                `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
                                `join_date` date DEFAULT NULL COMMENT '入党日期',
                                `party_branch` varchar(100) DEFAULT NULL COMMENT '所属党支部',
                                `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='党员信息表'


CREATE TABLE IF NOT EXISTS sys_user (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                        username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    );





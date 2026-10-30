USE cxnp;

SHOW TABLES;

CREATE TABLE IF NOT EXISTS connection_test (
                                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                               name VARCHAR(100)
    );

INSERT INTO connection_test (name)
VALUES ('Mac 连接腾讯云成功');

SELECT * FROM connection_test;
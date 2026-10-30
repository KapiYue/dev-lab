package com.qingmou.cxnp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void testConnection() throws Exception {

        try (Connection connection = dataSource.getConnection()) {

            System.out.println("数据库连接成功！");
            System.out.println("数据库：" + connection.getCatalog());

            assertTrue(connection.isValid(5));
        }
    }
}
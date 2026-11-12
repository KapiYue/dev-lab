package com.qingmou.cxnp.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Fails closed when a test context is accidentally connected to the development schema. */
@Component
@Profile("test")
public class TestDatabaseGuard {

    private static final String TEST_DATABASE = "cxnp_test";

    private final JdbcTemplate jdbcTemplate;

    public TestDatabaseGuard(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void verifyTestDatabase() {
        String databaseName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        if (!TEST_DATABASE.equals(databaseName)) {
            throw new IllegalStateException(
                    "测试只能连接 " + TEST_DATABASE + "；当前数据库为 " + databaseName);
        }
    }
}

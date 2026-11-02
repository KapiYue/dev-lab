package com.qingmou.cxnp;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashTest {

    @Test
    void generatePasswordHash() {

        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        String hash = encoder.encode("123456");

        System.out.println("BCrypt Hash: " + hash);

        System.out.println(
                "Password matches: " +
                        encoder.matches("123456", hash)
        );
    }
}
/**
 * 运行Unit测试之后
 *
Mac os终端
 curl --noproxy '*' -i -X POST http://127.0.0.1:8080/api/auth/login \
 -H "Content-Type: application/json" \
 -d '{"username":"admin","password":"123456"}'
 */
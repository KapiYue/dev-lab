
package com.qingmou.cxnp;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptGenerator {

    public static void main(String[] args) {

        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        String password = "123456";

        String hash = encoder.encode(password);

        System.out.println("BCrypt = " + hash);

        System.out.println(
                "验证结果 = " + encoder.matches(password, hash)
        );
    }
}

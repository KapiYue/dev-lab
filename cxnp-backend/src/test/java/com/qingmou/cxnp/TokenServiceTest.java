
package com.qingmou.cxnp;

import com.qingmou.cxnp.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TokenServiceTest {

    @Autowired
    private TokenService tokenService;

    // 测试1：有效 Token
    @Test
    void testValidToken() {
        String token = tokenService.createToken(1L);

        assertNotNull(token);
        assertFalse(token.isBlank());

        Long userId = tokenService.verifyToken(token);

        assertEquals(1L, userId);
    }

    // 测试2：无效 Token
    @Test
    void testInvalidToken() {
        Long userId = tokenService.verifyToken("invalid-token");

        assertNull(userId);
    }

    // 测试3：空 Token
    @Test
    void testEmptyToken() {
        assertNull(tokenService.verifyToken(""));
        assertNull(tokenService.verifyToken(null));
    }

    // 测试4：生成不同的 Token
    @Test
    void testUniqueToken() {
        String token1 = tokenService.createToken(1L);
        String token2 = tokenService.createToken(1L);

        assertNotEquals(token1, token2);
    }
}


package com.qingmou.cxnp;

import com.qingmou.cxnp.service.UserRoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class UserRoleServiceTest {

    @Autowired
    private UserRoleService userRoleService;

    @Test
    void shouldRejectDuplicateUserRoleBinding() {

        // 数据库中已经存在 user_id=2, role_id=1
        Long userId = 2L;
        Long roleId = 1L;

        // 验证 Service 层拒绝重复绑定
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userRoleService.bindRole(userId, roleId)
        );

        // 验证返回的业务异常信息
        assertEquals(
                "用户已绑定该角色",
                exception.getMessage()
        );

        System.out.println("Service 层重复绑定校验通过");
    }
}

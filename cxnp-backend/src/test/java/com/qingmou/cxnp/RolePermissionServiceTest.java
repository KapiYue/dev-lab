
package com.qingmou.cxnp;

import com.qingmou.cxnp.service.RolePermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class RolePermissionServiceTest {

    @Autowired
    private RolePermissionService rolePermissionService;

    @Test
    void shouldRejectDuplicateRolePermissionBinding() {

        // 数据库已存在 role_id=1, permission_id=1
        Long roleId = 1L;
        Long permissionId = 1L;

        // 验证 Service 拒绝重复绑定
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> rolePermissionService.bindPermission(
                        roleId, permissionId
                )
        );

        // 验证业务异常信息
        assertEquals(
                "角色已绑定该权限",
                exception.getMessage()
        );

        System.out.println("RolePermissionService 重复绑定校验通过");
    }
}

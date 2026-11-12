package com.qingmou.cxnp;

import com.qingmou.cxnp.common.Result;
import com.qingmou.cxnp.exception.DataScopeDeniedException;
import com.qingmou.cxnp.exception.GlobalExceptionHandler;
import com.qingmou.cxnp.exception.PermissionDeniedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void separatesFunctionalPermissionAndDataScopeErrors() {
        ResponseEntity<Result<Void>> functionalResponse = handler
                .handlePermissionDeniedException(
                        new PermissionDeniedException("无功能权限：party:member:list")
                );
        ResponseEntity<Result<Void>> scopeResponse = handler
                .handleDataScopeDeniedException(
                        new DataScopeDeniedException("当前用户没有有效数据范围")
                );

        assertError(functionalResponse, HttpStatus.FORBIDDEN, 403, "无功能权限");
        assertError(scopeResponse, HttpStatus.FORBIDDEN, 403, "无数据范围");
    }

    @Test
    void convertsServiceArgumentErrorsToBadRequestJson() {
        ResponseEntity<Result<Void>> response = handler
                .handleIllegalArgumentException(
                        new IllegalArgumentException("党员 ID 必须大于 0")
                );

        assertError(response, HttpStatus.BAD_REQUEST, 400, "党员 ID 必须大于 0");
    }

    private void assertError(
            ResponseEntity<Result<Void>> response,
            HttpStatus status,
            int code,
            String message
    ) {
        assertEquals(status, response.getStatusCode());
        Result<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals(code, body.getCode());
        assertEquals(message, body.getMessage());
        assertNull(body.getData());
    }
}

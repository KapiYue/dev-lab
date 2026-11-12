package com.qingmou.cxnp.exception;

import com.qingmou.cxnp.common.Result;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. URL 参数类型错误：HTTP 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Result.<Void>error(
                        400,
                        "ID 格式参数错误"
                ));
    }

    // 2. 请求参数校验失败：HTTP 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("请求参数校验失败");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Result.<Void>error(
                        400,
                        message
                ));
    }

    // 3. 登录认证异常：HTTP 401
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthenticationException(
            AuthenticationException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Result.<Void>error(
                        401,
                        ex.getMessage()
                ));
    }

    // 4. 功能权限不足：HTTP 403
    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<Result<Void>> handlePermissionDeniedException(
            PermissionDeniedException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Result.<Void>error(
                        403,
                        "无功能权限"
                ));
    }

    // 5. 功能权限已通过，但没有可访问的业务数据范围：HTTP 403
    @ExceptionHandler(DataScopeDeniedException.class)
    public ResponseEntity<Result<Void>> handleDataScopeDeniedException(
            DataScopeDeniedException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Result.<Void>error(
                        403,
                        "无数据范围"
                ));
    }

    // 6. Service 层的防御性参数校验：HTTP 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Result.<Void>error(
                        400,
                        ex.getMessage()
                ));
    }
}

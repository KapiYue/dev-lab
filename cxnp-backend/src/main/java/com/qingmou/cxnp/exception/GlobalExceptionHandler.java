package com.qingmou.cxnp.exception;

import com.qingmou.cxnp.common.Result;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;

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

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(
            ConstraintViolationException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Result.<Void>error(400, "用户 ID 必须大于 0"));
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

    // 3. 登录失败保护：HTTP 429
    @ExceptionHandler(LoginTemporarilyLockedException.class)
    public ResponseEntity<Result<Void>> handleLoginTemporarilyLockedException(
            LoginTemporarilyLockedException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Result.<Void>error(
                        429,
                        ex.getMessage()
                ));
    }

    // 4. 登录认证异常：HTTP 401
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

    // 5. 功能权限不足：HTTP 403
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

    // 6. 功能权限已通过，但没有可访问的业务数据范围：HTTP 403
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

    // 7. Service 层的防御性参数校验：HTTP 400
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

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Result<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Result.<Void>error(404, ex.getMessage()));
    }
}

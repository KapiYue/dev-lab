package com.qingmou.cxnp.exception;

import com.qingmou.cxnp.common.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException e){

            return ResponseEntity
                    .badRequest()
                    .body(Result.error(400, "ID 格式参数错误"));
        }
    }


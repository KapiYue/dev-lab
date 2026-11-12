package com.qingmou.cxnp.exception;

/** 当前用户具备功能权限，但没有可用业务数据范围。 */
public class DataScopeDeniedException extends RuntimeException {

    public DataScopeDeniedException(String message) {
        super(message);
    }
}

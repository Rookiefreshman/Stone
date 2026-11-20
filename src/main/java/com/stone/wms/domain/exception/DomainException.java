package com.stone.wms.domain.exception;

/**
 * 领域异常用于表达业务规则被破坏，避免领域层依赖接口层错误模型。
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}

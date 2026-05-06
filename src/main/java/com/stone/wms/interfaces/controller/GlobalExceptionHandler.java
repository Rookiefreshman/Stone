package com.stone.wms.interfaces.controller;

import com.stone.wms.domain.exception.DomainException;
import com.stone.wms.interfaces.response.ApiResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一异常处理把领域异常转换为接口层响应，避免异常模型跨层泄漏。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DomainException.class)
    public ApiResponse<Void> handleDomainException(DomainException exception) {
        return ApiResponse.failure(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException exception) {
        return ApiResponse.failure("请求参数不合法");
    }
}

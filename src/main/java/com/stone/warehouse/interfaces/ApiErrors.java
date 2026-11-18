package com.stone.warehouse.interfaces;

import com.stone.warehouse.domain.WarehouseProblem;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolationException;
import java.util.Map;

import static com.stone.warehouse.application.Views.fields;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(WarehouseProblem.class)
    ResponseEntity<Map<String, Object>> business(WarehouseProblem error) {
        HttpStatus status;
        switch (error.getCode()) {
            case "NOT_FOUND": status = HttpStatus.NOT_FOUND; break;
            case "INVALID_DOCUMENT":
            case "INVALID_MASTER":
            case "INVALID_WAVE":
            case "INVALID_RETURN":
            case "INVALID_QUANTITY":
            case "INVALID_LOCATION":
            case "INVALID_EXPIRY":
            case "INVALID_OBSERVATION":
            case "BATCH_REQUIRED":
            case "WRONG_WAREHOUSE":
                status = HttpStatus.BAD_REQUEST;
                break;
            default: status = HttpStatus.CONFLICT;
        }
        return error(status, error.getCode(), error.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<Map<String, Object>> duplicate() {
        return error(HttpStatus.CONFLICT, "DUPLICATE_KEY", "编码已存在或业务记录已提交");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException error) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_INPUT",
                "请校验字段 " + error.getBindingResult().getFieldError().getField());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class})
    ResponseEntity<Map<String, Object>> invalid() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "字段、类型或参数范围错误");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(fields("code", code, "message", message));
    }
}

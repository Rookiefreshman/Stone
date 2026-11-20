package com.stone.wms.domain.exception;

public final class DispatchException extends RuntimeException {
    public enum Code { NOT_FOUND, CONFLICT, QUEUE_FULL }

    private final Code code;

    public DispatchException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}

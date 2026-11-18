package com.stone.warehouse.domain;

public class WarehouseProblem extends RuntimeException {
    private final String code;

    public WarehouseProblem(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }

    public static void require(boolean condition, String code, String message) {
        if (!condition) {
            throw new WarehouseProblem(code, message);
        }
    }

    public static <T> T found(T value, String what) {
        require(value != null, "NOT_FOUND", what + "不存在");
        return value;
    }
}

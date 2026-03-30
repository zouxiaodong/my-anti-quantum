package com.quantum.mock.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private T data;
    private String msg;

    public static <T> Result<T> success(T data) {
        return new Result<>(0, data, "");
    }

    public static <T> Result<T> success(T data, String msg) {
        return new Result<>(0, data, msg);
    }

    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, null, msg);
    }
}

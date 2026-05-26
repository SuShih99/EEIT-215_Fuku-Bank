package com.javaeasybank.common.dto.response;

/**
 * 統一 API 回傳格式：success(data)、success(message, data)、fail(message)。
 */
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private String errorCode;
    private T data;

    private ApiResponse() {}

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> res = new ApiResponse<>();
        res.success = true;
        res.message = "操作成功";
        res.data = data;
        return res;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> res = new ApiResponse<>();
        res.success = true;
        res.message = message;
        res.data = data;
        return res;
    }

    public static <T> ApiResponse<T> fail(String message) {
        ApiResponse<T> res = new ApiResponse<>();
        res.success = false;
        res.message = message;
        res.data = null;
        return res;
    }

    public static <T> ApiResponse<T> fail(String errorCode, String message) {
        ApiResponse<T> res = new ApiResponse<>();
        res.success = false;
        res.errorCode = errorCode;
        res.message = message;
        res.data = null;
        return res;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getErrorCode() { return errorCode; }
    public T getData() { return data; }
}

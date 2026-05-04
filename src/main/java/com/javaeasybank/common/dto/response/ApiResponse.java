package com.javaeasybank.common.dto.response;

/**
 * 統一 API 回傳格式。
 *
 * 所有 API 的回傳都必須包成這個格式，
 * 確保前端可以用同一套邏輯處理所有回應。
 *
 * 規則：
 * - 成功 → ApiResponse.success(data)
 * - 失敗 → ApiResponse.fail("說明")
 * - 不要自己寫 Map 或其他格式回傳
 */
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private String errorCode;
    private T data;

    // 外部不能直接 new ApiResponse()，只能用下面的靜態方法建立
    private ApiResponse() {}

    /**
     * 成功回傳，附帶資料
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> res = new ApiResponse<>();
        res.success = true;
        res.message = "操作成功";
        res.data = data;
        return res;
    }
    // 法2:成功回傳，附帶訊息與資料
    public static <T> ApiResponse<T> success(String message, T data) {
    ApiResponse<T> res = new ApiResponse<>();
    res.success = true;
    res.message = message;
    res.data = data;
    return res;
    }

    /**
     * 失敗回傳，附帶錯誤訊息
     */
    public static <T> ApiResponse<T> fail(String message) {
        ApiResponse<T> res = new ApiResponse<>();
        res.success = false;
        res.message = message;
        res.data = null;
        return res;
    }

    /**
     * 失敗回傳，附帶錯誤碼與錯誤訊息
     */
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
package com.javaeasybank.common.exception;

import com.javaeasybank.common.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全域例外處理器。
 *
 * 任何 Controller 拋出的例外都會被這裡攔截，
 * 統一包成 ApiResponse 格式回傳給前端。
 *
 * 規則：
 * - 業務邏輯錯誤 → throw new BusinessException("說明")
 * - 不要自己 catch、不要自己寫回傳格式
 * - 這裡會自動處理
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 攔截業務邏輯錯誤（帳戶不存在、餘額不足等）
     * 回傳 HTTP 400
     */
    // 這個Annotation 代表全域例外處理器 以前JavaEE沒有 要針對每個Exception做處理
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(e.getMessage()));
    }

    /**
     * 攔截所有其他預期外的錯誤
     * 回傳 HTTP 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail("伺服器錯誤: " + e.getMessage()));
    }
}

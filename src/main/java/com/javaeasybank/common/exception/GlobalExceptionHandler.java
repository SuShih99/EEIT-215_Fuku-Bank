package com.javaeasybank.common.exception;

import com.javaeasybank.account.exception.AccountException;
import com.javaeasybank.common.dto.response.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

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
     * 攔截帳戶相關業務邏輯錯誤（帶 errorCode）
     * 回傳 HTTP 400
     */
    @ExceptionHandler(AccountException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountException(AccountException e) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(e.getErrorCode(), e.getMessage()));
    }

    /**
     * 攔截其他業務邏輯錯誤（帳戶不存在、餘額不足等）
     * 回傳 HTTP 400
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(e.getMessage()));
    }

    /**
     * 攔截參數驗證錯誤 (@Valid 失敗)
     * 回傳 HTTP 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(errorMessage));
    }

    /**
     * 攔截資料唯一鍵或完整性衝突，避免直接把 Hibernate / SQL 訊息丟給前端。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(resolveDataIntegrityMessage(e)));
    }

    /**

     * 攔截權限不足錯誤（角色無權限存取此 API）
     * 回傳 HTTP 403
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail("FORBIDDEN", "權限不足，您的角色無法執行此操作"));
    }

    /**

     * 攔截登入驗證錯誤（帳號密碼錯誤、帳號停用等）
     * 回傳 HTTP 401
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("帳號或密碼錯誤"));
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

    private String resolveDataIntegrityMessage(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage()
                : e.getMessage();
        String lowerMessage = message == null ? "" : message.toLowerCase();

        if (lowerMessage.contains("email")) {
            return "電子信箱已被使用";
        }
        if (lowerMessage.contains("phone")) {
            return "手機號碼已被使用";
        }
        if (lowerMessage.contains("username")) {
            return "使用者帳號已存在";
        }
        if (lowerMessage.contains("id_number")) {
            return "身分證字號已存在";
        }

        return "資料重複或格式不符，請確認帳號、信箱、手機與身分證字號是否已被使用";
    }
}

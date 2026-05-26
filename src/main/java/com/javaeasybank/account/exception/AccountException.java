package com.javaeasybank.account.exception;

import com.javaeasybank.common.exception.BusinessException;
import lombok.Getter;

/**
 * 帳戶相關業務邏輯異常類。
 * 用於處理帳戶操作中可能發生的特定錯誤，例如帳戶不存在、餘額不足等。
 */
@Getter
public class AccountException extends BusinessException {
    private final String errorCode;

    /**
     * 構造一個新的 AccountException。
     *
     * @param errorCode 錯誤碼，用於標識特定錯誤類型。
     * @param message   錯誤訊息，提供錯誤的詳細描述。
     */
    public AccountException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

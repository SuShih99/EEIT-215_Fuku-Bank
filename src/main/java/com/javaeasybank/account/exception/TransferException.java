package com.javaeasybank.account.exception;

import com.javaeasybank.common.exception.BusinessException;
import lombok.Getter;

/**
 * 轉帳相關業務邏輯異常類。
 * 用於處理轉帳操作中可能發生的特定錯誤，例如餘額不足、帳戶不存在、幣別不符等。
 */
@Getter
public class TransferException extends BusinessException {
    private final String errorCode;

    /**
     * 構造一個新的 TransferException。
     *
     * @param errorCode 錯誤碼，用於標識特定錯誤類型。
     * @param message   錯誤訊息，提供錯誤的詳細描述。
     */
    public TransferException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

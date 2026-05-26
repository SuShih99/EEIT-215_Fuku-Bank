package com.javaeasybank.common.exception;


/**
 * 以前各自 try-catch 處理錯誤，現在改成：
 * 遇到業務錯誤直接 throw new BusinessException("說明")，
 * 透過繼承 RuntimeException，讓 GlobalExceptionHandler
 * 可以用 polymorphism 統一攔截 BusinessException 型別，
 * 不需要在每個地方各自處理。
 * */
public class BusinessException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
    public BusinessException(String message) {
        // 把訊息存進 RuntimeException的 detailMessage欄位，之後e.getMessage()就可以取得
        super(message);
    }
}

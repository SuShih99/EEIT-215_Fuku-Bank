package com.javaeasybank.account.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RiskCallbackRequest {

    /**
     * 審核結果：APPROVED / REJECTED
     * 對應風控 CallbackRequest.newStatus
     */
    private String newStatus;

    /**
     * 來源模組識別
     * 風控打回來時帶 "RISK"
     */
    private String callerModule;

    /**
     * 備註原因
     */
    private String note;
}
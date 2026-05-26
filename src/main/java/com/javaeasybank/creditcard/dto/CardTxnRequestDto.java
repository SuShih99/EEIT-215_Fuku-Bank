package com.javaeasybank.creditcard.dto;

import java.math.BigDecimal;

import com.javaeasybank.creditcard.enums.TransactionChannel;
import com.javaeasybank.creditcard.enums.TxnType;

import lombok.Data;

@Data
public class CardTxnRequestDto {

	private BigDecimal txnAmount;
    private TxnType txnType;
    private String description;
    private Integer cardId;
    private Integer merchantId;
    private String customerId; // 從 JWT 解析出來的 customerId
    private TransactionChannel channel;// CARD 或 LINE_PAY
    private String externalTxnId; // 來自第三方支付的交易 ID，例如 Line Pay 的 transactionId
}

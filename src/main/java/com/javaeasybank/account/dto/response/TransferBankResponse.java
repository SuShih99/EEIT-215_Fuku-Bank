package com.javaeasybank.account.dto.response;

import com.javaeasybank.account.enums.TransferBank;
import lombok.Data;

@Data
public class TransferBankResponse {

    private String code;
    private String name;
    private String label;

    public static TransferBankResponse fromEnum(TransferBank bank) {
        TransferBankResponse response = new TransferBankResponse();
        response.setCode(bank.getCode());
        response.setName(bank.getDisplayName());
        response.setLabel(bank.getLabel());
        return response;
    }
}

package com.javaeasybank.account.dto.request;

import com.javaeasybank.account.enums.AccountPurpose;
import com.javaeasybank.account.enums.AccountType;
import com.javaeasybank.account.enums.Currency;
import com.javaeasybank.account.enums.FundSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 開戶申請 Request DTO
 * 圖片欄位以 MultipartFile 在 Controller 層處理，這裡只放文字欄位。
 */
@Getter
@Setter
public class AccountApplicationRequest {

    // 帳戶
    @NotNull(message = "帳戶類型不可為空")
    private AccountType accountType;

    private Currency currency;  // 外幣帳戶才需指定

    // KYC
    @NotBlank(message = "姓名不可為空")
    private String customerName;

    @NotBlank(message = "身分證字號不可為空")
    private String idNumber;

    @NotNull(message = "出生日期不可為空")
    private LocalDate birthday;

    @NotBlank(message = "性別不可為空")
    private String gender;

    @NotBlank(message = "電子信箱不可為空")
    private String email;

    @NotBlank(message = "通訊地址不可為空")
    private String address;

    @NotBlank(message = "國籍不可為空")
    private String nationality;

    @NotBlank(message = "手機號碼不可為空")
    private String phone;

    @NotBlank(message = "戶籍地址不可為空")
    private String registeredAddress;

    @NotBlank(message = "現居地址不可為空")
    private String currentAddress;

    // 職業
    private String occupation;
    private String employer;
    private Integer annualIncome;
    private Integer estimatedMonthlyTx;

    // 目的 & 資金來源
    private AccountPurpose accountPurpose;
    private FundSource fundSource;

    // 法遵
    private String taxResidency;
    private Boolean isPep = false;
}

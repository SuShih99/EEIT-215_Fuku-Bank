package com.javaeasybank.account.service;

import com.itextpdf.kernel.pdf.EncryptionConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;
import com.javaeasybank.account.entity.Account;
import com.javaeasybank.account.enums.AccountStatus;
import com.javaeasybank.account.enums.AccountType;
import com.javaeasybank.account.enums.Currency;
import com.javaeasybank.account.repository.AccountRepository;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.customer.entity.CustomerProfile;
import com.javaeasybank.customer.repository.CustomerProfileRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PassbookPdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy / MM / dd");
    private static final String PDF_FONT_FAMILY = "JavaBankCjk";

    private final AccountRepository accountRepository;
    private final CustomerProfileRepository customerProfileRepository;

    public byte[] generateEncryptedPassbookPdf(String customerId, String accountNumber) {
        Account account = accountRepository.findById(accountNumber)
                .orElseThrow(() -> new BusinessException("帳戶不存在"));
        if (!customerId.equals(account.getCustomerId())) {
            throw new BusinessException("帳戶不存在或不屬於您");
        }

        CustomerProfile profile = customerProfileRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("查無客戶資料"));

        try {
            byte[] plainPdf = renderHtmlToPdf(buildHtml(account, profile));
            return encryptPdf(plainPdf, profile);
        } catch (Exception e) {
            throw new BusinessException("電子存摺 PDF 產生失敗");
        }
    }

    private byte[] renderHtmlToPdf(String html) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        registerCjkFont(builder);
        builder.withHtmlContent(html, null);
        builder.toStream(output);
        builder.run();
        return output.toByteArray();
    }

    private byte[] encryptPdf(byte[] plainPdf, CustomerProfile profile) throws Exception {
        ByteArrayOutputStream encryptedOutput = new ByteArrayOutputStream();
        
        String idNumber = profile.getIdNumber();
        String password = idNumber.length() >= 4 ? idNumber.substring(idNumber.length() - 4) : idNumber;
        byte[] ownerPassword = password.getBytes(StandardCharsets.UTF_8);
        WriterProperties properties = new WriterProperties()
                .setStandardEncryption(
                        new byte[0],
                        ownerPassword,
                        EncryptionConstants.ALLOW_PRINTING | EncryptionConstants.ALLOW_COPY,
                        EncryptionConstants.ENCRYPTION_AES_256
                );

        try (PdfDocument pdfDocument = new PdfDocument(
                new PdfReader(new ByteArrayInputStream(plainPdf)),
                new PdfWriter(encryptedOutput, properties))) {
            pdfDocument.getDocumentInfo().setTitle("福庫銀行電子存摺");
        }
        return encryptedOutput.toByteArray();
    }

    private String buildHtml(Account account, CustomerProfile profile) {
        String accountNumber = safe(formatAccountNumber(account.getAccountNumber()));
        String ownerName = safe(profile.getName());
        String currency = currencyLabel(account.getCurrency());
        String accountType = accountTypeLabel(account.getAccountType(), account.getCurrency());
        String status = statusLabel(account.getStatus());
        String createdAt = account.getCreatedAt() == null ? "-" : account.getCreatedAt().format(DATE_FORMATTER);
        String logo = logoDataUri();

        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8" />
                  <style>
                    @page { size: A4 landscape; margin: 14mm; }
                    * { box-sizing: border-box; }
                    body {
                      margin: 0;
                      color: #2f3330;
                      font-family: "%s", sans-serif;
                      background: #fbf8f1;
                    }
                    .page {
                      width: 269mm;
                      height: 166mm;
                      border: 1px solid #d7cfc3;
                      border-radius: 12px;
                      background: #fffaf1;
                      position: relative;
                      overflow: hidden;
                    }
                    .page:before {
                      content: "";
                      position: absolute;
                      right: 32mm;
                      top: 25mm;
                      width: 54mm;
                      height: 54mm;
                      border: 9mm solid #e1e1dc;
                      border-left-color: transparent;
                      border-bottom-color: transparent;
                      border-radius: 50%%;
                    }
                    .page:after {
                      content: "";
                      position: absolute;
                      right: 0;
                      bottom: 0;
                      width: 96mm;
                      height: 40mm;
                      background: #eef1eb;
                      border-top-left-radius: 120mm 56mm;
                    }
                    .brand-logo {
                      position: absolute;
                      z-index: 1;
                      left: 18mm;
                      top: 15mm;
                      width: 36mm;
                      height: 22mm;
                    }
                    .brand-logo img {
                      width: 36mm;
                      height: auto;
                    }
                    .brand-title {
                      position: absolute;
                      z-index: 1;
                      left: 58mm;
                      top: 19mm;
                      font-size: 23pt;
                      letter-spacing: 0;
                      line-height: 1;
                    }
                    .brand-subtitle {
                      position: absolute;
                      z-index: 1;
                      left: 58mm;
                      top: 31mm;
                      font-size: 13pt;
                      letter-spacing: 0;
                    }
                    .brand-motto {
                      position: absolute;
                      z-index: 1;
                      left: 58mm;
                      top: 42mm;
                      font-size: 9pt;
                      color: #555e57;
                    }
                    .divider {
                      position: absolute;
                      z-index: 1;
                      left: 16mm;
                      right: 16mm;
                      top: 70mm;
                      height: 1px;
                      background: #d7cfc3;
                    }
                    .field {
                      position: absolute;
                      z-index: 1;
                      width: 82mm;
                      height: 11mm;
                      border-bottom: 1px dashed #d7cfc3;
                      line-height: 10mm;
                      font-size: 12pt;
                    }
                    .label {
                      position: absolute;
                      left: 0;
                      top: 0;
                      font-weight: 700;
                    }
                    .value {
                      position: absolute;
                      right: 0;
                      top: 0;
                      max-width: 56mm;
                      text-align: right;
                      font-size: 13pt;
                      font-weight: 500;
                    }
                    .pill {
                      display: inline-block;
                      min-width: 17mm;
                      padding: 0 4mm;
                      line-height: 7.5mm;
                      text-align: center;
                      color: #ffffff;
                      background: #5c6b5f;
                      border-radius: 8mm;
                    }
                    .status { color: #5c6b5f; font-weight: 700; }
                    .note {
                      position: absolute;
                      z-index: 1;
                      left: 16mm;
                      bottom: 13mm;
                      color: #68645d;
                      font-size: 9pt;
                    }
                    .seal {
                      position: absolute;
                      z-index: 1;
                      right: 18mm;
                      bottom: 15mm;
                      width: 13mm;
                      height: 13mm;
                      padding-top: 1.5mm;
                      border: 1.5px solid #b66a5b;
                      color: #b66a5b;
                      text-align: center;
                      line-height: 5mm;
                      font-size: 9pt;
                    }
                    .left-1 { left: 18mm; top: 84mm; }
                    .left-2 { left: 18mm; top: 98mm; }
                    .left-3 { left: 18mm; top: 112mm; }
                    .left-4 { left: 18mm; top: 126mm; }
                    .right-1 { left: 122mm; top: 84mm; }
                    .right-2 { left: 122mm; top: 98mm; }
                    .right-3 { left: 122mm; top: 112mm; }
                    .right-4 { left: 122mm; top: 126mm; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <div class="brand-logo">%s</div>
                    <div class="brand-title">福庫銀行</div>
                    <div class="brand-subtitle">E-PASSBOOK</div>
                    <div class="brand-motto">CALM · BALANCE · TRUST</div>
                    <div class="divider"></div>
                    <div class="field left-1"><span class="label">戶名</span><span class="value">%s</span></div>
                    <div class="field left-2"><span class="label">帳號</span><span class="value">%s</span></div>
                    <div class="field left-3"><span class="label">開戶分行</span><span class="value">總行</span></div>
                    <div class="field left-4"><span class="label">帳戶類型</span><span class="value">%s</span></div>
                    <div class="field right-1"><span class="label">幣別</span><span class="value pill">%s</span></div>
                    <div class="field right-2"><span class="label">銀行代號</span><span class="value">909</span></div>
                    <div class="field right-3"><span class="label">申請日期</span><span class="value">%s</span></div>
                    <div class="field right-4"><span class="label">狀態</span><span class="value status">%s</span></div>
                    <div class="note">本電子存摺僅供參考，實際帳戶資訊以本行系統資料為準。</div>
                    <div class="seal">福庫<br />銀行</div>
                  </div>
                </body>
                </html>
                """.formatted(
                PDF_FONT_FAMILY,
                logo,
                ownerName,
                accountNumber,
                accountType,
                currency,
                createdAt,
                status
        );
    }

    private void registerCjkFont(PdfRendererBuilder builder) {
        builder.useFont(() -> {
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("fonts/SourceHanSansTC-Normal.otf");
            if (is == null) {
                Path systemFontPath = findCjkFontPath();
                if (systemFontPath != null) {
                    try {
                        return new java.io.FileInputStream(systemFontPath.toFile());
                    } catch (Exception e) {
                        // ignore
                    }
                }
                throw new RuntimeException("無法載入中文字型！");
            }
            return is;
        }, PDF_FONT_FAMILY);
    }

    private Path findCjkFontPath() {
        String[] candidates = {
                "/Library/Fonts/Arial Unicode.ttf",
                "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
                "/System/Library/Fonts/STHeiti Medium.ttc",
                "/System/Library/Fonts/STHeiti Light.ttc",
                "C:/Windows/Fonts/msjh.ttc",
                "C:/Windows/Fonts/mingliu.ttc",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/opentype/noto/NotoSansCJKtc-Regular.otf",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/arphic/uming.ttc"
        };
        for (String candidate : candidates) {
            File file = new File(candidate);
            if (file.exists() && file.isFile()) {
                return file.toPath();
            }
        }
        return null;
    }

    private String logoDataUri() {
        Path logoPath = Path.of(System.getProperty("user.dir"), "frontend", "public", "logo.webp");
        if (!Files.exists(logoPath)) {
            return "<div></div>";
        }
        try {
            String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(logoPath));
            return "<img src=\"data:image/webp;base64," + base64 + "\" alt=\"福庫銀行\" />";
        } catch (Exception e) {
            return "<div></div>";
        }
    }

    private String safe(String value) {
        return HtmlUtils.htmlEscape(value == null || value.isBlank() ? "-" : value);
    }

    private String formatAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) return "-";
        if (accountNumber.length() == 12) {
            return accountNumber.substring(0, 3) + "-" + accountNumber.substring(3, 6) + "-" + accountNumber.substring(6);
        }
        return accountNumber;
    }

    private String accountTypeLabel(AccountType type, Currency currency) {
        if (type == null) return "-";
        return switch (type) {
            case CHECKING -> currencyLabel(currency) + "活期存款";
            case SAVINGS -> currencyLabel(currency) + "儲蓄存款";
            case TIME_DEPOSIT -> currencyLabel(currency) + "定期存款";
            case LOAN -> "貸款帳戶";
            case SUB_ACCOUNT -> currencyLabel(currency) + "子帳戶";
            case BUSINESS -> "銀行業務帳戶";
            case CREDIT_CARD -> "信用卡繳款帳戶";
        };
    }

    private String currencyLabel(Currency currency) {
        if (currency == null) return "-";
        return switch (currency) {
            case TWD -> "臺幣";
            case USD -> "美元";
            case EUR -> "歐元";
            case JPY -> "日圓";
            case GBP -> "英鎊";
            case CNY -> "人民幣";
            case AUD -> "澳幣";
            case CAD -> "加幣";
            case CHF -> "瑞郎";
            case HKD -> "港幣";
        };
    }

    private String statusLabel(AccountStatus status) {
        if (status == null) return "-";
        return switch (status) {
            case PENDING -> "待啟用";
            case ACTIVE -> "正常";
            case FROZEN -> "凍結";
            case DORMANT -> "靜止";
            case CLOSED -> "已銷戶";
        };
    }
}

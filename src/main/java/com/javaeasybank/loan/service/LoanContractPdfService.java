package com.javaeasybank.loan.service;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.customer.entity.CustomerProfile;
import com.javaeasybank.customer.repository.CustomerProfileRepository;
import com.javaeasybank.loan.entity.LoanApplication;
import com.javaeasybank.loan.entity.LoanReviewDetail;
import com.javaeasybank.loan.repository.LoanApplicationRepository;
import com.javaeasybank.loan.repository.LoanReviewDetailRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanContractPdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String PDF_FONT_FAMILY = "JavaBankCjk";

    private final CustomerProfileRepository customerProfileRepository;
    private final LoanReviewDetailRepository loanReviewDetailRepository;
    private final LoanApplicationRepository loanApplicationRepository;

    public byte[] generateContractPdf(String applicationId) {
        LoanApplication loan = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到貸款申請：" + applicationId));
        CustomerProfile profile = customerProfileRepository.findById(loan.getCustomerId())
                .orElseThrow(() -> new BusinessException("找不到客戶資料：" + loan.getCustomerId()));
        LoanReviewDetail detail = loanReviewDetailRepository.findByApplicationId(applicationId).orElse(null);

        try {
            return renderHtmlToPdf(buildHtml(loan, profile, detail));
        } catch (Exception e) {
            log.error("[LoanContractPdf] 產生失敗 applicationId={}, error={}", applicationId, e.getMessage(), e);
            throw new BusinessException("貸款契約 PDF 產生失敗");
        }
    }

    private byte[] renderHtmlToPdf(String html) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerCjkFont(builder);
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        }
    }

    private String buildHtml(LoanApplication loan, CustomerProfile profile, LoanReviewDetail detail) {
        String customerName = safe(profile.getName());
        String idNumber = safe(profile.getIdNumber());
        String birthday = profile.getBirthday() == null ? "-" : profile.getBirthday().format(DATE_FORMATTER);
        String phone = safe(profile.getPhone());
        String email = safe(profile.getEmail());
        String address = safe(firstNonBlank(profile.getCurrentAddress(), profile.getAddress(), profile.getRegisteredAddress()));
        String occupation = safe(firstNonBlank(profile.getOccupation(), profile.getJob()));
        String employer = safe(profile.getEmployer());
        String annualIncome = profile.getAnnualIncome() == null ? "-" : "NT$ " + formatMoney(BigDecimal.valueOf(profile.getAnnualIncome()));

        String loanType = safe(formatLoanType(loan.getApplyType()));
        String purpose = safe(formatPurposeByLoanType(loan.getApplyType()));
        String applyAmount = "NT$ " + formatMoney(loan.getApplyAmount());
        String confirmedAmount = "NT$ " + formatMoney(detail != null && detail.getConfirmedAmount() != null
                ? detail.getConfirmedAmount() : loan.getApplyAmount());
        String confirmedPeriod = detail != null && detail.getConfirmedPeriod() != null
                ? detail.getConfirmedPeriod().toString() : String.valueOf(loan.getApplyPeriod());
        String confirmedRate = formatAnnualRate(detail != null && detail.getConfirmedRate() != null
                ? detail.getConfirmedRate() : loan.getRate());
        String disbursementAccount = safe(loan.getDisbursementAccount());
        String contractNo = safe(formatContractNumber(loan.getApplicationId()));
        String reviewComment = safe(firstNonBlank(loan.getReviewComment(),
                detail == null ? null : detail.getReviewNote(),
                "無"));
        String contractDate = LocalDateTime.now().format(DATE_TIME_FORMATTER);

        return """
                <!DOCTYPE html>
                <html lang="zh-Hant">
                <head>
                  <meta charset="UTF-8" />
                  <style>
                    @page { size: A4 portrait; margin: 16mm 15mm 18mm; }
                    * { box-sizing: border-box; }
                    body {
                      margin: 0;
                      color: #202124;
                      font-family: "%s", "Microsoft JhengHei", "Noto Sans CJK TC", sans-serif;
                      line-height: 1.55;
                    }
                    .wrap { width: 100%%; }
                    .eyebrow {
                      font-size: 9pt;
                      color: #6f6f6f;
                      text-align: right;
                      margin-bottom: 3mm;
                    }
                    .head { text-align: center; margin-bottom: 8mm; }
                    h1 {
                      margin: 0;
                      font-size: 22pt;
                      letter-spacing: 0;
                    }
                    .sub {
                      margin-top: 2mm;
                      color: #5b5b5b;
                      font-size: 10.5pt;
                    }
                    .panel {
                      border: 1px solid #d9dee6;
                      background: #f6f8fb;
                      padding: 4.5mm 5.5mm;
                      margin: 0 0 6mm;
                    }
                    .panel p { margin: 0; font-size: 10.5pt; color: #4d4d4d; }
                    h2 {
                      margin: 5mm 0 3mm;
                      font-size: 13.5pt;
                      color: #245a8d;
                      border-bottom: 1px solid #e7ebf0;
                      padding-bottom: 1.5mm;
                    }
                    table { width: 100%%; border-collapse: collapse; table-layout: fixed; }
                    .summary th, .summary td {
                      border: 1px solid #d9dee6;
                      padding: 3.1mm 3.2mm;
                      vertical-align: middle;
                      word-wrap: break-word;
                      font-size: 10pt;
                    }
                    .summary th {
                      background: #e9eef5;
                      color: #214f79;
                      width: 19%%;
                      text-align: center;
                    }
                    .summary td { width: 31%%; }
                    .section p {
                      margin: 0 0 3mm;
                      font-size: 10.5pt;
                      text-align: justify;
                    }
                    .clause { margin: 0 0 3mm; }
                    .clause-title {
                      font-weight: 700;
                      color: #214f79;
                      font-size: 10.8pt;
                      margin-bottom: 1mm;
                    }
                    .sign { margin-top: 5mm; }
                    .sign td, .sign th {
                      border: 1px solid #d9dee6;
                      padding: 4mm 3mm;
                      font-size: 10pt;
                      text-align: center;
                    }
                    .sign th { background: #f6f7f8; color: #214f79; }
                    .sigline {
                      margin: 5mm 0 2mm;
                      font-family: "Courier New", monospace;
                    }
                    .small-note {
                      margin-top: 4mm;
                      font-size: 8.8pt;
                      color: #666;
                      font-style: italic;
                    }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <div class="eyebrow">系統產生 / 內部示意預覽</div>
                    <div class="head">
                      <h1>貸款契約書</h1>
                      <div class="sub">個人信貸契約示意稿</div>
                    </div>

                    <div class="panel">
                      <p>本契約依契約編號 %s 之最終填單與審核結果彙整而成，供借款人確認契約內容、核貸條件與撥款資訊之用。</p>
                    </div>

                    <h2>一、借款人與契約摘要</h2>
                    <table class="summary">
                      <tr><th>借款人姓名</th><td>%s</td><th>身分證字號</th><td>%s</td></tr>
                      <tr><th>出生日期</th><td>%s</td><th>聯絡電話</th><td>%s</td></tr>
                      <tr><th>電子郵件</th><td>%s</td><th>聯絡地址</th><td>%s</td></tr>
                      <tr><th>職業</th><td>%s</td><th>任職機構</th><td>%s</td></tr>
                      <tr><th>年收入</th><td>%s</td><th>貸款類型</th><td>%s</td></tr>
                      <tr><th>申請用途</th><td>%s</td><th>申請金額</th><td>%s</td></tr>
                      <tr><th>核定金額</th><td>%s</td><th>核定期數</th><td>%s 期</td></tr>
                      <tr><th>年利率</th><td>%s</td><th>撥款帳號</th><td>%s</td></tr>
                    </table>

                    <h2>二、契約條款摘要</h2>
                    <div class="section">
                      <div class="clause">
                        <div class="clause-title">1. 借款內容</div>
                        <p>借款人同意依本契約核定金額與年利率辦理貸款，並依核定期數按月攤還本金與利息。實際撥款金額、月付金與還款日期，依系統最終核定資料為準。</p>
                      </div>
                      <div class="clause">
                        <div class="clause-title">2. 撥款與還款</div>
                        <p>貸款核准後，撥款將匯入借款人指定之撥款帳號。借款人應於每期應繳日前完成還款；若有提前清償、部分清償或其他異動，悉依本行規定及系統通知為準。</p>
                      </div>
                      <div class="clause">
                        <div class="clause-title">3. 審核與補充說明</div>
                        <p>審核備註：%s</p>
                      </div>
                      <div class="clause">
                        <div class="clause-title">4. 資料確認</div>
                        <p>借款人確認上述資料係依最終填單內容與審核結果產製，如內容有誤，應立即聯繫承辦窗口辦理修正。</p>
                      </div>
                    </div>

                    <h2>三、簽署欄位</h2>
                    <table class="sign">
                      <tr><th>借款人簽名</th><th>承辦人員</th><th>簽署日期</th></tr>
                      <tr>
                        <td>________________________</td>
                        <td>________________________</td>
                        <td>%s</td>
                      </tr>
                    </table>

                    <p class="sigline">契約編號：%s</p>
                    <p class="small-note">本文件為系統產生之契約預覽稿，實際內容以正式簽署版本及本行最終核准資料為準。</p>
                  </div>
                </body>
                </html>
                """.formatted(
                PDF_FONT_FAMILY,
                contractNo,
                customerName,
                idNumber,
                birthday,
                phone,
                email,
                address,
                occupation,
                employer,
                annualIncome,
                loanType,
                purpose,
                applyAmount,
                confirmedAmount,
                confirmedPeriod,
                confirmedRate,
                disbursementAccount,
                reviewComment,
                contractDate,
                contractNo
        );
    }

    private void registerCjkFont(PdfRendererBuilder builder) {
        builder.useFont(() -> {
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("fonts/SourceHanSansTC-Normal.otf");
            if (is == null) {
                log.warn("[LoanContractPdf] 無法載入 classpath 中的中文字型 fonts/SourceHanSansTC-Normal.otf，嘗試使用系統字型");
                Path systemFontPath = findCjkFontPath();
                if (systemFontPath != null) {
                    try {
                        return new java.io.FileInputStream(systemFontPath.toFile());
                    } catch (Exception e) {
                        log.error("[LoanContractPdf] 載入後備系統字型失敗: {}", e.getMessage());
                    }
                }
                throw new RuntimeException("無法載入中文字型！");
            }
            return is;
        }, PDF_FONT_FAMILY);
    }

    private Path findCjkFontPath() {
        String[] candidates = {
                "C:/Windows/Fonts/msjh.ttc",
                "C:/Windows/Fonts/msjhbd.ttc",
                "C:/Windows/Fonts/mingliu.ttc",
                "/Library/Fonts/Arial Unicode.ttf",
                "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
                "/System/Library/Fonts/STHeiti Medium.ttc",
                "/System/Library/Fonts/STHeiti Light.ttc",
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

    private String safe(String value) {
        return HtmlUtils.htmlEscape(value == null || value.isBlank() ? "-" : value);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "-";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "-";
    }

    private String formatLoanType(String loanType) {
        if (loanType == null || loanType.isBlank()) {
            return "-";
        }
        return switch (loanType.trim()) {
            case "PERSONAL" -> "個人信貸";
            case "CAR" -> "汽車貸款";
            case "MOTOR" -> "機車貸款";
            case "STUDENT" -> "學生貸款";
            case "BUSINESS" -> "企業貸款";
            case "HOUSE" -> "房屋貸款";
            case "LAND" -> "土地貸款";
            default -> loanType;
        };
    }

    private String formatPurposeByLoanType(String loanType) {
        if (loanType == null || loanType.isBlank()) {
            return "-";
        }
        return switch (loanType.trim()) {
            case "PERSONAL" -> "個人周轉";
            case "CAR" -> "購車資金";
            case "MOTOR" -> "機車購置";
            case "STUDENT" -> "就學支出";
            case "BUSINESS" -> "營運週轉";
            case "HOUSE" -> "房屋相關";
            case "LAND" -> "土地相關";
            default -> "-";
        };
    }

    private String formatAnnualRate(BigDecimal rate) {
        if (rate == null) {
            return "-";
        }
        BigDecimal percent = rate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        return percent.stripTrailingZeros().toPlainString() + "%";
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        DecimalFormat formatter = new DecimalFormat("#,##0.##");
        return formatter.format(amount);
    }

    private String formatContractNumber(String applicationId) {
        if (applicationId == null || applicationId.isBlank()) {
            return "-";
        }
        if (applicationId.startsWith("LA")) {
            return "LC-" + applicationId.substring(2);
        }
        return "LC-" + applicationId;
    }
}

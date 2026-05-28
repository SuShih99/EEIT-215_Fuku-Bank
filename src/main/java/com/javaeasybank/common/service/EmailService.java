package com.javaeasybank.common.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@Async
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "Fuku Bank-福庫銀行");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            // 加入內嵌圖片 (Content-ID) 解決 Gmail 破圖問題
            helper.addInline("logoImage", new org.springframework.core.io.ClassPathResource("static/images/logo.webp"));
            helper.addInline("sealImage", new org.springframework.core.io.ClassPathResource("static/images/fukubank-seal.webp"));

            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public void sendVerificationEmail(String to, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;
        Context context = new Context();
        context.setVariable("link", link);
        String html = templateEngine.process("mail/verification-email", context);
        sendEmail(to, "Fuku Bank - 電子郵件驗證", html);
    }

    public void sendLoginNotification(String to, String username, boolean success, String ipAddress) {
        String time = LocalDateTime.now().format(formatter);
        String status = success ? "成功" : "失敗";
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("time", time);
        context.setVariable("status", status);
        context.setVariable("ipAddress", ipAddress);
        context.setVariable("success", success);
        String html = templateEngine.process("mail/login-notification", context);
        sendEmail(to, "Fuku Bank - 登入通知", html);
    }

    public void sendTransferNotification(String to, String fromAccount, String toAccount, BigDecimal amount, String currency, String referenceId) {
        String time = LocalDateTime.now().format(formatter);
        Context context = new Context();
        context.setVariable("time", time);
        context.setVariable("referenceId", referenceId);
        context.setVariable("fromAccount", fromAccount);
        context.setVariable("toAccount", toAccount);
        context.setVariable("amount", amount);
        context.setVariable("currency", currency);
        String html = templateEngine.process("mail/transfer-notification", context);
        sendEmail(to, "Fuku Bank - 轉帳交易通知", html);
    }

    public void sendPasswordResetEmail(String to, String link) {
        Context context = new Context();
        context.setVariable("link", link);
        String html = templateEngine.process("mail/password-reset", context);
        sendEmail(to, "Fuku Bank - 密碼重設連結", html);
    }

    /** 寄送轉帳交易安全覆核中的進度通知。 */
    public void sendTransferPendingNotification(String to, String fromAccount, String toAccount, BigDecimal amount, String currency, String referenceId) {
        String time = LocalDateTime.now().format(formatter);
        Context context = new Context();
        context.setVariable("time", time);
        context.setVariable("referenceId", referenceId);
        context.setVariable("fromAccount", fromAccount);
        context.setVariable("toAccount", toAccount);
        context.setVariable("amount", amount);
        context.setVariable("currency", currency);

        String html = templateEngine.process("mail/transfer-pending", context);
        sendEmail(to, "Fuku Bank - 轉帳交易處理進度通知", html);
    }

    public void sendTransferOtpEmail(String to, String otp) {
        Context context = new Context();
        context.setVariable("otp", otp);
        String html = templateEngine.process("mail/transfer-otp", context);
        sendEmail(to, "Fuku Bank - 轉帳交易驗證碼 (OTP)", html);
    }
    /** 寄送信用卡月結帳單通知。 */
    public void sendCardBillStatementEmail(
            String to,
            String customerName,
            String billingMonth,
            BigDecimal totalAmount,
            BigDecimal minimumPayment,
            LocalDate dueDate,
            String filename,
            byte[] attachmentBytes) {

        Context context = new Context();
        context.setVariable("customerName", customerName);
        context.setVariable("billingMonth", billingMonth);
        context.setVariable("totalAmount", totalAmount);
        context.setVariable("minimumPayment", minimumPayment);
        context.setVariable("dueDate", dueDate);

        String html = templateEngine.process("mail/card-bill-statement", context);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "Fuku Bank-福庫銀行");
            helper.setTo(to);
            helper.setSubject("福庫銀行 Fuku-Bank - 信用卡月結帳單");
            helper.setText(html, true);

            // 加入內嵌圖片 (Content-ID) 解決 Gmail 破圖問題
            helper.addInline("logoImage", new org.springframework.core.io.ClassPathResource("static/images/logo.webp"));
            helper.addInline("sealImage", new org.springframework.core.io.ClassPathResource("static/images/fukubank-seal.webp"));

            helper.addAttachment(
                    filename,
                    new ByteArrayResource(attachmentBytes)
            );

            mailSender.send(message);
            log.info("Email sent to {}: 福庫銀行 Fuku-Bank - 信用卡月結帳單", to);
        } catch (Exception e) {
            log.error("Failed to send email with attachment to {}: {}", to, e.getMessage());
        }
    }

    /** 寄送貸款契約書通知。 */
    public void sendLoanContractEmail(
            String to,
            String customerName,
            String contractNo,
            String filename,
            byte[] attachmentBytes) {

        Context context = new Context();
        context.setVariable("customerName", customerName);
        context.setVariable("contractNo", contractNo);

        String html = templateEngine.process("mail/loan-contract", context);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "Fuku Bank-福庫銀行");
            helper.setTo(to);
            helper.setSubject("福庫銀行 Fuku-Bank - 貸款契約書");
            helper.setText(html, true);

            // 加入內嵌圖片 (Content-ID) 解決 Gmail 破圖問題
            helper.addInline("logoImage", new org.springframework.core.io.ClassPathResource("static/images/logo.webp"));
            helper.addInline("sealImage", new org.springframework.core.io.ClassPathResource("static/images/fukubank-seal.webp"));

            helper.addAttachment(
                    filename,
                    new ByteArrayResource(attachmentBytes)
            );

            mailSender.send(message);
            log.info("Email sent to {}: 福庫銀行 Fuku-Bank - 貸款契約書", to);
        } catch (Exception e) {
            log.error("Failed to send loan contract email with attachment to {}: {}", to, e.getMessage());
        }
    }

    public void sendLoanDocumentRequiredNotification(
            String to,
            String referenceId,
            String loanType,
            BigDecimal amount) {
        sendLoanDocumentRequiredNotification(to, referenceId, loanType, amount, null, null);
    }

    public void sendLoanDocumentRequiredNotification(
            String to,
            String referenceId,
            String loanType,
            BigDecimal amount,
            List<String> requiredDocuments,
            String adminComment) {

        Context context = new Context();
        context.setVariable("time", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        context.setVariable("referenceId", referenceId);
        context.setVariable("loanType", formatLoanType(loanType));
        context.setVariable("amount", amount);
        context.setVariable("requiredDocuments", formatRequiredDocuments(requiredDocuments));
        context.setVariable("adminComment", adminComment);

        String html = templateEngine.process("mail/loan-document-required", context);

        sendEmail(to, "Fuku Bank - 貸款申請補件通知", html);
    }

    public void sendAccountLockedNotification(String to, String username,String ipAddress) {
        String time = LocalDateTime.now().format(formatter);
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("time", time);
        context.setVariable("ipAddress", ipAddress);
        String html = templateEngine.process("mail/account-locked-notification", context);
        sendEmail(to, "Fuku Bank - 登入通知", html);
    }
    /** 寄送 HTML 郵件與單一附件。 */
    public void sendEmailWithAttachment(
            String to,
            String subject,
            String content,
            String filename,
            byte[] attachmentBytes) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "FukuBank-福庫銀行");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            helper.addAttachment(
                    filename,
                    new ByteArrayResource(attachmentBytes)
            );

            mailSender.send(message);

        } catch (Exception e) {
            log.error("Failed to send email with attachment to {}: {}", to, e.getMessage());
        }
    }

    private String formatLoanType(String loanType) {
        if (loanType == null || loanType.isBlank()) {
            return "";
        }
        return switch (loanType) {
            case "PERSONAL" -> "個人信貸";
            case "CAR" -> "汽車貸款";
            case "MOTOR" -> "機車貸款";
            case "STUDENT" -> "學貸";
            case "BUSINESS" -> "創業貸款";
            case "HOUSE" -> "房屋貸款";
            case "LAND" -> "土地貸款";
            default -> loanType;
        };
    }

    private List<String> formatRequiredDocuments(List<String> requiredDocuments) {
        if (requiredDocuments == null || requiredDocuments.isEmpty()) {
            return List.of();
        }
        return requiredDocuments.stream()
                .filter(document -> document != null && !document.isBlank())
                .map(document -> formatLoanDocumentType(document.trim()))
                .toList();
    }

    private String formatLoanDocumentType(String documentType) {
        return switch (documentType) {
            case "ID_CARD" -> "身分證明文件";
            case "INCOME_CERT" -> "收入證明";
            case "EMPLOYMENT_CERT" -> "在職證明";
            case "BANK_STATEMENT" -> "銀行往來明細";
            case "PROPERTY_CERT" -> "財產證明";
            case "TITLE_DEED" -> "不動產權狀";
            case "OTHER" -> "其他補充文件";
            default -> documentType;
        };
    }

    /** 申請成立：客戶送出申請後立即通知 */
    public void sendLoanAppliedNotification(
            String to,
            String applicationId,
            String loanType,
            BigDecimal amount,
            Integer period) {

        String time = LocalDateTime.now().format(formatter);
        Context context = new Context();
        context.setVariable("time", time);
        context.setVariable("applicationId", applicationId);
        context.setVariable("loanType", formatLoanType(loanType));
        context.setVariable("amount", amount);
        context.setVariable("period", period);

        String html = templateEngine.process("mail/loan-applied", context);
        sendEmail(to, "Fuku Bank - 貸款申請受理通知", html);
    }

    /** 風控拒絕：風控回傳 REJECTED 後通知 */
    public void sendLoanRejectedNotification(
            String to,
            String applicationId,
            String loanType,
            BigDecimal amount) {

        String time = LocalDateTime.now().format(formatter);
        Context context = new Context();
        context.setVariable("time", time);
        context.setVariable("applicationId", applicationId);
        context.setVariable("loanType", formatLoanType(loanType));
        context.setVariable("amount", amount);

        String html = templateEngine.process("mail/loan-rejected", context);
        sendEmail(to, "Fuku Bank - 貸款申請審核結果通知", html);
    }

    /** 核准暨撥款：ACCOUNT 回調確認 DISBURSED、貸款帳號建立後通知 */
    public void sendLoanApprovedAndDisbursedNotification(
            String to,
            String applicationId,
            String loanType,
            BigDecimal confirmedAmount,
            Integer confirmedPeriod,
            BigDecimal confirmedRate,
            String loanAccountNumber,
            String disbursementAccount,
            String firstPaymentDate) {

        String time = LocalDateTime.now().format(formatter);
        Context context = new Context();
        context.setVariable("time", time);
        context.setVariable("applicationId", applicationId);
        context.setVariable("loanType", formatLoanType(loanType));
        context.setVariable("confirmedAmount", confirmedAmount);
        context.setVariable("confirmedPeriod", confirmedPeriod);
        context.setVariable("confirmedRate", formatAnnualRate(confirmedRate));
        context.setVariable("loanAccountNumber", loanAccountNumber);
        context.setVariable("disbursementAccount", disbursementAccount);
        context.setVariable("firstPaymentDate", firstPaymentDate);

        String html = templateEngine.process("mail/loan-disbursed", context);
        sendEmail(to, "Fuku Bank - 貸款核准暨撥款通知", html);
    }

    /** 還款成功：每次繳款完成後通知 */
    public void sendLoanRepaymentPaidNotification(
            String to,
            String loanAccountNumber,
            Integer periodIndex,
            Integer totalPeriods,
            BigDecimal totalAmount,
            BigDecimal principalPortion,
            BigDecimal interestPortion,
            String nextPaymentDate,
            BigDecimal nextAmount) {

        String time = LocalDateTime.now().format(formatter);
        Context context = new Context();
        context.setVariable("time", time);
        context.setVariable("loanAccountNumber", loanAccountNumber);
        context.setVariable("periodIndex", periodIndex);
        context.setVariable("totalPeriods", totalPeriods);
        context.setVariable("totalAmount", totalAmount);
        context.setVariable("principalPortion", principalPortion);
        context.setVariable("interestPortion", interestPortion);
        context.setVariable("nextPaymentDate", nextPaymentDate);
        context.setVariable("nextAmount", nextAmount);

        String html = templateEngine.process("mail/loan-repayment-paid", context);
        sendEmail(to, "Fuku Bank - 還款成功通知", html);
    }

    /** 全數結清：所有期數繳完、closeApplication 完成後通知 */
    public void sendLoanPaidOffNotification(
            String to,
            String applicationId,
            String loanAccountNumber,
            String loanType,
            Integer totalPeriods) {

        String time = LocalDateTime.now().format(formatter);
        Context context = new Context();
        context.setVariable("time", time);
        context.setVariable("applicationId", applicationId);
        context.setVariable("loanAccountNumber", loanAccountNumber);
        context.setVariable("loanType", formatLoanType(loanType));
        context.setVariable("totalPeriods", totalPeriods);

        String html = templateEngine.process("mail/loan-paid-off", context);
        sendEmail(to, "Fuku Bank - 貸款結清通知", html);
    }

    /** 逾期通知：排程掃到逾期期數時逐筆觸發 */
    public void sendLoanOverdueNotification(
            String to,
            String loanAccountNumber,
            Integer periodIndex,
            String dueDate,
            BigDecimal overdueAmount) {

        String time = LocalDateTime.now().format(formatter);
        Context context = new Context();
        context.setVariable("time", time);
        context.setVariable("loanAccountNumber", loanAccountNumber);
        context.setVariable("periodIndex", periodIndex);
        context.setVariable("dueDate", dueDate);
        context.setVariable("overdueAmount", overdueAmount);

        String html = templateEngine.process("mail/loan-overdue", context);
        sendEmail(to, "Fuku Bank - 貸款逾期繳款通知", html);
    }

    /** 繳款到期提醒：排程掃到距應繳日 3 天內的期數時逐筆觸發 */
    public void sendLoanRepaymentReminderNotification(
            String to,
            String loanAccountNumber,
            Integer periodIndex,
            Integer totalPeriods,
            String dueDate,
            Integer daysLeft,
            BigDecimal amount) {

        Context context = new Context();
        context.setVariable("loanAccountNumber", loanAccountNumber);
        context.setVariable("periodIndex", periodIndex);
        context.setVariable("totalPeriods", totalPeriods);
        context.setVariable("dueDate", dueDate);
        context.setVariable("daysLeft", daysLeft);
        context.setVariable("amount", amount);

        String html = templateEngine.process("mail/loan-repayment-reminder", context);
        sendEmail(to, "Fuku Bank - 貸款繳款到期提醒", html);
    }


    private String formatAnnualRate(BigDecimal rate) {
        if (rate == null) {
            return "";
        }
        BigDecimal percent = rate.abs().compareTo(BigDecimal.ONE) <= 0
                ? rate.multiply(BigDecimal.valueOf(100))
                : rate;
        return percent.setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString() + "%";
    }
}

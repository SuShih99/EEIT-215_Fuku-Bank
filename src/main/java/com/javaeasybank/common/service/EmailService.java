package com.javaeasybank.common.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Async
    public void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
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
        sendEmail(to, "Java Easy Bank - 電子郵件驗證", html);
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
        sendEmail(to, "Java Easy Bank - 登入通知", html);
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
        sendEmail(to, "Java Easy Bank - 轉帳交易通知", html);
    }

    public void sendPasswordResetEmail(String to, String link) {
        Context context = new Context();
        context.setVariable("link", link);
        String html = templateEngine.process("mail/password-reset", context);
        sendEmail(to, "Java Easy Bank - 密碼重設連結", html);
    }

    /**
     * ─── 新增：轉帳交易安全覆核中（審核中）通知信 ───
     * 採用隱晦委婉、站在維護資金安全角度的文案設計
     */
    public void sendTransferPendingNotification(String to, String fromAccount, String toAccount, BigDecimal amount, String currency, String referenceId) {
        String time = LocalDateTime.now().format(formatter);
        Context context = new Context();
        context.setVariable("time", time);
        context.setVariable("referenceId", referenceId);
        context.setVariable("fromAccount", fromAccount);
        context.setVariable("toAccount", toAccount);
        context.setVariable("amount", amount);
        context.setVariable("currency", currency);

        // 渲染對應的審核中 HTML 樣板
        String html = templateEngine.process("mail/transfer-pending", context);

        // 使用溫和的主旨，不使用「風控」、「審核」、「攔截」等字眼
        sendEmail(to, "Java Easy Bank - 轉帳交易處理進度通知", html);
    }
    //月結信用卡帳單
    @Async
    public void sendCardBillStatementEmail(
        String to,
        String customerName,
        String billingMonth,
        BigDecimal totalAmount,
        BigDecimal minimumPayment,
        LocalDate dueDate,
        Integer billId) {

    Context context = new Context();

    context.setVariable("customerName", customerName);
    context.setVariable("billingMonth", billingMonth);
    context.setVariable("totalAmount", totalAmount);
    context.setVariable("minimumPayment", minimumPayment);
    context.setVariable("dueDate", dueDate);
    context.setVariable("billId", billId);

    String html = templateEngine.process(
            "mail/card-bill-statement",
            context
    );

    sendEmail(
            to,
            "Java Easy Bank - 信用卡月結帳單通知",
            html
    );
}

    public void sendLoanDocumentRequiredNotification(
            String to,
            String referenceId,
            String loanType,
            BigDecimal amount) {

        Context context = new Context();
        context.setVariable("time", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        context.setVariable("referenceId", referenceId);
        context.setVariable("loanType", loanType);
        context.setVariable("amount", amount);

        String html = templateEngine.process("mail/loan-document-required", context);

        sendEmail(to, "Java Easy Bank - 貸款申請補件通知", html);
    }

    public void sendAccountLockedNotification(String to, String username,String ipAddress) {}

    // ─── 貸款模組通知 ───

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
        context.setVariable("loanType", loanType);
        context.setVariable("amount", amount);
        context.setVariable("period", period);

        String html = templateEngine.process("mail/loan-applied", context);
        sendEmail(to, "Java Easy Bank - 貸款申請受理通知", html);
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
        context.setVariable("loanType", loanType);
        context.setVariable("amount", amount);

        String html = templateEngine.process("mail/loan-rejected", context);
        sendEmail(to, "Java Easy Bank - 貸款申請審核結果通知", html);
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
        context.setVariable("loanType", loanType);
        context.setVariable("confirmedAmount", confirmedAmount);
        context.setVariable("confirmedPeriod", confirmedPeriod);
        context.setVariable("confirmedRate", confirmedRate);
        context.setVariable("loanAccountNumber", loanAccountNumber);
        context.setVariable("disbursementAccount", disbursementAccount);
        context.setVariable("firstPaymentDate", firstPaymentDate);

        String html = templateEngine.process("mail/loan-disbursed", context);
        sendEmail(to, "Java Easy Bank - 貸款核准暨撥款通知", html);
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
        sendEmail(to, "Java Easy Bank - 還款成功通知", html);
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
        context.setVariable("loanType", loanType);
        context.setVariable("totalPeriods", totalPeriods);

        String html = templateEngine.process("mail/loan-paid-off", context);
        sendEmail(to, "Java Easy Bank - 貸款結清通知", html);
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
        sendEmail(to, "Java Easy Bank - 貸款逾期繳款通知", html);
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
        sendEmail(to, "Java Easy Bank - 貸款繳款到期提醒", html);
    }

    //郵件附件
    @Async
    public void sendEmailWithAttachment(
        String to,
        String subject,
        String content,
        String filename,
        byte[] attachmentBytes) {

    try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
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
}


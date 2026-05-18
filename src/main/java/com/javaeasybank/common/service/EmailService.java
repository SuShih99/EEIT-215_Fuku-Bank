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


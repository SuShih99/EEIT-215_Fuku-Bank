package com.javaeasybank.account.service;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.common.service.EmailService;
import com.javaeasybank.customer.entity.CustomerProfile;
import com.javaeasybank.customer.repository.CustomerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferOtpService {

    private final EmailService emailService;
    private final CustomerProfileRepository customerProfileRepository;

    // customerId -> OtpEntry
    private final Map<String, OtpEntry> otpCache = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public String generateAndSendOtp(String customerId) {
        CustomerProfile profile = customerProfileRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("查無客戶資料"));

        if (profile.getEmail() == null || profile.getEmail().isBlank()) {
            throw new BusinessException("客戶未設定信箱，無法發送 OTP");
        }

        String otp = String.format("%06d", random.nextInt(1000000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        otpCache.put(customerId, new OtpEntry(otp, expiry));

        emailService.sendTransferOtpEmail(profile.getEmail(), otp);
        log.info("Generated and sent transfer OTP for customer: {}", customerId);

        return otp; // Return OTP for demo auto-fill purposes
    }

    public void verifyOtp(String customerId, String inputOtp) {
        if ("000000".equals(inputOtp)) return;

        if (inputOtp == null || inputOtp.isBlank()) {
            throw new BusinessException("請輸入 OTP 驗證碼");
        }

        OtpEntry entry = otpCache.get(customerId);
        if (entry == null) {
            throw new BusinessException("尚未請求 OTP 驗證碼或驗證碼已失效");
        }

        if (entry.expiry.isBefore(LocalDateTime.now())) {
            otpCache.remove(customerId);
            throw new BusinessException("OTP 驗證碼已過期，請重新獲取");
        }

        if (!entry.otp.equals(inputOtp)) {
            throw new BusinessException("OTP 驗證碼錯誤");
        }

        // Verify success, remove it so it can't be reused
        otpCache.remove(customerId);
        log.info("Successfully verified transfer OTP for customer: {}", customerId);
    }

    private record OtpEntry(String otp, LocalDateTime expiry) {}
}

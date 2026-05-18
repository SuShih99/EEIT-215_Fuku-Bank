package com.javaeasybank.customer.service;

import com.javaeasybank.account.enums.FundSource;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.common.util.JwtUtil;
import com.javaeasybank.customer.entity.CustomerDevice;
import com.javaeasybank.customer.entity.CustomerLoginLog;
import com.javaeasybank.customer.repository.CustomerRespository;
import com.javaeasybank.customer.entity.CustomerAuth;
import com.javaeasybank.customer.entity.CustomerProfile;
import com.javaeasybank.customer.repository.CustomerAuthRepository;
import com.javaeasybank.customer.LoginRiskClient;
import com.javaeasybank.customer.repository.CustomerDeviceRepository;
import com.javaeasybank.customer.repository.CustomerLoginLogRepository;
import com.javaeasybank.customer.repository.CustomerProfileRepository;
import com.javaeasybank.common.service.EmailService;
import com.javaeasybank.customer.util.TaiwanIdValidator;
import com.javaeasybank.risk.enums.Occupation;
import com.javaeasybank.risk.repository.CustomerCreditRepository;
import com.javaeasybank.risk.service.CreditScoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * 客戶認證服務實作：
 * 處理客戶註冊（同時建立 customer_profile + customer_auth）、
 * 登入（驗證帳密 + 發放 JWT）、個人資料維護、密碼重設等。
 */
@Service
@Slf4j
public class CustomerAuthServiceImpl implements CustomerAuthService {

    private final CustomerAuthRepository customerAuthRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerLoginLogRepository customerLoginLogRepository;
    private final CustomerDeviceRepository customerDeviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final LoginRiskClient loginRiskClient;
    private final LoginAuditService loginAuditService;
    private final CreditScoreService  creditScoreService;
    private final CustomerCreditRepository  customerCreditRepository;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;


    // 用於產生隨機英數
    private static final String ALPHANUMERIC_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final String TAIWAN_MOBILE_PATTERN = "^09\\d{8}$";
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String CUSTOMER_NAME_PATTERN = "^[\\p{IsHan}A-Za-z\\s]{2,30}$";
    private final SecureRandom secureRandom = new SecureRandom();

    public CustomerAuthServiceImpl(CustomerAuthRepository customerAuthRepository,
                                   CustomerProfileRepository customerProfileRepository,
                                   CustomerLoginLogRepository customerLoginLogRepository,
                                   CustomerDeviceRepository customerDeviceRepository,
                                   PasswordEncoder passwordEncoder,
                                   JwtUtil jwtUtil,
                                   EmailService emailService,
                                   LoginRiskClient loginRiskClient,
                                   LoginAuditService loginAuditService, CreditScoreService creditScoreService, CustomerCreditRepository customerCreditRepository) {
        this.customerAuthRepository = customerAuthRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.customerLoginLogRepository = customerLoginLogRepository;
        this.customerDeviceRepository = customerDeviceRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.loginRiskClient = loginRiskClient;
        this.loginAuditService = loginAuditService;
        this.creditScoreService = creditScoreService;
        this.customerCreditRepository = customerCreditRepository;
    }

    // ===========================
    // 註冊
    // ===========================
    @Override
    @Transactional
    public CustomerRespository.LoginResponse register(CustomerRespository.RegisterRequest request) {
        String username = request.getUsername() == null ? null : request.getUsername().trim();
        String email = request.getEmail() == null ? null : request.getEmail().trim();
        String phone = request.getPhone() == null ? null : request.getPhone().trim();
        String idNumber = TaiwanIdValidator.normalize(request.getIdNumber());

        if (!TaiwanIdValidator.isValid(idNumber)) {
            throw new BusinessException("身分證字號格式不正確");
        }
        if (!isValidCustomerName(request.getName())) {
            throw new BusinessException("姓名格式不正確，請輸入 2 到 30 個中英文字符");
        }
        if (!isValidEmail(email)) {
            throw new BusinessException("電子信箱格式不正確");
        }
        if (!isValidTaiwanMobile(phone)) {
            throw new BusinessException("手機號碼格式不正確，請輸入 09 開頭共 10 碼");
        }

        // 1. Demo 保留 username 唯一，其他身分資料僅做格式驗證
        if (customerAuthRepository.existsByUsername(username)) {
            throw new BusinessException("使用者帳號已存在");
        }

        // 2. 建立 customer_profile
        String customerId = generateAlphanumeric(8);
        String yymm = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM"));
        String cif = yymm + "-" + generateAlphanumeric(8);

        CustomerProfile profile = new CustomerProfile();
        profile.setCustomerId(customerId);
        profile.setCif(cif);
        profile.setIdNumber(idNumber);
        profile.setName(request.getName());
        profile.setBirthday(request.getBirthday());
        profile.setGender(request.getGender());
        profile.setEmail(email);
        profile.setPhone(phone);
        profile.setAddress(request.getAddress());
        profile.setRegisteredAddress(request.getAddress());
        profile.setCurrentAddress(request.getAddress());
        profile.setIsPep(false);
        profile.setStatus("ACTIVE");
        customerProfileRepository.save(profile);

        // 3. 建立 customer_auth
        String verificationToken = UUID.randomUUID().toString();
        CustomerAuth auth = new CustomerAuth();
        auth.setAuthId(generateAlphanumeric(10));
        auth.setCustomerId(customerId);
        auth.setUsername(username);
        auth.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        auth.setRole("CUSTOMER");
        auth.setStatus("PENDING"); // 註冊後預設為 PENDING
        auth.setVerificationToken(verificationToken);
        customerAuthRepository.save(auth);

        // 4. 發送驗證信到使用者填寫的信箱
        emailService.sendVerificationEmail(email, verificationToken);

        CustomerRespository.LoginResponse response = new CustomerRespository.LoginResponse();
        response.setCustomerId(customerId);
        response.setCif(cif);
        response.setName(request.getName());
        response.setUsername(username);
        response.setRole("CUSTOMER");
        return response;
    }

    // ===========================
    // 登入
    // ===========================
    @Override
    public CustomerRespository.LoginResponse login(CustomerRespository.LoginRequest request, String ipAddress,
            String userAgent) {
        String deviceName = resolveDeviceName(userAgent);
        String location = (ipAddress == null || ipAddress.isBlank() ? "未知位置" : ipAddress) + " / " + deviceName;

        // 1. 查帳號
        CustomerAuth auth = customerAuthRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (auth == null) {
            loginAuditService.recordLogin(null, request.getUsername(), "失敗", "帳號或密碼錯誤", ipAddress, userAgent,
                    deviceName);
            throw new BusinessException("帳號或密碼錯誤");
        }

        // 取得客戶信箱用於發信
        CustomerProfile profile = customerProfileRepository.findById(auth.getCustomerId())
                .orElse(null);
        String email = (profile != null) ? profile.getEmail() : null;

        // 2. 檢查狀態
        // 只要 Auth 或 Profile 其中一方被標記為 LOCKED，皆視為鎖定狀態
        boolean isLocked = "LOCKED".equals(auth.getStatus())
                || (profile != null && "LOCKED".equals(profile.getStatus()));

        if (isLocked) {
            loginAuditService.recordLogin(auth.getCustomerId(), auth.getUsername(),
                    "失敗", "帳號已鎖定", ipAddress, userAgent, deviceName);
            loginRiskClient.recordLoginEvent(auth.getCustomerId(), ipAddress, "FAILURE", "帳號已鎖定");
            throw new BusinessException("帳號已暫時鎖定，請聯繫客服解鎖");
        }

        if ("PENDING".equals(auth.getStatus())) {
            loginAuditService.recordLogin(auth.getCustomerId(), auth.getUsername(), "失敗", "帳號尚未驗證", ipAddress,
                    userAgent, deviceName);
            throw new BusinessException("帳號尚未驗證，請先至信箱收取驗證信");
        }
        if (!"ACTIVE".equals(auth.getStatus())
                || profile == null
                || !"ACTIVE".equals(profile.getStatus())) {
            loginAuditService.recordLogin(auth.getCustomerId(), auth.getUsername(), "失敗", "帳號已被停用", ipAddress,
                    userAgent, deviceName);
            loginRiskClient.recordLoginEvent(auth.getCustomerId(), ipAddress, "FAILURE", "帳號已被停用");
            throw new BusinessException("此帳號已被停用");
        }

        // 3. 驗密碼
        if (!passwordEncoder.matches(request.getPassword(), auth.getPasswordHash())) {
            handleLoginFailure(auth, profile, email, location, "帳號或密碼錯誤", ipAddress, userAgent, deviceName, "帳號或密碼錯誤");
        }

        // 4. 驗證身分證字號
        if (request.getIdNumber() != null && !request.getIdNumber().isEmpty()) {
            String loginIdNumber = TaiwanIdValidator.normalize(request.getIdNumber());
            if (!profile.getIdNumber().equals(loginIdNumber)) {
                handleLoginFailure(auth, profile, email, location, "身分證字號不正確", ipAddress, userAgent, deviceName,
                        "身分證字號不正確");
            }
        }

        // 5. 更新最後登入時間與授權裝置
        auth.setLastLoginDate(LocalDateTime.now());
        customerAuthRepository.save(auth);
        upsertDevice(auth.getCustomerId(), ipAddress, userAgent);
        loginAuditService.recordLogin(auth.getCustomerId(), auth.getUsername(), "成功", null, ipAddress, userAgent,
                deviceName);
        loginRiskClient.recordLoginEvent(auth.getCustomerId(), ipAddress, "SUCCESS", "登入成功");

        // 6. 產生 JWT
        String token = jwtUtil.generateToken(auth.getUsername(), "CUSTOMER", auth.getCustomerId());

        // 7. 發送登入成功通知
        if (email != null) {
            emailService.sendLoginNotification(email, auth.getUsername(), true, location);
        }

        CustomerRespository.LoginResponse response = new CustomerRespository.LoginResponse();
        response.setToken(token);
        response.setCustomerId(auth.getCustomerId());
        response.setCif(profile != null ? profile.getCif() : "");
        response.setName(profile != null ? profile.getName() : "");
        response.setUsername(auth.getUsername());
        response.setAvatarUrl(profile != null ? profile.getAvatarUrl() : null);
        response.setRole("CUSTOMER");
        return response;
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        CustomerAuth auth = customerAuthRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BusinessException("無效的驗證連結"));

        auth.setStatus("ACTIVE");
        auth.setVerificationToken(null);
        customerAuthRepository.save(auth);
    }

    // ===========================
    // 取得個人資料
    // ===========================
    @Override
    public CustomerRespository.CustomerResponse getProfile(String customerId) {
        CustomerProfile profile = customerProfileRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("查無此客戶"));
        CustomerAuth auth = customerAuthRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new BusinessException("認證資料異常"));

        return convertToResponse(profile, auth);
    }

    // ===========================
    // 修改個人資料
    // ===========================
    @Override
    @Transactional
    public CustomerRespository.CustomerResponse updateProfile(String customerId,
            CustomerRespository.ProfileUpdateRequest request) {
        CustomerProfile profile = customerProfileRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("查無此客戶"));

        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            if (!isValidTaiwanMobile(phone)) {
                throw new BusinessException("手機號碼格式不正確，請輸入 09 開頭共 10 碼");
            }
            profile.setPhone(phone);
        }
        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (!isValidEmail(email)) {
                throw new BusinessException("電子信箱格式不正確");
            }
            profile.setEmail(email);
        }
        if (request.getAddress() != null) {
            profile.setAddress(request.getAddress());
            profile.setCurrentAddress(request.getAddress());
        }
        if (request.getJob() != null) {
            profile.setJob(request.getJob());
        }
        if (request.getOccupation() != null) {
            profile.setOccupation(request.getOccupation());
        }
        if (request.getEmployer() != null) {
            profile.setEmployer(request.getEmployer());
        }
        if (request.getFundSource() != null) {
            profile.setFundSource(request.getFundSource());
        }
        if (request.getAnnualIncome() != null) {
            profile.setAnnualIncome(request.getAnnualIncome());
        }

        customerProfileRepository.save(profile);
        CustomerAuth auth = customerAuthRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new BusinessException("認證資料異常"));

        log.info("[CustomerService] 檢查信用資料是否存在 customerId={} exists={}",
                customerId, customerCreditRepository.existsById(customerId));

        // 若信用資料已存在，同步觸發重新評分
        if (customerCreditRepository.existsById(customerId)) {
            log.info("[CustomerService] 客戶資料已更新，觸發信用評分重算 customerId={}", customerId);
            creditScoreService.syncAndRescore(
                    customerId,
                    Occupation.fromString(profile.getOccupation()),
                    BigDecimal.valueOf(profile.getAnnualIncome()* 10000L),
                    profile.getFundSource()!= null ? mapChineseToEnum(profile.getFundSource()): null,
                    profile.getIsPep()
            );
        } else {
            log.debug("[CustomerService] 客戶 {} 尚無信用資料，跳過重新評分", customerId);
        }


        return convertToResponse(profile, auth);
    }

    // ===========================
    // 上傳大頭照
    // ===========================
    @Override
    @Transactional
    public CustomerRespository.CustomerResponse uploadAvatar(String customerId, String avatarUrl) {
        CustomerProfile profile = customerProfileRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("查無此客戶"));

        profile.setAvatarUrl(avatarUrl);
        customerProfileRepository.save(profile);

        CustomerAuth auth = customerAuthRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new BusinessException("認證資料異常"));
        return convertToResponse(profile, auth);
    }

    // ===========================
    // 請求密碼重設（產生 token + 印出連結）
    // ===========================
    @Override
    @Transactional
    public void requestPasswordReset(CustomerRespository.PasswordResetEmailRequest request) {
        // 用 email, idNumber, birthday 找到 customer_profile
        CustomerProfile profile = customerProfileRepository.findAll().stream()
                .filter(p -> request.getEmail().equals(p.getEmail())
                        && request.getIdNumber().equals(p.getIdNumber())
                        && request.getBirthday().equals(p.getBirthday()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("驗證失敗：查無此身分資料或信箱錯誤"));

        CustomerAuth auth = customerAuthRepository.findByCustomerId(profile.getCustomerId())
                .orElseThrow(() -> new BusinessException("認證資料異常"));

        // 產生 reset token（UUID），效期 30 分鐘
        String resetToken = UUID.randomUUID().toString();
        auth.setResetToken(resetToken);
        auth.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
        customerAuthRepository.save(auth);
        loginAuditService.recordLogin(
                profile.getCustomerId(),
                auth.getUsername(),
                "成功",
                "密碼重設連結已發送",
                null,
                "Password reset request",
                "安全中心 / 密碼重設");

        // 組成重設連結
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        emailService.sendPasswordResetEmail(profile.getEmail(), resetLink);
    }

    // ===========================
    // 執行密碼重設
    // ===========================
    @Override
    @Transactional
    public void resetPassword(CustomerRespository.PasswordResetRequest request) {
        CustomerAuth auth = customerAuthRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new BusinessException("無效的重設連結"));

        if (auth.getResetTokenExpiry() == null || auth.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessException("重設連結已過期，請重新申請");
        }

        auth.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        auth.setResetToken(null);
        auth.setResetTokenExpiry(null);
        customerAuthRepository.save(auth);
        loginAuditService.recordLogin(
                auth.getCustomerId(),
                auth.getUsername(),
                "成功",
                "密碼已完成變更",
                null,
                "Password reset completed",
                "安全中心 / 密碼重設");
    }

    // ===========================
    // 一鍵帶入客戶認證測試資料
    // ===========================
    @Override
    @Transactional
    public void seedAuthTestData() {
        // 為已有的 seed 客戶資料建立對應的 customer_auth
        String[][] data = {
                { "X7K9P2M4", "mingwang85" },
                { "V4L6T1Y8", "hualin90" },
                { "D3H8F5G2", "chienchen78" },
                { "B9W1C7R5", "yachang95" },
                { "P6M4N2Q8", "chihlee82" },
        };

        for (String[] d : data) {
            if (customerAuthRepository.findByCustomerId(d[0]).isEmpty()) {
                CustomerAuth auth = new CustomerAuth();
                auth.setAuthId(generateAlphanumeric(10));
                auth.setCustomerId(d[0]);
                auth.setUsername(d[1]);
                auth.setPasswordHash(passwordEncoder.encode("123456"));
                auth.setRole("CUSTOMER");
                auth.setStatus("ACTIVE");
                customerAuthRepository.save(auth);
            }
        }
    }

    // ===========================
    // 私有方法
    // ===========================
    private void handleLoginFailure(CustomerAuth auth, CustomerProfile profile, String email, String location,
            String failReason,
            String ipAddress, String userAgent, String deviceName, String userErrorMessage) {
        // 取得最近 30 分鐘內的失敗次數 (不含本次)
        int recentFailures = customerLoginLogRepository.countRecentFailures(
                auth.getCustomerId(),
                LocalDateTime.now().minusMinutes(30));

        // 紀錄本次失敗
        loginAuditService.recordLogin(auth.getCustomerId(), auth.getUsername(), "失敗", failReason, ipAddress, userAgent,
                deviceName);

        // 如果加上本次是第 5 次 (recentFailures 原本為 4)
        if (recentFailures >= 4) {
            auth.setStatus("LOCKED");
            customerAuthRepository.save(auth);

            // 同步更新 Profile 狀態，確保管理後台能看到鎖定狀態
            if (profile != null) {
                profile.setStatus("LOCKED");
                customerProfileRepository.save(profile);
            }

            loginRiskClient.recordLoginEvent(auth.getCustomerId(), ipAddress, "LOCK", "觸發多次失敗鎖定");
            if (email != null) {
                emailService.sendAccountLockedNotification(email, auth.getUsername(), location);
            }
            throw new BusinessException("登入失敗次數過多，帳號已暫時鎖定，請聯繫客服解鎖");
        } else if (recentFailures >= 2) {
            // 第 3 次失敗開始發送警示 (recentFailures=2 代表前面 2 次失敗，本次第 3 次)
            loginRiskClient.recordLoginEvent(auth.getCustomerId(), ipAddress, "WARNING", "登入三次失敗警告");
            if (email != null) {
                emailService.sendLoginNotification(email, auth.getUsername(), false, location);
            }
        }
        throw new BusinessException(userErrorMessage);
    }

    private FundSource mapChineseToEnum(String label) {
        if (label == null || label.trim().isEmpty()) {
            return null;
        }

        switch (label.trim()) {
            case "薪資":
                return FundSource.SALARY;
            case "經營事業收入":
                return FundSource.BUSINESS_INCOME;
            case "退休(職)金":
                return FundSource.RETIREMENT;
            case "遺產繼承(含贈與)":
                return FundSource.INHERITANCE;
            case "理財投資":
            case "買賣房地產":  // 外部映射：歸類到投資
            case "租金收入":    // 外部映射：歸類到投資
                return FundSource.INVESTMENT;
            case "其他":
                return FundSource.OTHER;
            default:
                // 容錯機制：如果前端傳來的是原始的 Enum 字串（如 "SALARY"），依然能正常解析
                try {
                    return FundSource.valueOf(label);
                } catch (IllegalArgumentException e) {
                    return FundSource.OTHER; // 真的找不到就給其他
                }
        }
    }

    private void upsertDevice(String customerId, String ipAddress, String userAgent) {
        String fingerprint = fingerprint(customerId, userAgent);
        String browserName = resolveBrowserName(userAgent);
        String operatingSystem = resolveOperatingSystem(userAgent);
        String deviceName = browserName + " / " + operatingSystem;

        CustomerDevice device = customerDeviceRepository.findByCustomerIdAndDeviceFingerprint(customerId, fingerprint)
                .orElseGet(CustomerDevice::new);
        device.setCustomerId(customerId);
        device.setDeviceFingerprint(fingerprint);
        device.setDeviceName(deviceName);
        device.setBrowserName(browserName);
        device.setOperatingSystem(operatingSystem);
        device.setIpAddress(truncate(ipAddress, 45));
        device.setUserAgent(truncate(userAgent, 512));
        device.setStatus("ACTIVE");
        device.setTrusted(true);
        device.setRevokedAt(null);
        device.setLastSeenAt(LocalDateTime.now());
        customerDeviceRepository.save(device);
    }

    private String fingerprint(String customerId, String userAgent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((customerId + "::" + (userAgent == null ? "" : userAgent))
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new BusinessException("裝置識別失敗");
        }
    }

    private String resolveDeviceName(String userAgent) {
        return resolveBrowserName(userAgent) + " / " + resolveOperatingSystem(userAgent);
    }

    private String resolveBrowserName(String userAgent) {
        String ua = userAgent == null ? "" : userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("edg/"))
            return "Microsoft Edge";
        if (ua.contains("chrome/") || ua.contains("crios/"))
            return "Chrome";
        if (ua.contains("firefox/") || ua.contains("fxios/"))
            return "Firefox";
        if (ua.contains("safari/"))
            return "Safari";
        return "未知瀏覽器";
    }

    private String resolveOperatingSystem(String userAgent) {
        String ua = userAgent == null ? "" : userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("windows"))
            return "Windows";
        if (ua.contains("mac os") || ua.contains("macintosh"))
            return "macOS";
        if (ua.contains("iphone") || ua.contains("ipad"))
            return "iOS";
        if (ua.contains("android"))
            return "Android";
        if (ua.contains("linux"))
            return "Linux";
        return "未知系統";
    }

    private String truncate(String value, int maxLength) {
        if (value == null)
            return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private boolean isValidTaiwanMobile(String phone) {
        return phone != null && phone.matches(TAIWAN_MOBILE_PATTERN);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches(EMAIL_PATTERN);
    }

    private boolean isValidCustomerName(String name) {
        return name != null && name.trim().matches(CUSTOMER_NAME_PATTERN);
    }

    private CustomerRespository.CustomerResponse convertToResponse(CustomerProfile profile, CustomerAuth auth) {
        CustomerRespository.CustomerResponse res = new CustomerRespository.CustomerResponse();
        org.springframework.beans.BeanUtils.copyProperties(profile, res);
        res.setUsername(auth.getUsername());
        return res;
    }

    private String generateAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(ALPHANUMERIC_CHARS.length());
            sb.append(ALPHANUMERIC_CHARS.charAt(randomIndex));
        }
        return sb.toString();
    }
}

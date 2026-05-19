package com.javaeasybank.customer.service;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.common.service.EmailService;
import com.javaeasybank.common.util.JwtUtil;
import com.javaeasybank.customer.LoginRiskClient;
import com.javaeasybank.customer.entity.CustomerAuth;
import com.javaeasybank.customer.entity.CustomerDevice;
import com.javaeasybank.customer.entity.CustomerProfile;
import com.javaeasybank.customer.repository.CustomerAuthRepository;
import com.javaeasybank.customer.repository.CustomerDeviceRepository;
import com.javaeasybank.customer.repository.CustomerLoginLogRepository;
import com.javaeasybank.customer.repository.CustomerProfileRepository;
import com.javaeasybank.customer.repository.CustomerRespository;
import com.javaeasybank.notification.enums.NotificationType;
import com.javaeasybank.notification.service.NotificationService;
import com.javaeasybank.risk.repository.CustomerCreditRepository;
import com.javaeasybank.risk.service.CreditScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAuthServiceImplTest {

    @Mock
    private CustomerAuthRepository customerAuthRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private CustomerLoginLogRepository customerLoginLogRepository;

    @Mock
    private CustomerDeviceRepository customerDeviceRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private LoginRiskClient loginRiskClient;

    @Mock
    private LoginAuditService loginAuditService;

    @Mock
    private CreditScoreService creditScoreService;

    @Mock
    private CustomerCreditRepository customerCreditRepository;

    private CustomerAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CustomerAuthServiceImpl(
                customerAuthRepository,
                customerProfileRepository,
                customerLoginLogRepository,
                customerDeviceRepository,
                passwordEncoder,
                jwtUtil,
                emailService,
                notificationService,
                loginRiskClient,
                loginAuditService,
                creditScoreService,
                customerCreditRepository);
    }

    @Test
    @DisplayName("register 會建立客戶認證資料並同步寄出驗證信與安全通知")
    void register_sendsVerificationEmailAndNotification() {
        CustomerRespository.RegisterRequest request = new CustomerRespository.RegisterRequest();
        request.setName("王小明");
        request.setBirthday(LocalDate.of(1990, 1, 1));
        request.setGender("M");
        request.setIdNumber("A123456789");
        request.setUsername("mingwang");
        request.setPassword("secret123");
        request.setAddress("台北市信義區信義路五段7號");
        request.setPhone("0912345678");
        request.setEmail("ming@example.com");

        when(customerAuthRepository.existsByUsername("mingwang")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");

        var response = service.register(request);

        ArgumentCaptor<CustomerAuth> authCaptor = ArgumentCaptor.forClass(CustomerAuth.class);
        verify(customerAuthRepository).save(authCaptor.capture());
        CustomerAuth savedAuth = authCaptor.getValue();

        assertEquals("mingwang", savedAuth.getUsername());
        assertEquals("encoded-password", savedAuth.getPasswordHash());
        assertEquals("PENDING", savedAuth.getStatus());
        assertNotNull(savedAuth.getVerificationToken());
        assertEquals(savedAuth.getCustomerId(), response.getCustomerId());

        verify(emailService).sendVerificationEmail(eq("ming@example.com"), eq(savedAuth.getVerificationToken()));
        verify(notificationService).createNotification(
                eq(savedAuth.getCustomerId()),
                eq(NotificationType.SECURITY),
                eq("電子郵件驗證"),
                eq("請點擊驗證信完成帳號驗證。"),
                eq("/login"));
    }

    @Test
    @DisplayName("login 成功會更新登入狀態、記錄裝置並建立登入通知")
    void login_successSendsSecurityNotification() {
        CustomerAuth auth = activeAuth();
        CustomerProfile profile = activeProfile();
        CustomerRespository.LoginRequest request = loginRequest("secret123");

        when(customerAuthRepository.findByUsername("mingwang")).thenReturn(Optional.of(auth));
        when(customerProfileRepository.findById("CUST0001")).thenReturn(Optional.of(profile));
        when(passwordEncoder.matches("secret123", "encoded-password")).thenReturn(true);
        when(customerAuthRepository.save(auth)).thenReturn(auth);
        when(customerDeviceRepository.findByCustomerIdAndDeviceFingerprint(eq("CUST0001"), anyString()))
                .thenReturn(Optional.empty());
        when(customerDeviceRepository.save(any(CustomerDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.generateToken("mingwang", "CUSTOMER", "CUST0001")).thenReturn("jwt-token");

        var response = service.login(request, "127.0.0.1",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X) AppleWebKit/537.36 Chrome/124.0 Safari/537.36");

        assertEquals("jwt-token", response.getToken());
        assertEquals("CUST0001", response.getCustomerId());
        assertNotNull(auth.getLastLoginDate());
        verify(emailService).sendLoginNotification(eq("ming@example.com"), eq("mingwang"), eq(true), anyString());
        verify(notificationService).createNotification(
                eq("CUST0001"),
                eq(NotificationType.SECURITY),
                eq("登入通知"),
                eq("您已成功登入系統。"),
                eq("/user/security/login-records"));
    }

    @Test
    @DisplayName("login 第三次失敗會寄出登入異常提醒並建立安全通知")
    void login_thirdFailureSendsWarningNotification() {
        CustomerAuth auth = activeAuth();
        CustomerProfile profile = activeProfile();

        when(customerAuthRepository.findByUsername("mingwang")).thenReturn(Optional.of(auth));
        when(customerProfileRepository.findById("CUST0001")).thenReturn(Optional.of(profile));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);
        when(customerLoginLogRepository.countRecentFailures(eq("CUST0001"), any(), eq(null))).thenReturn(2);

        assertThrows(BusinessException.class,
                () -> service.login(loginRequest("wrong-password"), "127.0.0.1", "Chrome"));

        verify(emailService).sendLoginNotification(eq("ming@example.com"), eq("mingwang"), eq(false), anyString());
        verify(notificationService).createNotification(
                eq("CUST0001"),
                eq(NotificationType.SECURITY),
                eq("登入異常提醒"),
                eq("您的帳號登入失敗次數增加，請確認是否為本人操作。"),
                eq("/user/security/login-records"));
    }

    @Test
    @DisplayName("login 第五次失敗會鎖定 auth/profile 並建立帳號鎖定通知")
    void login_fifthFailureLocksAccountAndSendsNotification() {
        CustomerAuth auth = activeAuth();
        CustomerProfile profile = activeProfile();

        when(customerAuthRepository.findByUsername("mingwang")).thenReturn(Optional.of(auth));
        when(customerProfileRepository.findById("CUST0001")).thenReturn(Optional.of(profile));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);
        when(customerLoginLogRepository.countRecentFailures(eq("CUST0001"), any(), eq(null))).thenReturn(4);

        assertThrows(BusinessException.class,
                () -> service.login(loginRequest("wrong-password"), "127.0.0.1", "Chrome"));

        assertEquals("LOCKED", auth.getStatus());
        assertEquals("LOCKED", profile.getStatus());
        verify(customerAuthRepository).save(auth);
        verify(customerProfileRepository).save(profile);
        verify(emailService).sendAccountLockedNotification(eq("ming@example.com"), eq("mingwang"), anyString());
        verify(notificationService).createNotification(
                eq("CUST0001"),
                eq(NotificationType.SECURITY),
                eq("帳號已鎖定"),
                eq("登入失敗次數過多，帳號已暫時鎖定。"),
                eq("/login"));
    }

    @Test
    @DisplayName("unlockCustomer 會啟用 auth/profile 並記錄解鎖時間")
    void unlockCustomer_resetsAuthAndProfileStatus() {
        CustomerAuth auth = activeAuth();
        auth.setStatus("LOCKED");
        CustomerProfile profile = activeProfile();
        profile.setStatus("LOCKED");

        when(customerAuthRepository.findByCustomerId("CUST0001")).thenReturn(Optional.of(auth));
        when(customerProfileRepository.findById("CUST0001")).thenReturn(Optional.of(profile));

        service.unlockCustomer("CUST0001");

        assertEquals("ACTIVE", auth.getStatus());
        assertNotNull(auth.getUnlockedAt());
        assertEquals("ACTIVE", profile.getStatus());
        verify(customerAuthRepository).save(auth);
        verify(customerProfileRepository).save(profile);
    }

    private CustomerAuth activeAuth() {
        CustomerAuth auth = new CustomerAuth();
        auth.setAuthId("AUTH0001");
        auth.setCustomerId("CUST0001");
        auth.setUsername("mingwang");
        auth.setPasswordHash("encoded-password");
        auth.setRole("CUSTOMER");
        auth.setStatus("ACTIVE");
        return auth;
    }

    private CustomerProfile activeProfile() {
        CustomerProfile profile = new CustomerProfile();
        profile.setCustomerId("CUST0001");
        profile.setCif("2605-ABCDEFGH");
        profile.setIdNumber("A123456789");
        profile.setName("王小明");
        profile.setBirthday(LocalDate.of(1990, 1, 1));
        profile.setGender("M");
        profile.setEmail("ming@example.com");
        profile.setPhone("0912345678");
        profile.setAddress("台北市信義區信義路五段7號");
        profile.setStatus("ACTIVE");
        return profile;
    }

    private CustomerRespository.LoginRequest loginRequest(String password) {
        CustomerRespository.LoginRequest request = new CustomerRespository.LoginRequest();
        request.setUsername("mingwang");
        request.setPassword(password);
        request.setIdNumber("A123456789");
        return request;
    }
}

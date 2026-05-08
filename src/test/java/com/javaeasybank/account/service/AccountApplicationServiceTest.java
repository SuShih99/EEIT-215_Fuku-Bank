package com.javaeasybank.account.service;

import com.javaeasybank.account.dto.request.AccountApplicationRequest;
import com.javaeasybank.account.dto.response.AccountApplicationResponse;
import com.javaeasybank.account.entity.Account;
import com.javaeasybank.account.entity.AccountApplication;
import com.javaeasybank.account.enums.*;
import com.javaeasybank.account.repository.AccountApplicationRepository;
import com.javaeasybank.account.repository.AccountRepository;
import com.javaeasybank.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountApplicationServiceTest {

    @Mock
    private AccountApplicationRepository applicationRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountApplicationService service;

    private AccountApplicationRequest baseRequest;

    @BeforeEach
    void setUp() {
        baseRequest = new AccountApplicationRequest();
        baseRequest.setAccountType(AccountType.CHECKING);
        baseRequest.setCustomerName("王小明");
        baseRequest.setIdNumber("A123456789");
        baseRequest.setBirthday(LocalDate.of(1990, 1, 15));
        baseRequest.setNationality("TW");
        baseRequest.setPhone("0912345678");
        baseRequest.setRegisteredAddress("台北市中正區重慶南路一段1號");
        baseRequest.setCurrentAddress("台北市中正區重慶南路一段1號");
        baseRequest.setOccupation("軟體工程師");
        baseRequest.setEmployer("ABC 科技公司");
        baseRequest.setEstimatedMonthlyTx(50);
        baseRequest.setAccountPurpose(AccountPurpose.SALARY);
        baseRequest.setFundSource(FundSource.SALARY);
        baseRequest.setIsPep(false);
    }

    // =========================================================
    // submit()
    // =========================================================

    @Nested
    @DisplayName("submit() 提交申請")
    class SubmitTests {

        @Test
        @DisplayName("正常提交 — 風險標記 NORMAL，狀態 PENDING")
        void submit_normal_success() {
            // given
            String customerId = "C001";
            String ip = "192.168.1.1";

            when(applicationRepository.existsByCustomerIdAndStatus(customerId, ApplicationStatus.PENDING))
                    .thenReturn(false);
            when(applicationRepository.countByApplyIpAndCreatedAtAfter(eq(ip), any(LocalDateTime.class)))
                    .thenReturn(0L);
            when(applicationRepository.countByPhoneAndCreatedAtAfter(eq("0912345678"), any(LocalDateTime.class)))
                    .thenReturn(0L);
            when(applicationRepository.save(any(AccountApplication.class)))
                    .thenAnswer(inv -> {
                        AccountApplication app = inv.getArgument(0);
                        app.setId(1L);
                        app.setCreatedAt(LocalDateTime.now());
                        app.setUpdatedAt(LocalDateTime.now());
                        return app;
                    });

            // when
            AccountApplicationResponse result = service.submit(
                    customerId, baseRequest, "/uploads/id-cards/front.jpg",
                    "/uploads/id-cards/back.jpg", "/uploads/id-cards/second.jpg", ip);

            // then
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(ApplicationStatus.PENDING, result.getStatus());
            assertEquals(RiskFlag.NORMAL, result.getRiskFlag());
            assertEquals(AccountType.CHECKING, result.getAccountType());
            assertEquals(Currency.TWD, result.getCurrency()); // 預設 TWD

            // 驗證 save 被呼叫一次
            verify(applicationRepository, times(1)).save(any(AccountApplication.class));
        }

        @Test
        @DisplayName("重複申請 — 已有 PENDING 申請時拋出例外")
        void submit_duplicatePending_throwsException() {
            // given
            when(applicationRepository.existsByCustomerIdAndStatus("C001", ApplicationStatus.PENDING))
                    .thenReturn(true);

            // when & then
            BusinessException ex = assertThrows(BusinessException.class, () ->
                    service.submit("C001", baseRequest, "f.jpg", "b.jpg", "s.jpg", "1.1.1.1"));

            assertTrue(ex.getMessage().contains("審核中"));
            verify(applicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("PEP 標記 — isPep=true 時 riskFlag=PEP")
        void submit_pep_flagged() {
            // given
            baseRequest.setIsPep(true);
            when(applicationRepository.existsByCustomerIdAndStatus(anyString(), any())).thenReturn(false);
            when(applicationRepository.countByApplyIpAndCreatedAtAfter(anyString(), any())).thenReturn(0L);
            when(applicationRepository.countByPhoneAndCreatedAtAfter(anyString(), any())).thenReturn(0L);
            when(applicationRepository.save(any(AccountApplication.class))).thenAnswer(inv -> {
                AccountApplication app = inv.getArgument(0);
                app.setId(2L);
                app.setCreatedAt(LocalDateTime.now());
                app.setUpdatedAt(LocalDateTime.now());
                return app;
            });

            // when
            AccountApplicationResponse result = service.submit(
                    "C002", baseRequest, "f.jpg", "b.jpg", "s.jpg", "10.0.0.1");

            // then
            assertEquals(RiskFlag.PEP, result.getRiskFlag());
        }

        @Test
        @DisplayName("高頻防堵 — IP 24h 內超過 3 次，標記 HIGH_FREQUENCY")
        void submit_highFrequencyIp_flagged() {
            // given
            when(applicationRepository.existsByCustomerIdAndStatus(anyString(), any())).thenReturn(false);
            when(applicationRepository.countByApplyIpAndCreatedAtAfter(eq("1.2.3.4"), any()))
                    .thenReturn(3L); // 已達上限
            when(applicationRepository.countByPhoneAndCreatedAtAfter(anyString(), any())).thenReturn(0L);
            when(applicationRepository.save(any(AccountApplication.class))).thenAnswer(inv -> {
                AccountApplication app = inv.getArgument(0);
                app.setId(3L);
                app.setCreatedAt(LocalDateTime.now());
                app.setUpdatedAt(LocalDateTime.now());
                return app;
            });

            // when
            AccountApplicationResponse result = service.submit(
                    "C003", baseRequest, "f.jpg", "b.jpg", "s.jpg", "1.2.3.4");

            // then
            assertEquals(RiskFlag.HIGH_FREQUENCY, result.getRiskFlag());
        }

        @Test
        @DisplayName("高頻防堵 — 手機 24h 內超過 3 次，標記 HIGH_FREQUENCY")
        void submit_highFrequencyPhone_flagged() {
            // given
            when(applicationRepository.existsByCustomerIdAndStatus(anyString(), any())).thenReturn(false);
            when(applicationRepository.countByApplyIpAndCreatedAtAfter(anyString(), any())).thenReturn(0L);
            when(applicationRepository.countByPhoneAndCreatedAtAfter(eq("0912345678"), any()))
                    .thenReturn(5L); // 超過上限
            when(applicationRepository.save(any(AccountApplication.class))).thenAnswer(inv -> {
                AccountApplication app = inv.getArgument(0);
                app.setId(4L);
                app.setCreatedAt(LocalDateTime.now());
                app.setUpdatedAt(LocalDateTime.now());
                return app;
            });

            // when
            AccountApplicationResponse result = service.submit(
                    "C004", baseRequest, "f.jpg", "b.jpg", "s.jpg", "10.0.0.2");

            // then
            assertEquals(RiskFlag.HIGH_FREQUENCY, result.getRiskFlag());
        }

        @Test
        @DisplayName("雙重風險 — PEP + 高頻，標記 PEP_HIGH_FREQUENCY")
        void submit_pepAndHighFreq_dualFlag() {
            // given
            baseRequest.setIsPep(true);
            when(applicationRepository.existsByCustomerIdAndStatus(anyString(), any())).thenReturn(false);
            when(applicationRepository.countByApplyIpAndCreatedAtAfter(anyString(), any())).thenReturn(5L);
            when(applicationRepository.countByPhoneAndCreatedAtAfter(anyString(), any())).thenReturn(0L);
            when(applicationRepository.save(any(AccountApplication.class))).thenAnswer(inv -> {
                AccountApplication app = inv.getArgument(0);
                app.setId(5L);
                app.setCreatedAt(LocalDateTime.now());
                app.setUpdatedAt(LocalDateTime.now());
                return app;
            });

            // when
            AccountApplicationResponse result = service.submit(
                    "C005", baseRequest, "f.jpg", "b.jpg", "s.jpg", "1.2.3.4");

            // then
            assertEquals(RiskFlag.PEP_HIGH_FREQUENCY, result.getRiskFlag());
        }

        @Test
        @DisplayName("幣別預設 — 未指定 currency 時預設 TWD")
        void submit_defaultCurrencyTWD() {
            // given
            baseRequest.setCurrency(null);
            when(applicationRepository.existsByCustomerIdAndStatus(anyString(), any())).thenReturn(false);
            when(applicationRepository.countByApplyIpAndCreatedAtAfter(anyString(), any())).thenReturn(0L);
            when(applicationRepository.countByPhoneAndCreatedAtAfter(anyString(), any())).thenReturn(0L);

            ArgumentCaptor<AccountApplication> captor = ArgumentCaptor.forClass(AccountApplication.class);
            when(applicationRepository.save(captor.capture())).thenAnswer(inv -> {
                AccountApplication app = inv.getArgument(0);
                app.setId(6L);
                app.setCreatedAt(LocalDateTime.now());
                app.setUpdatedAt(LocalDateTime.now());
                return app;
            });

            // when
            service.submit("C006", baseRequest, "f.jpg", "b.jpg", "s.jpg", "10.0.0.1");

            // then
            assertEquals(Currency.TWD, captor.getValue().getCurrency());
        }

        @Test
        @DisplayName("外幣帳戶 — 指定 currency=USD 時保留")
        void submit_foreignCurrency_preserved() {
            // given
            baseRequest.setAccountType(AccountType.TIME_DEPOSIT);
            baseRequest.setCurrency(Currency.USD);
            when(applicationRepository.existsByCustomerIdAndStatus(anyString(), any())).thenReturn(false);
            when(applicationRepository.countByApplyIpAndCreatedAtAfter(anyString(), any())).thenReturn(0L);
            when(applicationRepository.countByPhoneAndCreatedAtAfter(anyString(), any())).thenReturn(0L);

            ArgumentCaptor<AccountApplication> captor = ArgumentCaptor.forClass(AccountApplication.class);
            when(applicationRepository.save(captor.capture())).thenAnswer(inv -> {
                AccountApplication app = inv.getArgument(0);
                app.setId(7L);
                app.setCreatedAt(LocalDateTime.now());
                app.setUpdatedAt(LocalDateTime.now());
                return app;
            });

            // when
            service.submit("C007", baseRequest, "f.jpg", "b.jpg", "s.jpg", "10.0.0.1");

            // then
            assertEquals(Currency.USD, captor.getValue().getCurrency());
            assertEquals(AccountType.TIME_DEPOSIT, captor.getValue().getAccountType());
        }
    }

    // =========================================================
    // approve()
    // =========================================================

    @Nested
    @DisplayName("approve() 核准申請")
    class ApproveTests {

        private AccountApplication pendingApp;

        @BeforeEach
        void setUp() {
            pendingApp = new AccountApplication();
            pendingApp.setId(100L);
            pendingApp.setCustomerId("C001");
            pendingApp.setAccountType(AccountType.CHECKING);
            pendingApp.setCurrency(Currency.TWD);
            pendingApp.setName("王小明");
            pendingApp.setIdNumber("A123456789");
            pendingApp.setBirthday(LocalDate.of(1990, 1, 15));
            pendingApp.setNationality("TW");
            pendingApp.setPhone("0912345678");
            pendingApp.setRegisteredAddress("台北市");
            pendingApp.setCurrentAddress("台北市");
            pendingApp.setIdFrontUrl("front.jpg");
            pendingApp.setIdBackUrl("back.jpg");
            pendingApp.setSecondIdUrl("second.jpg");
            pendingApp.setRiskFlag(RiskFlag.NORMAL);
            pendingApp.setStatus(ApplicationStatus.PENDING);
            pendingApp.setCreatedAt(LocalDateTime.now().minusDays(1));
            pendingApp.setUpdatedAt(LocalDateTime.now().minusDays(1));
        }

        @Test
        @DisplayName("核准成功 — 自動建立帳戶，狀態變 APPROVED")
        void approve_success() {
            // given
            when(applicationRepository.findById(100L)).thenReturn(Optional.of(pendingApp));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            when(applicationRepository.save(any(AccountApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            AccountApplicationResponse result = service.approve(100L, "admin01");

            // then
            assertEquals(ApplicationStatus.APPROVED, result.getStatus());
            assertEquals("admin01", result.getReviewedBy());
            assertNotNull(result.getReviewedAt());

            // 驗證 Account 被建立
            ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(accountCaptor.capture());
            Account created = accountCaptor.getValue();
            assertEquals("C001", created.getCustomerId());
            assertEquals(AccountType.CHECKING, created.getAccountType());
            assertEquals(Currency.TWD, created.getCurrency());
            assertEquals(AccountStatus.ACTIVE, created.getStatus());
            assertNotNull(created.getAccountNumber());
            assertEquals(12, created.getAccountNumber().length());
        }

        @Test
        @DisplayName("核准活存帳戶 — 初始餘額 1000，利率 0.15%")
        void approve_checking_initialBalance() {
            // given
            when(applicationRepository.findById(100L)).thenReturn(Optional.of(pendingApp));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            when(applicationRepository.save(any(AccountApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            service.approve(100L, "admin01");

            // then
            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            Account acct = captor.getValue();
            assertNotNull(acct.getBalance());
            assertEquals(0, acct.getBalance().compareTo(new java.math.BigDecimal("1000")));
            assertNotNull(acct.getInterestRate());
            assertEquals(0, acct.getInterestRate().compareTo(new java.math.BigDecimal("0.0015")));
        }

        @Test
        @DisplayName("核准定存帳戶 — 不主動設定餘額（維持預設值）")
        void approve_timeDeposit_noInitialBalance() {
            // given
            pendingApp.setAccountType(AccountType.TIME_DEPOSIT);
            when(applicationRepository.findById(100L)).thenReturn(Optional.of(pendingApp));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            when(applicationRepository.save(any(AccountApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            service.approve(100L, "admin01");

            // then — 定存不像活存會設 1000 初始餘額，餘額應為 0 或 null
            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            Account acct = captor.getValue();
            assertTrue(acct.getBalance() == null
                    || acct.getBalance().compareTo(java.math.BigDecimal.ZERO) == 0);
        }

        @Test
        @DisplayName("非 PENDING 狀態不可核准")
        void approve_nonPending_throwsException() {
            // given
            pendingApp.setStatus(ApplicationStatus.REJECTED);
            when(applicationRepository.findById(100L)).thenReturn(Optional.of(pendingApp));

            // when & then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.approve(100L, "admin01"));

            assertTrue(ex.getMessage().contains("無法核准"));
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("申請不存在時拋出例外")
        void approve_notFound_throwsException() {
            // given
            when(applicationRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThrows(BusinessException.class, () -> service.approve(999L, "admin01"));
        }
    }

    // =========================================================
    // reject()
    // =========================================================

    @Nested
    @DisplayName("reject() 駁回申請")
    class RejectTests {

        @Test
        @DisplayName("駁回成功 — 狀態變 REJECTED，寫入原因")
        void reject_success() {
            // given
            AccountApplication app = new AccountApplication();
            app.setId(200L);
            app.setCustomerId("C010");
            app.setAccountType(AccountType.CHECKING);
            app.setCurrency(Currency.TWD);
            app.setName("李小華");
            app.setIdNumber("B987654321");
            app.setBirthday(LocalDate.of(1985, 6, 20));
            app.setNationality("TW");
            app.setPhone("0987654321");
            app.setRegisteredAddress("高雄市");
            app.setCurrentAddress("高雄市");
            app.setIdFrontUrl("f.jpg");
            app.setIdBackUrl("b.jpg");
            app.setSecondIdUrl("s.jpg");
            app.setRiskFlag(RiskFlag.NORMAL);
            app.setStatus(ApplicationStatus.PENDING);
            app.setCreatedAt(LocalDateTime.now());
            app.setUpdatedAt(LocalDateTime.now());

            when(applicationRepository.findById(200L)).thenReturn(Optional.of(app));
            when(applicationRepository.save(any(AccountApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            AccountApplicationResponse result = service.reject(200L, "證件模糊無法辨識", "admin02");

            // then
            assertEquals(ApplicationStatus.REJECTED, result.getStatus());
            assertEquals("證件模糊無法辨識", result.getRejectReason());
            assertEquals("admin02", result.getReviewedBy());
            assertNotNull(result.getReviewedAt());

            // 不應建立帳戶
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("非 PENDING 狀態不可駁回")
        void reject_nonPending_throwsException() {
            // given
            AccountApplication app = new AccountApplication();
            app.setId(201L);
            app.setStatus(ApplicationStatus.APPROVED);
            app.setCreatedAt(LocalDateTime.now());
            app.setUpdatedAt(LocalDateTime.now());

            when(applicationRepository.findById(201L)).thenReturn(Optional.of(app));

            // when & then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.reject(201L, "reason", "admin02"));

            assertTrue(ex.getMessage().contains("無法駁回"));
            verify(applicationRepository, never()).save(any());
        }
    }

    // =========================================================
    // getMyApplications()
    // =========================================================

    @Nested
    @DisplayName("getMyApplications() 查詢我的申請")
    class GetMyApplicationsTests {

        @Test
        @DisplayName("回傳客戶所有申請，身分證遮蔽")
        void getMyApplications_returnsMaskedData() {
            // given
            AccountApplication app1 = new AccountApplication();
            app1.setId(301L);
            app1.setCustomerId("C020");
            app1.setAccountType(AccountType.CHECKING);
            app1.setCurrency(Currency.TWD);
            app1.setName("陳大明");
            app1.setIdNumber("A123456789");
            app1.setPhone("0912345678");
            app1.setBirthday(LocalDate.of(1990, 1, 1));
            app1.setNationality("TW");
            app1.setRegisteredAddress("台北");
            app1.setCurrentAddress("台北");
            app1.setIdFrontUrl("f.jpg");
            app1.setIdBackUrl("b.jpg");
            app1.setSecondIdUrl("s.jpg");
            app1.setRiskFlag(RiskFlag.NORMAL);
            app1.setStatus(ApplicationStatus.APPROVED);
            app1.setCreatedAt(LocalDateTime.now());
            app1.setUpdatedAt(LocalDateTime.now());

            when(applicationRepository.findByCustomerIdOrderByCreatedAtDesc("C020"))
                    .thenReturn(List.of(app1));

            // when
            List<AccountApplicationResponse> result = service.getMyApplications("C020");

            // then
            assertEquals(1, result.size());
            AccountApplicationResponse resp = result.get(0);
            // 身分證遮蔽：A123456789 → A12****789
            assertEquals("A12****789", resp.getIdNumber());
            // 手機遮蔽：0912345678 → 0912***678
            assertEquals("0912***678", resp.getPhone());
        }

        @Test
        @DisplayName("無申請紀錄時回傳空 List")
        void getMyApplications_empty() {
            // given
            when(applicationRepository.findByCustomerIdOrderByCreatedAtDesc("C099"))
                    .thenReturn(List.of());

            // when
            List<AccountApplicationResponse> result = service.getMyApplications("C099");

            // then
            assertTrue(result.isEmpty());
        }
    }
}

package com.javaeasybank.loan.service;

import com.javaeasybank.loan.dto.requests.LoanContactLogRequestDTO;
import com.javaeasybank.loan.dto.requests.LoanReviewDetailRequestDTO;
import com.javaeasybank.loan.entity.LoanApplication;
import com.javaeasybank.loan.entity.LoanContactLog;
import com.javaeasybank.loan.entity.LoanReviewDetail;
import com.javaeasybank.loan.enums.LoanApplicationStatus;
import com.javaeasybank.loan.enums.LoanContactChannel;
import com.javaeasybank.loan.enums.LoanContactStatus;
import com.javaeasybank.loan.enums.LoanReviewStatus;
import com.javaeasybank.loan.repository.LoanApplicationRepository;
import com.javaeasybank.loan.repository.LoanContactLogRepository;
import com.javaeasybank.loan.repository.LoanReviewDetailRepository;
import com.javaeasybank.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    @Mock
    private LoanApplicationRepository laRepo;
    @Mock
    private LoanContactLogRepository contactLogRepo;
    @Mock
    private LoanReviewDetailRepository reviewDetailRepo;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LoanApplicationService service;

    @Test
    @DisplayName("addContactLog 使用 repository 儲存聯繫紀錄並更新主表狀態")
    void addContactLog_savesLogAndUpdatesLoanWithRepositories() {
        LoanApplication loan = new LoanApplication();
        loan.setApplicationId("LA202605141400001005");
        loan.setApplicationStatus(LoanApplicationStatus.PENDING_CONTACT);

        when(laRepo.findById("LA202605141400001005")).thenReturn(Optional.of(loan));

        LoanContactLogRequestDTO request = new LoanContactLogRequestDTO();
        request.setEmpId("E001");
        request.setContactStatus(LoanContactStatus.REACHED.name());
        request.setContactChannel(LoanContactChannel.PHONE.name());
        request.setNote("已聯繫客戶");

        service.addContactLog("LA202605141400001005", request);

        ArgumentCaptor<LoanContactLog> logCaptor = ArgumentCaptor.forClass(LoanContactLog.class);
        verify(contactLogRepo).save(logCaptor.capture());
        LoanContactLog savedLog = logCaptor.getValue();

        assertNotNull(savedLog.getLogId());
        assertEquals("LA202605141400001005", savedLog.getApplicationId());
        assertEquals("E001", savedLog.getEmpId());
        assertEquals(LoanContactStatus.REACHED, savedLog.getContactStatus());
        assertEquals(LoanContactChannel.PHONE, savedLog.getContactChannel());

        ArgumentCaptor<LoanApplication> loanCaptor = ArgumentCaptor.forClass(LoanApplication.class);
        verify(laRepo).save(loanCaptor.capture());
        LoanApplication savedLoan = loanCaptor.getValue();

        assertEquals(LoanApplicationStatus.IN_CONTACT, savedLoan.getApplicationStatus());
        assertEquals(LoanContactStatus.REACHED, savedLoan.getLatestContactStatus());
        assertNotNull(savedLoan.getLatestContactTime());
        assertNotNull(savedLoan.getUpdateTime());
    }

    @Test
    @DisplayName("saveReviewDetail 新草稿使用 repository 儲存")
    void saveReviewDetail_createsDraftWithRepository() {
        LoanApplication loan = new LoanApplication();
        loan.setApplicationId("LA202605141400001005");

        when(laRepo.findById("LA202605141400001005")).thenReturn(Optional.of(loan));
        when(reviewDetailRepo.findByApplicationId("LA202605141400001005")).thenReturn(Optional.empty());

        LoanReviewDetailRequestDTO request = new LoanReviewDetailRequestDTO();
        request.setConfirmedAmount(new BigDecimal("20000.00"));
        request.setConfirmedPeriod(12);
        request.setConfirmedRate(new BigDecimal("0.040000"));
        request.setCollateralNote("無擔保");
        request.setEmpId("E001");

        service.saveReviewDetail("LA202605141400001005", request);

        ArgumentCaptor<LoanReviewDetail> captor = ArgumentCaptor.forClass(LoanReviewDetail.class);
        verify(reviewDetailRepo).save(captor.capture());
        LoanReviewDetail savedDetail = captor.getValue();

        assertNotNull(savedDetail.getReviewId());
        assertEquals("LA202605141400001005", savedDetail.getApplicationId());
        assertEquals(LoanReviewStatus.DRAFT, savedDetail.getReviewStatus());
        assertEquals(new BigDecimal("20000.00"), savedDetail.getConfirmedAmount());
        assertEquals(12, savedDetail.getConfirmedPeriod());
        assertEquals(new BigDecimal("0.040000"), savedDetail.getConfirmedRate());
        assertEquals("E001", savedDetail.getEmpId());
        assertNotNull(savedDetail.getReviewTime());
    }
}

package com.javaeasybank.account.service;

import com.javaeasybank.account.dto.request.LoanInterestRequest;
import com.javaeasybank.account.dto.request.LoanAccountCreateRequest;
import com.javaeasybank.account.dto.response.LoanAccountResponse;
import com.javaeasybank.account.entity.Account;
import com.javaeasybank.account.entity.TransLog;
import com.javaeasybank.account.enums.AccountStatus;
import com.javaeasybank.account.enums.AccountType;
import com.javaeasybank.account.enums.Currency;
import com.javaeasybank.account.enums.EntryType;
import com.javaeasybank.account.enums.TransactionType;
import com.javaeasybank.account.repository.AccountRepository;
import com.javaeasybank.account.repository.TransLogRepository;
import com.javaeasybank.creditcard.repository.CardBillRepository;
import com.javaeasybank.creditcard.repository.CreditCardRepository;
import com.javaeasybank.customer.entity.CustomerProfile;
import com.javaeasybank.customer.repository.CustomerProfileRepository;
import com.javaeasybank.loan.repository.LoanAccountRepository;
import com.javaeasybank.loan.repository.LoanRepaymentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountIntegrationServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransLogRepository transLogRepository;
    @Mock
    private CustomerProfileRepository customerProfileRepository;
    @Mock
    private CardBillRepository cardBillRepository;
    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private LoanAccountRepository loanAccountRepository;
    @Mock
    private LoanRepaymentRepository loanRepaymentRepository;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AccountIntegrationService service;

    @Test
    @DisplayName("createLoanAccount 依身分證編碼產生 901 貸款帳號")
    void createLoanAccount_generatesLoanAccountNumberFromIdNumber() {
        CustomerProfile customer = new CustomerProfile();
        customer.setCustomerId("CUST001");
        customer.setIdNumber("A123456789");

        LoanAccountCreateRequest request = new LoanAccountCreateRequest();
        request.setCustomerId("CUST001");
        request.setLiability(new BigDecimal("100000"));
        request.setRate(new BigDecimal("0.02000"));

        when(customerProfileRepository.findById("CUST001")).thenReturn(Optional.of(customer));
        when(accountRepository.existsById("90101123456789")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanAccountResponse response = service.createLoanAccount(request);

        assertEquals("90101123456789", response.getLoanAccountNumber());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        Account saved = captor.getValue();
        assertEquals("90101123456789", saved.getAccountNumber());
        assertEquals("CUST001", saved.getCustomerId());
        assertEquals(AccountType.LOAN, saved.getAccountType());
        assertEquals(Currency.TWD, saved.getCurrency());
        assertEquals(BigDecimal.ZERO, saved.getBalance());
        assertEquals(new BigDecimal("100000.00"), saved.getLiability());
        assertEquals(new BigDecimal("0.02000"), saved.getInterestRate());
    }

    @Test
    @DisplayName("createLoanAccount 若 901 已存在會依序改用 902")
    void createLoanAccount_triesNextPrefixWhenLoanAccountNumberExists() {
        CustomerProfile customer = new CustomerProfile();
        customer.setCustomerId("CUST001");
        customer.setIdNumber("A123456789");

        LoanAccountCreateRequest request = new LoanAccountCreateRequest();
        request.setCustomerId("CUST001");
        request.setLiability(new BigDecimal("100000"));
        request.setRate(new BigDecimal("0.02000"));

        when(customerProfileRepository.findById("CUST001")).thenReturn(Optional.of(customer));
        when(accountRepository.existsById("90101123456789")).thenReturn(true);
        when(accountRepository.existsById("90201123456789")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanAccountResponse response = service.createLoanAccount(request);

        assertEquals("90201123456789", response.getLoanAccountNumber());
    }

    @Test
    @DisplayName("addLoanInterest 使用 TransLogRepository 儲存交易紀錄")
    void addLoanInterest_savesTransLogWithRepository() {
        Account loanAccount = new Account();
        loanAccount.setAccountNumber("90301123456789");
        loanAccount.setAccountType(AccountType.LOAN);
        loanAccount.setCurrency(Currency.TWD);
        loanAccount.setStatus(AccountStatus.ACTIVE);
        loanAccount.setBalance(BigDecimal.ZERO);
        loanAccount.setLiability(new BigDecimal("1000.00"));

        when(accountRepository.findAllByAccountNumberInForUpdate(List.of("90301123456789")))
                .thenReturn(List.of(loanAccount));

        LoanInterestRequest request = new LoanInterestRequest();
        request.setLoanAccountNumber("90301123456789");
        request.setAmount(new BigDecimal("125.00"));
        request.setNote("測試利息");

        service.addLoanInterest(request);

        ArgumentCaptor<TransLog> captor = ArgumentCaptor.forClass(TransLog.class);
        verify(transLogRepository).save(captor.capture());
        TransLog savedLog = captor.getValue();

        assertEquals("90301123456789", savedLog.getAccountNumber());
        assertEquals(EntryType.DEBIT, savedLog.getEntryType());
        assertEquals(TransactionType.INTEREST, savedLog.getTransactionType());
        assertEquals(new BigDecimal("125.00"), savedLog.getAmount());
        assertEquals(new BigDecimal("1000.00"), savedLog.getBalanceBefore());
        assertEquals(new BigDecimal("1125.00"), savedLog.getBalanceAfter());
    }
}

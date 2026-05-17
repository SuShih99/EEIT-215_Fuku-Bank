package com.javaeasybank.account.service;

import com.javaeasybank.account.dto.request.FavoriteAccountRequest;
import com.javaeasybank.account.dto.request.FavoriteAccountUpdateRequest;
import com.javaeasybank.account.dto.response.FavoriteAccountResponse;
import com.javaeasybank.account.entity.FavoriteAccount;
import com.javaeasybank.account.enums.TransferBank;
import com.javaeasybank.account.repository.FavoriteAccountRepository;
import com.javaeasybank.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteAccountService {

    private final FavoriteAccountRepository favoriteAccountRepository;

    @Transactional(readOnly = true)
    public List<FavoriteAccountResponse> getByCustomerId(String customerId) {
        return favoriteAccountRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(FavoriteAccountResponse::fromEntity)
                .toList();
    }

    @Transactional
    public FavoriteAccountResponse create(String customerId, FavoriteAccountRequest request) {
        TransferBank bank = TransferBank.fromCode(request.getBankCode());
        String bankCode = bank.getCode();
        if (favoriteAccountRepository.existsByCustomerIdAndBankCodeAndAccountNumber(
                customerId, bankCode, request.getAccountNumber())) {
            throw new BusinessException("此帳號已在常用名單中");
        }

        FavoriteAccount entity = new FavoriteAccount();
        entity.setCustomerId(customerId);
        entity.setBankCode(bankCode);
        entity.setAccountNumber(request.getAccountNumber());
        entity.setAlias(request.getAlias());
        entity.setBankName(bank.getDisplayName());

        FavoriteAccount saved = favoriteAccountRepository.save(entity);
        log.info("Customer {} added favorite account: {}", customerId, request.getAccountNumber());
        return FavoriteAccountResponse.fromEntity(saved);
    }

    @Transactional
    public FavoriteAccountResponse update(String customerId, Long id, FavoriteAccountUpdateRequest request) {
        FavoriteAccount entity = favoriteAccountRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new BusinessException("常用帳號不存在"));

        if (request.getAlias() != null) entity.setAlias(request.getAlias());
        if (request.getBankCode() != null) {
            TransferBank bank = TransferBank.fromCode(request.getBankCode());
            if (favoriteAccountRepository.existsByCustomerIdAndBankCodeAndAccountNumberAndIdNot(
                    customerId, bank.getCode(), entity.getAccountNumber(), id)) {
                throw new BusinessException("此帳號已在常用名單中");
            }
            entity.setBankCode(bank.getCode());
            entity.setBankName(bank.getDisplayName());
        }
        if (request.getBankName() != null) entity.setBankName(request.getBankName());

        FavoriteAccount saved = favoriteAccountRepository.save(entity);
        log.info("Customer {} updated favorite account id: {}", customerId, id);
        return FavoriteAccountResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(String customerId, Long id) {
        FavoriteAccount entity = favoriteAccountRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new BusinessException("常用帳號不存在"));
        favoriteAccountRepository.delete(entity);
        log.info("Customer {} deleted favorite account id: {}", customerId, id);
    }
}

package com.javaeasybank.creditcard.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.creditcard.dto.CardApplicationResponseDto;
import com.javaeasybank.creditcard.dto.CardApplicationRequestDto;
import com.javaeasybank.creditcard.entity.CardApplication;
import com.javaeasybank.creditcard.enums.CardApplicationStatus;
import com.javaeasybank.creditcard.mapper.CardApplicationMapper;
import com.javaeasybank.creditcard.repository.CardAppRepository;
import com.javaeasybank.customer.entity.CustomerProfile;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
@Transactional
public class CardAppService {

	private final CardAppRepository cardAppRepository;
	private final CardApplicationMapper cardApplicationMapper;
    private final CustomerRepository customerRepository;


	// 查全部
    public Page<CardApplicationResponseDto> findAll(Pageable pageable) {
        return cardAppRepository.findAll(pageable)
            .map(cardApplicationMapper::toDto);

    }

    // 查單筆（DTO）
    public CardApplicationResponseDto findById(Integer id) {
        return cardApplicationMapper.toDto(getEntityById(id));
    }

    // 內部用（Entity）
    private CardApplication getEntityById(Integer id) {
        return cardAppRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Credit card application not found."));
    }

    // 刪除
    public void deleteById(Integer id) {
        CardApplication app = getEntityById(id);
        cardAppRepository.delete(app);
    }

    // 新增
    public CardApplicationResponseDto create(CardApplicationRequestDto requestDto) {
        CardApplication entity = cardApplicationMapper.toEntity(requestDto);

        entity.setStatus(CardApplicationStatus.PENDING);

        CustomerProfile customer = customerRepository.findById(requestDto.getCustomerId())
        .orElseThrow(() -> new BusinessException("Customer not found"));
        entity.setCustomer(customer);



        CardApplication saved = cardAppRepository.save(entity);

        return cardApplicationMapper.toDto(saved);
    }

    // 更新狀態
    public CardApplicationResponseDto updateStatus(Integer id, CardApplicationStatus status) {
        CardApplication app = getEntityById(id);

        app.setStatus(status);

        CardApplication saved = cardAppRepository.save(app);

        return cardApplicationMapper.toDto(saved);
    }
}

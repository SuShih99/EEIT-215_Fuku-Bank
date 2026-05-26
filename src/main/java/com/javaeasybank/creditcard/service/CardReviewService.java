package com.javaeasybank.creditcard.service;

import java.math.BigDecimal;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.creditcard.dto.ApproveCardRequestDto;
import com.javaeasybank.creditcard.dto.CardApplicationItemResponseDto;
import com.javaeasybank.creditcard.entity.CardApplication;
import com.javaeasybank.creditcard.entity.CardApplicationItem;
import com.javaeasybank.creditcard.enums.CardApplicationItemResult;
import com.javaeasybank.creditcard.enums.CardApplicationStatus;
import com.javaeasybank.creditcard.mapper.CardApplicationItemMapper;
import com.javaeasybank.creditcard.repository.CardAppItemRepository;
import com.javaeasybank.creditcard.repository.CardAppRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CardReviewService {

    private static final BigDecimal DEFAULT_CREDIT_LIMIT = new BigDecimal("100000");

    private final CardAppItemRepository cardAppItemRepository;
    private final CardApplicationItemMapper cardAppItemMapper;
    private final CreditCardService cardService;
    private final CardAppRepository cardAppRepository;

    // 審核卡片
    public CardApplicationItemResponseDto approveItem(Integer id, ApproveCardRequestDto request) {
        CardApplicationItem item = cardAppItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Card application item not found"));
        // 防止重複審核
        if (item.getResult() != CardApplicationItemResult.PENDING) {
            throw new BusinessException("Item already reviewed");
        }
        // 審核通過
        item.setResult(CardApplicationItemResult.APPROVED);

        // 核准額度
        BigDecimal approvedLimit = request.approvedLimit() == null
                ? DEFAULT_CREDIT_LIMIT
                : request.approvedLimit();
        item.setApprovedLimit(approvedLimit);

        item.setReviewDate(LocalDateTime.now());
        // 發卡
        cardService.createFromApplicationItem(item);

        // 更新申請狀態
        updateApplicationStatus(item.getApplication().getApplicationId());

        return cardAppItemMapper.toDto(cardAppItemRepository.save(item));
    }

    // 拒絕卡片
    public CardApplicationItemResponseDto rejectItem(Integer id, String remark) {

        CardApplicationItem item = cardAppItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Card application item not found"));

        // 已審核不能再審
        if (item.getResult() != CardApplicationItemResult.PENDING) {
            throw new BusinessException("Item already reviewed");
        }

        // 拒絕
        item.setResult(CardApplicationItemResult.REJECTED);

        // 備註
        item.setRemark(remark);

        // 審核時間
        item.setReviewDate(LocalDateTime.now());

        // 更新申請狀態
        updateApplicationStatus(item.getApplication().getApplicationId());

        return cardAppItemMapper.toDto(cardAppItemRepository.save(item));
    }
    // 更新申請狀態 根據Item的Result來判斷狀態
    private void updateApplicationStatus(Integer applicationId) {

    boolean hasPending = cardAppItemRepository
            .findByApplicationApplicationId(applicationId)
            .stream()
            .anyMatch(item -> item.getResult() == CardApplicationItemResult.PENDING);

    CardApplication application = cardAppRepository.findById(applicationId)
            .orElseThrow(() -> new BusinessException("Application not found"));

    if (hasPending) {
        application.setStatus(CardApplicationStatus.PENDING);
    } else {
        application.setStatus(CardApplicationStatus.COMPLETED);
    }

    cardAppRepository.save(application);
}

}

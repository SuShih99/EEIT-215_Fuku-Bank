package com.javaeasybank.creditcard.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.creditcard.dto.CardApplicationItemResponseDto;
import com.javaeasybank.creditcard.entity.CardApplicationItem;
import com.javaeasybank.creditcard.enums.CardApplicationItemResult;
import com.javaeasybank.creditcard.mapper.CardApplicationItemMapper;
import com.javaeasybank.creditcard.repository.CardAppItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CardReviewService {

    private final CardAppItemRepository cardAppItemRepository;
    private final CardApplicationItemMapper cardAppItemMapper;
    private final CreditCardService cardService;


    // 審核卡片
    public CardApplicationItemResponseDto approveItem(Integer id) {
        CardApplicationItem item = cardAppItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Card application item not found"));
        //防止重複審核
        if (item.getResult() != CardApplicationItemResult.PENDING) {
            throw new BusinessException("Item already reviewed");
        }
        //審核通過
        item.setResult(CardApplicationItemResult.APPROVED);

        item.setReviewDate(LocalDateTime.now());
        //發卡
        cardService.createFromApplicationItem(item);
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

    return cardAppItemMapper.toDto(cardAppItemRepository.save(item));
}



}

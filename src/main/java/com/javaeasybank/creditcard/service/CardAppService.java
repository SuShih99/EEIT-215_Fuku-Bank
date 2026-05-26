package com.javaeasybank.creditcard.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.creditcard.dto.CardApplicationRequestDto;
import com.javaeasybank.creditcard.dto.CardApplicationResponseDto;
import com.javaeasybank.creditcard.entity.CardAccount;
import com.javaeasybank.creditcard.entity.CardApplication;
import com.javaeasybank.creditcard.entity.CardApplicationItem;
import com.javaeasybank.creditcard.entity.CardType;
import com.javaeasybank.creditcard.enums.CardApplicationItemResult;
import com.javaeasybank.creditcard.enums.CardApplicationStatus;
import com.javaeasybank.creditcard.mapper.CardApplicationItemMapper;
import com.javaeasybank.creditcard.mapper.CardApplicationMapper;
import com.javaeasybank.creditcard.repository.CardAccountRepository;
import com.javaeasybank.creditcard.repository.CardAppItemRepository;
import com.javaeasybank.creditcard.repository.CardAppRepository;
import com.javaeasybank.creditcard.repository.CardTypeRepository;
import com.javaeasybank.customer.entity.CustomerProfile;
import com.javaeasybank.customer.repository.CustomerProfileRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
@Transactional
public class CardAppService {

    private static final BigDecimal DEFAULT_CREDIT_LIMIT = new BigDecimal("100000");

    private final CardAppRepository cardAppRepository;
    private final CardAppItemRepository cardAppItemRepository;
    private final CardApplicationMapper cardApplicationMapper;
    private final CardApplicationItemMapper cardApplicationItemMapper;
    private final CustomerProfileRepository customerRepository;
    private final CardTypeRepository cardTypeRepository;
    private final CardAccountRepository cardAccountRepository;

    // 查全部
    public Page<CardApplicationResponseDto> findAll(Pageable pageable) {
        return cardAppRepository.findAll(pageable)
                .map(this::toDtoWithItem);

    }

    // 查單筆（DTO）
    public CardApplicationResponseDto findById(Integer id) {
        return toDtoWithItem(getEntityById(id));
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

        List<Integer> cardTypeIds = requestDto.getCardTypeIds();

        if ((cardTypeIds == null || cardTypeIds.isEmpty()) && requestDto.getCardTypeId() != null) {
            cardTypeIds = List.of(requestDto.getCardTypeId());
        }

        if (cardTypeIds == null || cardTypeIds.isEmpty()) {
            throw new BusinessException("Card type is required");
        }

        // 防止同一次申請重複卡別
        cardTypeIds = cardTypeIds.stream()
                .distinct()
                .toList();

        // 檢查是否已有待審核申請
        for (Integer cardTypeId : cardTypeIds) {
            boolean exists = cardAppItemRepository
                    .existsByApplication_Customer_CustomerIdAndCardType_CardTypeIdAndApplication_Status(
                            requestDto.getCustomerId(),
                            cardTypeId,
                            CardApplicationStatus.PENDING);

            if (exists) {
                throw new BusinessException("你已申辦過其中一張卡片");
            }
        }

        CardApplication entity = cardApplicationMapper.toEntity(requestDto);
        entity.setStatus(CardApplicationStatus.PENDING);

        CustomerProfile customer = customerRepository.findById(requestDto.getCustomerId())
                .orElseThrow(() -> new BusinessException("Customer not found"));
        entity.setCustomer(customer);

        CardApplication saved = cardAppRepository.save(entity);

        // 預設額度
        BigDecimal approvedLimit = cardAccountRepository
                .findByCustomer_CustomerId(requestDto.getCustomerId())
                .map(CardAccount::getCreditLimit)
                .orElse(DEFAULT_CREDIT_LIMIT);

        // 多卡建立多筆 item
        for (Integer cardTypeId : cardTypeIds) {
            CardType cardType = cardTypeRepository.findById(cardTypeId)
                    .orElseThrow(() -> new BusinessException("Card type not found"));

            CardApplicationItem item = new CardApplicationItem();
            item.setApplication(saved);
            item.setCardType(cardType);
            item.setResult(CardApplicationItemResult.PENDING);
            item.setApprovedLimit(approvedLimit);
            item.setAnnualFee(cardType.getAnnualFee());

            cardAppItemRepository.save(item);
        }

        return toDtoWithItem(saved);
    }

    // // 更新狀態 已註解 透過Item的Result來判斷狀態
    // public CardApplicationResponseDto updateStatus(Integer id,
    // CardApplicationStatus status) {
    // CardApplication app = getEntityById(id);

    // app.setStatus(status);

    // CardApplication saved = cardAppRepository.save(app);

    // return toDtoWithItem(saved);
    // }

    public CardApplicationResponseDto updateRemark(Integer id, String remark) {
        CardApplication app = cardAppRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Card application not found"));

        app.setRemark(remark);
        return toDtoWithItem(cardAppRepository.save(app));
    }

    public Page<CardApplicationResponseDto> search(Pageable pageable, String keyword, CardApplicationStatus status) {
        return cardAppRepository.search(
                pageable,
                keyword,
                status).map(this::toDtoWithItem);
    }

    public Page<CardApplicationResponseDto> findMyApplications(String customerId, Pageable pageable) {
        return cardAppRepository
                .findByCustomer_CustomerId(customerId, pageable)
                .map(this::toDtoWithItem);
    }

    private CardApplicationResponseDto toDtoWithItem(CardApplication app) {
        CardApplicationResponseDto dto = cardApplicationMapper.toDto(app);

        List<CardApplicationItem> items = cardAppItemRepository.findByApplicationApplicationId(app.getApplicationId());
        dto.setItems(cardApplicationItemMapper.toDtoList(items));

        items.stream().findFirst().ifPresent(item -> {
            CardType cardType = item.getCardType();
            if (cardType != null) {
                dto.setCardTypeId(cardType.getCardTypeId());
                dto.setCardTypeName(cardType.getCardTypeName());
            }
            dto.setItemResult(item.getResult());
        });

        return dto;
    }

    public void needSupplement(Integer applicationId, String remark) {

        CardApplication application = cardAppRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("找不到信用卡申請單"));

        application.setStatus(CardApplicationStatus.NEED_SUPPLEMENT);

        application.setRemark(remark);

        cardAppRepository.save(application);
    }
}

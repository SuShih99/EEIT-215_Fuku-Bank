package com.javaeasybank.creditcard.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.creditcard.dto.CardTypeCreateRequest;
import com.javaeasybank.creditcard.repository.CreditCardRepository;
import com.javaeasybank.creditcard.entity.CardType;
import com.javaeasybank.creditcard.repository.CardTypeRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CardTypeService {

	
	private final CardTypeRepository cardTypeRepository;
	private final CreditCardRepository cardRepository;

	public CardType insert(CardType cardType) {
		return cardTypeRepository.save(cardType);
	}
	public CardType update(CardType cardType) {
		return cardTypeRepository.save(cardType);
	}
	public void deleteById(Integer id) {

		// 1. 確認卡片存在
	    CardType cardType = cardTypeRepository.findById(id)
	        .orElseThrow(() -> new BusinessException("Card type not found"));

	    // 2. 檢查是否被使用（FK）
	    boolean isUsed = cardRepository.existsByCardType_CardTypeId(id);

	    if (isUsed) {
	        throw new BusinessException("Card type is already in use and cannot be deleted");
	    }

	    // 3. 刪除
	    cardTypeRepository.delete(cardType);
	}
	public CardType findById(Integer id) {
		return cardTypeRepository.findById(id)
	.orElseThrow(()->new BusinessException("Card type not found, id: " + id));
	}
	public List<CardType> findAll() {
		return cardTypeRepository.findAll();
	}
	
	public CardType createCardType(CardTypeCreateRequest request,MultipartFile mf) throws IOException {
		if (mf == null || mf.isEmpty()) {
	        throw new BusinessException("Image file is required");
	    }

	    CardType card = new CardType();
	    card.setCardTypeName(request.getCardTypeName());
	    card.setBrand(request.getBrand());
	    card.setAnnualFee(request.getAnnualFee());
	    card.setCashbackRate(request.getCashbackRate());

	    // 存圖片
	    card.setCardImageUrl(saveImage(mf));
		return cardTypeRepository.save(card);
		
		
	}
	public CardType updateCardType(Integer id, CardTypeCreateRequest request, MultipartFile mf) throws IOException {
		CardType card = cardTypeRepository.findById(id)
	            .orElseThrow(() -> new BusinessException("Card type not found, id: " + id));

	    //更新基本欄位
	    card.setCardTypeName(request.getCardTypeName());
	    card.setBrand(request.getBrand());
	    card.setAnnualFee(request.getAnnualFee());
	    card.setCashbackRate(request.getCashbackRate());

	 // 刪舊圖片
	    String oldImageUrl = card.getCardImageUrl();
	    if (oldImageUrl != null && oldImageUrl.startsWith("/uploads/")) {
	        String oldFileName = oldImageUrl.replace("/uploads/", "");
	        Path oldPath = Paths.get("uploads/", oldFileName);
	        Files.deleteIfExists(oldPath);
	    }
	    card.setCardImageUrl(saveImage(mf));
	    return cardTypeRepository.save(card);
	}
	
	private String saveImage(MultipartFile mf) throws IOException {
	    String originalName = mf.getOriginalFilename();

	    if (originalName == null || !originalName.contains(".")) {
	        throw new BusinessException("Invalid file name");
	    }

	    String ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();

	    // 檔案驗證（建議一定要有）
	    if (!ext.equals(".jpg") && !ext.equals(".png")) {
	        throw new BusinessException("Only .jpg and .png files are allowed");
	    }

	    String fileName = UUID.randomUUID() + ext;

	    Path path = Paths.get("uploads/", fileName);
	    Files.createDirectories(path.getParent());
	    Files.write(path, mf.getBytes());

	    return "/uploads/" + fileName;
	}
}

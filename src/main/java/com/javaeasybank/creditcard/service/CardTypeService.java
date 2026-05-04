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
import com.javaeasybank.creditcard.dto.CardTypeRequestDto;
import com.javaeasybank.creditcard.dto.CardTypeResponseDto;
import com.javaeasybank.creditcard.repository.CreditCardRepository;
import com.javaeasybank.creditcard.entity.CardType;
import com.javaeasybank.creditcard.mapper.CardTypeMapper;
import com.javaeasybank.creditcard.repository.CardTypeRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CardTypeService {

	private final CardTypeRepository cardTypeRepository;
	private final CreditCardRepository cardRepository;
	private final CardTypeMapper cardTypeMapper;

	// 新增卡片型態
	public CardTypeResponseDto createCardType(CardTypeRequestDto request) throws IOException {
		if (request.getCardImageUrl() == null || request.getCardImageUrl().isEmpty()) {
	        throw new BusinessException("Image URL is required");
	    }
		// 建立卡片型態
		CardType cardType = cardTypeMapper.toEntity(request);

		// 存圖片
		cardType.setCardImageUrl(request.getCardImageUrl());
		CardType saved = cardTypeRepository.save(cardType);
		return cardTypeMapper.toDto(saved);
	}
	// 更新卡片型態
	public CardTypeResponseDto updateCardType(Integer id, CardTypeRequestDto request, MultipartFile mf) throws IOException {
		CardType cardType = cardTypeRepository.findById(id)
				.orElseThrow(() -> new BusinessException("Card type not found, id: " + id));

		// 更新基本欄位
		cardTypeMapper.updateEntityFromDto(request, cardType);

		// 如果有傳新圖片才處理
		if (mf != null && !mf.isEmpty()) {
			// 刪舊圖片
			String oldImageUrl = cardType.getCardImageUrl();
			if (oldImageUrl != null && oldImageUrl.startsWith("uploads/")) {
				String oldFileName = oldImageUrl.replace("uploads/", "");
				Path oldPath = Paths.get("uploads/", oldFileName);
				Files.deleteIfExists(oldPath);
			}
			cardType.setCardImageUrl(saveImage(mf));
		}

		return cardTypeMapper.toDto(cardTypeRepository.save(cardType));
	}

	public CardTypeResponseDto updateCardType(Integer id, CardTypeRequestDto request) {
		CardType cardType = cardTypeRepository.findById(id)
				.orElseThrow(() -> new BusinessException("Card type not found, id: " + id));
		cardTypeMapper.updateEntityFromDto(request, cardType);
		return cardTypeMapper.toDto(cardTypeRepository.save(cardType));
	}
	// 存圖片
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

		return "uploads/" + fileName;
	}
	// 刪除卡片型態
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
	// 查詢卡片型態
	public CardTypeResponseDto findById(Integer id) {
		CardType cardType = cardTypeRepository.findById(id)
			.orElseThrow(() -> new BusinessException("Card type not found, id: " + id));
			return cardTypeMapper.toDto(cardType);
	}
	// 查詢所有卡片型態
	public List<CardTypeResponseDto> findAll() {
		return cardTypeMapper.toDtoList(cardTypeRepository.findAll());

	}
}

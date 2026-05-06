package com.javaeasybank.risk.service;

import com.javaeasybank.risk.core.enums.BlacklistType;
import com.javaeasybank.risk.dto.BlackListRequest;
import com.javaeasybank.risk.dto.BlackListResponse;
import com.javaeasybank.risk.entity.Blacklist;
import com.javaeasybank.risk.repository.BlackListRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class BlackListService {

    private final BlackListRepository blRepos;

    public Page<BlackListResponse> findAll(Pageable pageable) {
        return blRepos.findAll(pageable)
                .map(this::toResponse);
    }

    public BlackListRequest create(BlackListRequest request) {
        Blacklist entity = toEntity(request);
        Blacklist saved = blRepos.save(entity);
        return toRequest(saved);
    }

    public BlackListResponse findByBusinessKey(BlacklistType type, String value) {
        return blRepos.findActiveBlacklist(type, value)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("找不到有效的黑名單紀錄: [%s] %s", type, value)));
    }

    /**
     * 更新：同樣透過業務主鍵定位
     */
    public BlackListResponse updateByBusinessKey(BlacklistType type, String value, BlackListRequest request) {
        Blacklist entity = blRepos.findByBusinessKey(type, value)
                .orElseThrow(() -> new EntityNotFoundException("找不到欲更新的有效紀錄"));

        // 更新內容 (例如修改原因或過期時間)
        entity.setReason(request.getReason());
        entity.setExpireAt(request.getExpireAt());
        entity.setSource(request.getSource());

        return toResponse(blRepos.save(entity));
    }

    /**
     * 停用：透過業務主鍵將狀態設為 false
     */
    public void updateStatusByBusinessKey(BlacklistType type, String value, Boolean status) {
        Blacklist entity = blRepos.findByBusinessKey(type, value)
                .orElseThrow(() -> new EntityNotFoundException("找不到對應的黑名單紀錄"));

        entity.setStatus(status);
        blRepos.save(entity);
    }

    /**
     * 給外部模組（如 CustomerService）呼叫的快速校驗接口
     */
    public boolean isBlacklisted(BlacklistType type, String value) {
        // 調用 Repository 的 findActiveBlacklist 確保只針對「生效中」的黑名單進行攔截
        return blRepos.findActiveBlacklist(type, value).isPresent();
    }

    private BlackListResponse toResponse(Blacklist bl) {
        BlackListResponse response = new BlackListResponse();
        response.setListType(bl.getListType());
        response.setListValue(bl.getListValue());
        response.setSource(bl.getSource());
        response.setReason(bl.getReason());
        response.setStatus(bl.getStatus());
        response.setExpireAt(bl.getExpireAt());
        response.setCreatedAt(bl.getCreatedAt());
        // response.setUpdatedAt(bl.getUpdatedAt());complie有問題

        return response;
    }

    private BlackListRequest toRequest(Blacklist bl) {
        BlackListRequest request = new BlackListRequest();
        request.setListType(bl.getListType());
        request.setListValue(bl.getListValue());
        request.setSource(bl.getSource());
        request.setReason(bl.getReason());
        request.setStatus(bl.getStatus());
        request.setExpireAt(bl.getExpireAt());
        request.setCreatedAt(bl.getCreatedAt());
        // request.setUpdatedAt(bl.getUpdatedAt());complie有問題

        return request;
    }

    private Blacklist toEntity(BlackListRequest dto) {
        Blacklist entity = new Blacklist();
        entity.setListType(dto.getListType());
        entity.setListValue(dto.getListValue());
        entity.setSource(dto.getSource());
        entity.setReason(dto.getReason());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : Boolean.TRUE);
        entity.setExpireAt(dto.getExpireAt());
        return entity;
    }
}

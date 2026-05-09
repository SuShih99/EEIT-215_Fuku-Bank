package com.javaeasybank.risk.handler;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.risk.core.BaseRiskHandler;
import com.javaeasybank.risk.core.RiskTarget;
import com.javaeasybank.risk.core.enums.BlacklistType;
import com.javaeasybank.risk.core.KYC;
import com.javaeasybank.risk.core.enums.BusinessScene;
import com.javaeasybank.risk.core.enums.Disposition;
import com.javaeasybank.risk.core.enums.RiskLevel;
import com.javaeasybank.risk.service.BlackListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.javaeasybank.risk.utils.MaskSensitiveUtils.maskSensitive;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerCreateHandler extends BaseRiskHandler {

    private final BlackListService blService;

    @Override
    public BusinessScene getScene() {
        return BusinessScene.CREATE_CUSTOMER;
    }

    @Override
    public void handle(RiskTarget target) {
        // 取得所有擴充欄位
        Map<String, Object> metadata = target.getRiskMetadata();

        // 1. 組裝校驗矩陣
        Map<BlacklistType, String> checkMap = new EnumMap<>(BlacklistType.class);
        putIfPresent(checkMap, BlacklistType.ID_CARD, metadata.get(KYC.ID_CARD));
        putIfPresent(checkMap, BlacklistType.PHONE, metadata.get(KYC.PHONE));
        putIfPresent(checkMap, BlacklistType.EMAIL, metadata.get(KYC.EMAIL));

        List<BlacklistType> hits = blService.checkAll(checkMap);

        if (!hits.isEmpty()) {
            String hitDetails = hits.stream()
                    .map(BlacklistType::getDescription)
                    .collect(Collectors.joining(", "));
            // 確保有 businessID，優先使用 ID_CARD，若無則回退至手機號碼
            String businessId = checkMap.getOrDefault(BlacklistType.ID_CARD,
                    checkMap.getOrDefault(BlacklistType.PHONE, "UNKNOWN_CUSTOMER"));
            // 拋出異常前，必須確保 Event 被送出以供 RiskLogService 記錄
            String reason = "命中黑名單: " + hitDetails;

            // 記錄log (注意：在生產環境中 ID 應脫敏)
            String masked = maskSensitive(businessId);
            log.warn("[Risk] Blacklist Hit! BusinessId: {}, Hits: {}", masked, hitDetails);

            // 發送事件
            publishEvent(target, businessId, RiskLevel.HIGH, Disposition.REJECT, reason);
            // 阻斷業務流程：回傳統一錯誤碼與訊息，避免外露敏感資訊
            throw new BusinessException("E_RISK_REJECT: 系統核對資訊異常，請洽客服人員。");
        }
    }

    private void putIfPresent(Map<BlacklistType, String> map, BlacklistType type, Object value) {
        if (value != null) {
            map.put(type, String.valueOf(value));
        }
    }

    @Override
    public RiskTarget resolve(Object[] args) {
        if (args == null || args.length == 0) {
            log.warn("CustomerCreateHandler: no args");
            return null;
        }
        if (args[0] instanceof RiskTarget rt) {
            return rt;
        }
        log.warn("CustomerCreateHandler: args[0] is not RiskTarget, got: {}",
                args[0] == null ? "null" : args[0].getClass().getSimpleName());
        return null;
    }

}

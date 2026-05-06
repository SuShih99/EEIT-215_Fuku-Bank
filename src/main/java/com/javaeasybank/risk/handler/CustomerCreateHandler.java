package com.javaeasybank.risk.handler;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.risk.core.RiskHandler;
import com.javaeasybank.risk.core.RiskTarget;
import com.javaeasybank.risk.core.enums.BlacklistType;
import com.javaeasybank.risk.core.KYC;
import com.javaeasybank.risk.core.enums.BusinessScene;
import com.javaeasybank.risk.core.enums.Disposition;
import com.javaeasybank.risk.core.enums.RiskLevel;
import com.javaeasybank.risk.service.BlackListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CustomerCreateHandler implements RiskHandler {

    private final BlackListService blService;

    public CustomerCreateHandler(BlackListService blService) {
        this.blService = blService;
    }

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
        checkMap.put(BlacklistType.ID_CARD, (String) metadata.get(KYC.ID_CARD));
        checkMap.put(BlacklistType.PHONE, (String) metadata.get(KYC.PHONE));
        checkMap.put(BlacklistType.EMAIL, (String) metadata.get(KYC.EMAIL));

        List<BlacklistType> hits = blService.checkAll(checkMap);

        if (!hits.isEmpty()) {
            String hitDetails = hits.stream()
                    .map(BlacklistType::getDescription)
                    .collect(Collectors.joining(", "));

            // 嚴格處置：拋出異常前，必須確保 Event 被送出以供 RiskLogService 記錄
            String reason = "命中黑名單: " + hitDetails;

            // 發送事件 (這部分邏輯應寫在基底類或輔助類中)
           // publishRiskEvent(target, RiskLevel.HIGH, Disposition.REJECT, reason ,businessID);

            throw new BusinessException("風險控管攔截：系統偵測到異常資訊，請聯繫分行人員。");
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

package com.javaeasybank.risk.aspect;

import com.javaeasybank.risk.annotation.RiskCheck;
import com.javaeasybank.risk.core.RiskHandler;
import com.javaeasybank.risk.core.RiskScene;
import com.javaeasybank.risk.core.RiskTarget;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Aspect // 宣告這是一個切面 (攔截器)
@Component
@Slf4j
public class RiskCheckAspect {

    // Key 是 Enum，Value 是對應的處理實作
    private final Map<RiskScene, RiskHandler> handlerMap;

    // 利用建構子注入，並直接轉換為 Map
    public RiskCheckAspect(List<RiskHandler> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(RiskHandler::getScene, h -> h));
    }

    @Around("@annotation(riskCheck)")
    public Object monitorRisk(ProceedingJoinPoint joinPoint, RiskCheck riskCheck) throws Throwable {

        RiskScene scene = riskCheck.scene();
        Object[] args = joinPoint.getArgs();

        for (Object arg : args) {
            if (arg instanceof RiskTarget target) {
                RiskHandler handler = handlerMap.get(scene);

                if (handler != null) {
                    handler.check(target.getTargetIdentifier(), target.getAmount());
                } else {
                    log.warn("找不到對應 {} 的風控處理器", scene);
                }
            }
        }

        return joinPoint.proceed();
    }
}

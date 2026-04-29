package com.javaeasybank.risk.service;

import com.javaeasybank.risk.dto.RiskEventResponse;
import com.javaeasybank.risk.entity.RiskEventLog;
import com.javaeasybank.risk.repository.RiskEventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskEventService {

    private final RiskEventLogRepository riskEventLogRepository;

    public Page<RiskEventResponse> findAll(Pageable pageable) {
        return riskEventLogRepository.findAll(pageable)
                .map(this::toDto);
    }

    public Page<RiskEventResponse> search(String eventType, String actionTaken, Pageable pageable) {
        return riskEventLogRepository.search(eventType, actionTaken, pageable)
                .map(this::toDto);
    }

    private RiskEventResponse toDto(RiskEventLog rel) {
        RiskEventResponse response = new RiskEventResponse();

        response.setEventType(rel.getEventType());
        response.setRiskLevel(rel.getRiskLevel());
        response.setTargetIdentifier(rel.getTargetIdentifier());
        response.setActionTaken(rel.getActionTaken());
        response.setTriggerReason(rel.getTriggerReason());
        response.setTransactionAmount(rel.getTransactionAmount());
        response.setCreatedAt(rel.getCreatedAt());

        return response;
    }
}

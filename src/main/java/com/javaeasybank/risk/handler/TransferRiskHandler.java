package com.javaeasybank.risk.handler;

import com.javaeasybank.risk.core.RiskHandler;
import com.javaeasybank.risk.core.RiskScene;

import java.math.BigDecimal;

public class TransferRiskHandler implements RiskHandler {


    @Override
    public RiskScene getScene() {
        return RiskScene.TRANSFER;
    }

    @Override
    public void check(String identifier, BigDecimal amount) {

    }
}

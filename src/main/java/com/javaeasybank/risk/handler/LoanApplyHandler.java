package com.javaeasybank.risk.handler;

import com.javaeasybank.risk.core.RiskHandler;
import com.javaeasybank.risk.core.RiskTarget;
import com.javaeasybank.risk.core.enums.BusinessScene;

public class LoanApplyHandler implements RiskHandler {

    @Override
    public BusinessScene getScene() {
        return BusinessScene.LOAN;
    }

    @Override
    public void handle(RiskTarget target) {

    }

    @Override
    public RiskTarget resolve(Object[] args) {
        return null;
    }


}

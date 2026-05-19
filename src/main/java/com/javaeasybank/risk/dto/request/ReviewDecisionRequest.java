package com.javaeasybank.risk.dto.request;

import com.javaeasybank.loan.enums.LoanDocumentType;
import com.javaeasybank.risk.enums.ReviewResult;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReviewDecisionRequest {
    /**
     * 最終決策：APPROVED / REJECTED
     */
    @NotNull(message = "reviewResult 不可為空")
    private ReviewResult reviewResult;

    /**
     * 審核人備註
     */
    private String adminComment;
    private List<LoanDocumentType> requiredDocuments;
}

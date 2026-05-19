package com.javaeasybank.loan.enums;

/**
 * 還款期數狀態列舉。
 *
 * <p>描述單一期還款紀錄的繳款狀態。
 * 撥款時由 {@code LoanRepaymentService} 預建所有期數（初始皆為 {@code SCHEDULED}），
 * 後續由排程任務（{@code LoanRepaymentScheduler}）定期掃描並更新為 {@code OVERDUE}。</p>
 */
public enum LoanRepaymentStatus {

    /** 待繳：預建紀錄，尚未到達應繳日期，或到期但尚未繳款。 */
    SCHEDULED,

    /** 已繳清：該期款項已成功入帳，{@code paidDate} 有值。 */
    PAID,

    /** 逾期未繳：已超過應繳日期仍未繳款，由排程自動標記。 */
    OVERDUE
}

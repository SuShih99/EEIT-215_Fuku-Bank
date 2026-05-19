package com.javaeasybank.loan.enums;

/**
 * 貸款帳戶狀態列舉。
 *
 * <p>描述一筆已撥款貸款帳戶目前的還款狀態，
 * 由排程任務（{@code LoanRepaymentScheduler}）依還款紀錄自動更新。</p>
 */
public enum LoanAccountStatus {

    /** 還款中：帳戶正常，所有期數皆未逾期。 */
    ACTIVE,

    /** 存在逾期：至少有一期還款紀錄狀態為 {@code OVERDUE}。 */
    OVERDUE,

    /** 已結清：所有期數均已繳清，貸款合約終止。 */
    PAID_OFF,
}

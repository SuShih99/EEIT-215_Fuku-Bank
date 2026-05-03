/**
 * 後端 errorCode → 前端中文訊息對照表
 * 後端回傳英文 errorCode，前端依此顯示對應中文
 */
const ERROR_MESSAGES = {
  // Account 帳戶模組
  ACCOUNT_NOT_FOUND: '找不到該帳戶',
  CUSTOMER_NOT_FOUND: '客戶不存在',
  DUPLICATE_CHECKING_ACCOUNT: '該客戶已擁有相同幣別的活存帳戶',
  SUB_ACCOUNT_CURRENCY_MUST_BE_TWD: '子帳戶僅限台幣（TWD）',
  NO_ACTIVE_TWD_CHECKING: '該客戶必須先擁有啟用中的台幣活存帳戶，才能建立子帳戶',
  PARENT_ACCOUNT_REQUIRED: '建立子帳戶必須提供父帳戶帳號',
  PARENT_ACCOUNT_NOT_FOUND: '找不到指定的父帳戶',
  PARENT_ACCOUNT_OWNER_MISMATCH: '父帳戶不屬於該客戶',
  STATUS_UNCHANGED: '帳戶狀態未變更',
  INVALID_STATUS_TRANSITION: '不允許的狀態變更',
  INSUFFICIENT_BALANCE: '餘額不足',
}

/**
 * 根據後端回傳的 error response 取得中文錯誤訊息
 * @param {Error} err - axios catch 到的 error
 * @param {string} fallback - 查不到時的預設訊息
 * @returns {string} 中文錯誤訊息
 */
export function getErrorMessage(err, fallback = '操作失敗') {
  const errorCode = err.response?.data?.errorCode
  if (errorCode && ERROR_MESSAGES[errorCode]) {
    return ERROR_MESSAGES[errorCode]
  }
  // 沒有 errorCode 就 fallback 到後端的 message 或預設訊息
  return err.response?.data?.message || fallback
}

export default ERROR_MESSAGES

package com.javaeasybank.common.util;

import jakarta.servlet.http.HttpSession;


public class SessionUtil {

    // Session 裡存放登入資訊的 key，統一在這裡定義
    // 避免各地字串打錯或不一致
    private static final String ADMIN_KEY = "loginAdmin";
    private static final String CLIENT_KEY = "loginCustomer";
    private static final String ROLE_KEY = "loginRole";

    // 經理角色的 roleId，與 auth 模組的 role 表對應
    private static final int MANAGER_ROLE_ID = 1;

    // 工具類別不需要被實例化，constructor 設為 private
    private SessionUtil() {}
    // 寫入 Session（登入）

    /**
     * 行員登入，將登入標記與角色 ID 寫入 Session。
     *
     * @param session  HttpSession
     * @param roleId   行員的角色 ID（由 auth 模組的 AuthEmpResponse 提供）
     */
    public static void login(HttpSession session, Integer roleId) {
        session.setAttribute(ADMIN_KEY, true);
        session.setAttribute(ROLE_KEY, roleId);
    }

    /**
     * 客戶登入，將登入標記寫入 Session。
     *
     * @param session    HttpSession
     * @param customerId 客戶 ID
     */
    public static void loginCustomer(HttpSession session, Long customerId) {
        session.setAttribute(CLIENT_KEY, customerId);
    }
    // 讀取 Session（驗證）

    /**
     * 確認行員是否已登入。
     * 回傳 true = 已登入，false = 未登入或 session 過期。
     */
    public static boolean isAdminLoggedIn(HttpSession session) {
        return session.getAttribute(ADMIN_KEY) != null;
    }

    /**
     * 確認客戶是否已登入。
     */
    public static boolean isClientLoggedIn(HttpSession session) {
        return session.getAttribute(CLIENT_KEY) != null;
    }

    /**
     * 確認是否為經理角色。
     * 需要先確認已登入再呼叫這個方法。
     *
     * 判斷邏輯：比對 Session 中的 roleId 是否等於經理角色 ID。
     */
    public static boolean isManager(HttpSession session) {
        if (!isAdminLoggedIn(session)) return false;
        Object roleId = session.getAttribute(ROLE_KEY);
        if (roleId == null) return false;
        return MANAGER_ROLE_ID == (Integer) roleId;
    }

    /**
     * 取得目前登入的行員角色 ID。
     * 回傳 null 代表未登入。
     */
    public static Integer getLoginRoleId(HttpSession session) {
        Object roleId = session.getAttribute(ROLE_KEY);
        return roleId != null ? (Integer) roleId : null;
    }

    /**
     * 取得目前登入的客戶 ID。
     * 回傳 null 代表未登入。
     */
    public static Long getLoginCustomerId(HttpSession session) {
        Object customerId = session.getAttribute(CLIENT_KEY);
        return customerId != null ? (Long) customerId : null;
    }
    // 登出

    /**
     * 登出，銷毀整個 session。
     */
    public static void logout(HttpSession session) {
        session.invalidate();
    }
}
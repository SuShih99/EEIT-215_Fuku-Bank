package com.javaeasybank.account.enums;

import com.javaeasybank.account.exception.TransferException;

import java.util.Arrays;
import java.util.List;

/**
 * 國內轉帳可選金融機構代碼。
 */
public enum TransferBank {
    CENTRAL_BANK("000", "中央銀行國庫局"),
    BOT("004", "臺灣銀行"),
    LAND_BANK("005", "臺灣土地銀行"),
    TCB("006", "合作金庫商業銀行"),
    FIRST_BANK("007", "第一商業銀行"),
    HUA_NAN("008", "華南商業銀行"),
    CHANG_HWA("009", "彰化商業銀行"),
    SHANGHAI("011", "上海商業儲蓄銀行"),
    TAIPEI_FUBON("012", "台北富邦商業銀行"),
    CATHAY_UNITED("013", "國泰世華商業銀行"),
    BANK_OF_KAOHSIUNG("016", "高雄銀行"),
    MEGA("017", "兆豐國際商業銀行"),
    AGRICULTURAL_BANK("018", "全國農業金庫"),
    MIZUHO("020", "日商瑞穗銀行台北分行"),
    CITI_TAIWAN("021", "花旗(台灣)商業銀行"),
    BANK_OF_AMERICA("022", "美國銀行台北分行"),
    BANGKOK_BANK("023", "泰國盤谷銀行台北分行"),
    METROPOLITAN_BANK("025", "菲律賓首都銀行台北分行"),
    UOB("029", "新加坡商大華銀行台北分行"),
    STATE_STREET("030", "美商道富銀行台北分行"),
    SOCIETE_GENERALE("037", "法商法國興業銀行台北分行"),
    ANZ("039", "澳商澳盛銀行台北分行"),
    O_BANK("048", "王道商業銀行"),
    TAIWAN_BUSINESS_BANK("050", "臺灣中小企業銀行"),
    STANDARD_CHARTERED("052", "渣打國際商業銀行"),
    TAICHUNG_BANK("053", "台中商業銀行"),
    KING_TOWN_BANK("054", "京城商業銀行"),
    MEGA_BILLS("060", "兆豐票券金融股份有限公司"),
    CHINA_BILLS("061", "中華票券金融股份有限公司"),
    INTERNATIONAL_BILLS("062", "國際票券金融股份有限公司"),
    WANTONG_BILLS("066", "萬通票券金融股份有限公司"),
    DEUTSCHE_BANK("072", "德商德意志銀行台北分行"),
    BANK_OF_EAST_ASIA("075", "香港商東亞銀行台北分行"),
    JP_MORGAN("076", "美商摩根大通銀行台北分行"),
    HSBC_TAIWAN("081", "匯豐(台灣)商業銀行"),
    BNP_PARIBAS("082", "法國巴黎銀行台北分行"),
    OCBC("085", "新加坡商新加坡華僑銀行台北分行"),
    CREDIT_AGRICOLE("086", "法商東方匯理銀行台北分行"),
    UBS("092", "瑞士商瑞士銀行台北分行"),
    ING("093", "荷商安智銀行台北分行"),
    MUFG("098", "日商三菱日聯銀行台北分行"),
    RUIXING_BANK("101", "瑞興商業銀行"),
    HWATAI_BANK("102", "華泰商業銀行"),
    SHIN_KONG_BANK("103", "臺灣新光商業銀行"),
    SUNNY_BANK("108", "陽信商業銀行"),
    KEELUNG_FIRST_CREDIT("114", "基隆第一信用合作社"),
    KEELUNG_SECOND_CREDIT("115", "基隆市第二信用合作社"),
    BANXIN_BANK("118", "板信商業銀行"),
    TAMSUI_FIRST_CREDIT("119", "淡水第一信用合作社"),
    HSINCHU_FIRST_CREDIT("130", "新竹第一信用合作社"),
    HSINCHU_THIRD_CREDIT("132", "新竹第三信用合作社"),
    TAICHUNG_SECOND_CREDIT("146", "台中市第二信用合作社"),
    SANXIN_BANK("147", "三信商業銀行"),
    CHANGHUA_SIXTH_CREDIT("162", "彰化第六信用合作社"),
    KAOHSIUNG_THIRD_CREDIT("204", "高雄市第三信用合作社"),
    HUALIEN_FIRST_CREDIT("215", "花蓮第一信用合作社"),
    HUALIEN_SECOND_CREDIT("216", "花蓮第二信用合作社"),
    SMBC("321", "日商三井住友銀行台北分行"),
    BBVA("326", "西班牙商西班牙對外銀行臺北分行"),
    TA_CHING_BILLS("372", "大慶票券金融股份有限公司"),
    BANK_OF_CHINA("380", "大陸商中國銀行臺北分行"),
    BANK_OF_COMMUNICATIONS("381", "大陸商交通銀行臺北分行"),
    CHINA_CONSTRUCTION_BANK("382", "大陸商中國建設銀行臺北分行"),
    AGRICULTURE_FINANCE_INFO("600", "農金資訊股份有限公司"),
    CHUNGHWA_POST("700", "中華郵政股份有限公司"),
    UNION_BANK("803", "聯邦商業銀行"),
    FAR_EASTERN_BANK("805", "遠東國際商業銀行"),
    YUANTA_BANK("806", "元大商業銀行"),
    SINOPAC_BANK("807", "永豐商業銀行"),
    ESUN_BANK("808", "玉山商業銀行"),
    KGI_BANK("809", "凱基商業銀行"),
    DBS_TAIWAN("810", "星展(台灣)商業銀行"),
    TAISHIN_BANK("812", "台新國際商業銀行"),
    JIH_SUN_BANK("815", "日盛國際商業銀行"),
    ENTIE_BANK("816", "安泰商業銀行"),
    CTBC_BANK("822", "中國信託商業銀行"),
    NEXT_BANK("823", "將來商業銀行"),
    LINE_BANK("824", "連線商業銀行"),
    RAKUTEN_BANK("826", "樂天國際商業銀行"),
    JVB("909", "爪哇銀行"),
    SOUTH_FARMER_FISHER_INFO("952", "財團法人農漁會南區資訊中心"),
    TRADE_VAN("995", "關貿網路股份有限公司"),
    NATIONAL_TREASURY("996", "財政部國庫署"),
    CREDIT_COOP_SOUTH_INFO("997", "中華民國信用合作社聯合社南區聯合資訊處理中心");

    private final String code;
    private final String displayName;

    TransferBank(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLabel() {
        return displayName + " " + code;
    }

    public boolean isJavaBank() {
        return this == JVB;
    }

    public static TransferBank fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new TransferException("MISSING_BANK_CODE", "轉入銀行不可為空");
        }
        String normalizedCode = code.trim();
        return Arrays.stream(values())
                .filter(bank -> bank.code.equals(normalizedCode))
                .findFirst()
                .orElseThrow(() -> new TransferException("INVALID_BANK_CODE", "不支援的轉入銀行代碼"));
    }

    public static List<TransferBank> customerOptions() {
        return Arrays.asList(values());
    }
}

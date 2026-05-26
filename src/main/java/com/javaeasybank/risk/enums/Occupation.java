package com.javaeasybank.risk.enums;

import lombok.Getter;

@Getter
public enum Occupation {
    LEGISLATOR_MANAGER("民意代表/高階主管", EmploymentStatus.EMPLOYED),
    PROFESSIONAL("專業人員", EmploymentStatus.EMPLOYED),
    TECHNICIAN("技術員及助理專業人員", EmploymentStatus.EMPLOYED),
    CLERICAL("事務支援人員", EmploymentStatus.EMPLOYED),
    SERVICE_SALES("服務及銷售工作人員", EmploymentStatus.EMPLOYED),
    AGRICULTURAL("農林漁牧業生產人員", EmploymentStatus.EMPLOYED),
    CRAFT_WORKER("技藝有關工作人員", EmploymentStatus.EMPLOYED),
    MACHINE_OPERATOR("機械設備操作及組裝人員", EmploymentStatus.EMPLOYED),
    ELEMENTARY("基層技術工及勞力工", EmploymentStatus.EMPLOYED),
    MILITARY("軍人", EmploymentStatus.EMPLOYED),
    NONE("無", EmploymentStatus.UNEMPLOYED),
    OTHER("其他", EmploymentStatus.EMPLOYED);

    private final String desc;
    private final EmploymentStatus defaultStatus;

    Occupation(String desc, EmploymentStatus defaultStatus) {
        this.desc = desc;
        this.defaultStatus = defaultStatus;
    }

    public static Occupation fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return OTHER; // 沒填就歸類其他
        }

        String cleanText = text.trim().toUpperCase();

        // 1. 先做第一輪精準比對 (Enum變數名或原本的完整中文desc)
        for (Occupation o : Occupation.values()) {
            if (o.name().equalsIgnoreCase(cleanText) || o.getDesc().equals(cleanText)) {
                return o;
            }
        }

        // 2. 第二輪：如果精準比對沒中，改用「關鍵字模糊匹配」
        if (cleanText.contains("工程師") || cleanText.contains("軟體") || cleanText.contains("技術") || cleanText.contains("科技")) {
            return TECHNICIAN; // 歸類到 技術員及助理專業人員
        }
        if (cleanText.contains("立委") || cleanText.contains("議員") || cleanText.contains("經理") || cleanText.contains("主管") || cleanText.contains("CEO")) {
            return LEGISLATOR_MANAGER; // 歸類到 民意代表/高階主管
        }
        if (cleanText.contains("醫生") || cleanText.contains("律師") || cleanText.contains("會計師") || cleanText.contains("護理")) {
            return PROFESSIONAL; // 歸類到 專業人員
        }
        if (cleanText.contains("櫃檯") || cleanText.contains("行政") || cleanText.contains("助理") || cleanText.contains("小秘書")) {
            return CLERICAL; // 歸類到 事務支援人員
        }
        if (cleanText.contains("店員") || cleanText.contains("外送") || cleanText.contains("業務") || cleanText.contains("銷售") || cleanText.contains("餐飲")) {
            return SERVICE_SALES; // 歸類到 服務及銷售工作人員
        }
        if (cleanText.contains("軍") || cleanText.contains("兵") || cleanText.contains("校官")) {
            return MILITARY; // 軍人
        }
        if (cleanText.contains("沒工作") || cleanText.contains("待業") || cleanText.contains("無業") || cleanText.contains("失業")) {
            return NONE; // 無
        }

        // 3. 真的都完全找不到關鍵字，才兜底給 OTHER
        return OTHER;
    }

}
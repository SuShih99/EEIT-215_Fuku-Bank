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

}
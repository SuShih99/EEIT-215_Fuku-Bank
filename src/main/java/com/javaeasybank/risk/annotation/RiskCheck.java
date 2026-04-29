package com.javaeasybank.risk.annotation;

import com.javaeasybank.risk.core.RiskScene;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// @Target 限制這個標註只能用在「方法」上
@Target(ElementType.METHOD)
// @Retention 設定這個標註在「執行時期」也要保留，這樣 AOP 才抓得到
@Retention(RetentionPolicy.RUNTIME)
public @interface RiskCheck {

    // 定義業務場景，預設值為 GENERAL
    RiskScene scene() default RiskScene.GENERAL;
}

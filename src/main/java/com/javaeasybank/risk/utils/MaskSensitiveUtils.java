package com.javaeasybank.risk.utils;

public class MaskSensitiveUtils {
    public static String maskSensitive(String v) {
        if (v == null) return "****";
        String s = v.trim();
        if (s.length() <= 4) return "****";
        return "****" + s.substring(s.length() - 4);
    }
}

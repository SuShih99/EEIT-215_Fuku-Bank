package com.javaeasybank.account.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceIdGeneratorTest {

    @Test
    @DisplayName("驗證生成的 ID 格式是否符合預期")
    void testGenerateFormat() {
        String id = ReferenceIdGenerator.generate();

        assertNotNull(id);
        // 驗證是否以 TXN- 開頭
        assertTrue(id.startsWith("TXN-"));

        // 驗證總長度：TXN(3) + -(1) + yyyyMMdd(8) + -(1) + HHmmss(6) + -(1) + 8hex(8) = 28 字元
        assertEquals(28, id.length());

        // 使用正則表達式驗證更精確的格式
        // ^TXN-\d{8}-\d{6}-[0-9a-f]{8}$
        assertTrue(id.matches("^TXN-\\d{8}-\\d{6}-[0-9a-f]{8}$"));
    }

    @Test
    @DisplayName("在高頻率生成下驗證 ID 的唯一性")
    void testUniqueness() {
        int count = 10000;
        Set<String> idSet = new HashSet<>();

        for (int i = 0; i < count; i++) {
            String id = ReferenceIdGenerator.generate();
            // 如果 add 回傳 false，代表 Set 裡已經有重複的 ID
            assertTrue(idSet.add(id), "偵測到重複的 ID: " + id);
        }

        assertEquals(count, idSet.size());
    }
}
package com.javaeasybank.common.util;

import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class PdfCjkFontLoader {

    public static final String FONT_FAMILY = "JavaBankCjk";
    public static final String FONT_RESOURCE_PATH = "fonts/NotoSansTC-Regular.ttf";

    private PdfCjkFontLoader() {
    }

    public static Path copyClasspathFontToTempFile(ClassLoader classLoader, Logger log, String logPrefix) throws IOException {
        try (InputStream inputStream = classLoader.getResourceAsStream(FONT_RESOURCE_PATH)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Classpath resource not found: " + FONT_RESOURCE_PATH);
            }
            Path tempFontFile = Files.createTempFile("java-bank-cjk-", ".ttf");
            Files.copy(inputStream, tempFontFile, StandardCopyOption.REPLACE_EXISTING);
            return tempFontFile;
        } catch (IOException e) {
            log.error("{} 載入中文字型失敗 resource={}", logPrefix, FONT_RESOURCE_PATH, e);
            throw e;
        }
    }

    public static void deleteTempFontFile(Path tempFontFile, Logger log, String logPrefix) {
        if (tempFontFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempFontFile);
        } catch (IOException e) {
            log.warn("{} 清理暫存中文字型失敗 path={}", logPrefix, tempFontFile, e);
        }
    }
}

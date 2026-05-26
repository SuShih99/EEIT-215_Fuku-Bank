package com.javaeasybank.common.config;

import com.openhtmltopdf.util.XRLog;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.logging.Level;

@Configuration
public class OpenHtmlToPdfLoggingConfig {

    @PostConstruct
    public void configureOpenHtmlToPdfLogging() {
        XRLog.setLevel(XRLog.CONFIG, Level.WARNING);
        XRLog.setLevel(XRLog.GENERAL, Level.WARNING);
        XRLog.setLevel(XRLog.INIT, Level.WARNING);
        XRLog.setLevel(XRLog.LOAD, Level.WARNING);
        XRLog.setLevel(XRLog.MATCH, Level.WARNING);
        XRLog.setLevel(XRLog.CASCADE, Level.WARNING);
        XRLog.setLevel(XRLog.XML_ENTITIES, Level.WARNING);
        XRLog.setLevel(XRLog.CSS_PARSE, Level.WARNING);
        XRLog.setLevel(XRLog.LAYOUT, Level.WARNING);
        XRLog.setLevel(XRLog.RENDER, Level.WARNING);
    }
}

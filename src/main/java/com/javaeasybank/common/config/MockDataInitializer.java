package com.javaeasybank.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(prefix = "app.mock-data", name = "enabled", havingValue = "true")
public class MockDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MockDataInitializer.class);
    private static final Pattern SQL_SERVER_GO_SEPARATOR = Pattern.compile("(?im)^\\s*GO\\s*$");

    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;
    private final Environment environment;

    public MockDataInitializer(
            JdbcTemplate jdbcTemplate,
            ResourceLoader resourceLoader,
            Environment environment
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.resourceLoader = resourceLoader;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> scriptLocations = getScriptLocations();
        if (scriptLocations.isEmpty()) {
            log.warn("Mock data initialization is enabled, but app.mock-data.scripts is empty.");
            return;
        }

        for (String scriptLocation : scriptLocations) {
            executeScript(scriptLocation);
        }
    }

    private List<String> getScriptLocations() {
        String configuredScripts = environment.getProperty("app.mock-data.scripts", "");
        return Arrays.stream(configuredScripts.split(","))
                .map(String::trim)
                .filter(script -> !script.isEmpty())
                .toList();
    }

    private void executeScript(String scriptLocation) throws IOException {
        Resource resource = resourceLoader.getResource(scriptLocation);
        if (!resource.exists()) {
            throw new IllegalStateException("Mock data script not found: " + scriptLocation);
        }

        String script = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (script.startsWith("\uFEFF")) {
            script = script.substring(1);
        }

        List<String> batches = Arrays.stream(SQL_SERVER_GO_SEPARATOR.split(script))
                .map(String::trim)
                .filter(batch -> !batch.isEmpty())
                .toList();

        for (String batch : batches) {
            jdbcTemplate.execute(batch);
        }

        log.info("Executed mock data script: {}", scriptLocation);
    }
}

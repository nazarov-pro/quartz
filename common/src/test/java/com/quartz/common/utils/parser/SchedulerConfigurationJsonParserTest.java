package com.quartz.common.utils.parser;

import com.quartz.common.config.SchedulerConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

class SchedulerConfigurationJsonParserTest {
    private static final SchedulerConfigurationJsonParser JSON_PARSER = new SchedulerConfigurationJsonParser();

    @Test
    void parsingTest() {
        final InputStream resourceAsStream = getClass().getClassLoader()
                .getResourceAsStream("config/test-quartz-scheduler.json");
        final SchedulerConfiguration schedulerConfiguration = JSON_PARSER.parse(resourceAsStream);
        Assertions.assertNotNull(schedulerConfiguration);
    }
}

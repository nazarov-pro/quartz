package com.quartz.common.utils.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quartz.common.config.SchedulerConfiguration;
import com.quartz.common.config.SchedulerConfigurationModel;
import lombok.SneakyThrows;

import java.io.InputStream;

public class SchedulerConfigurationJsonParser implements SchedulerConfigurationParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    @Override
    public SchedulerConfiguration parse(final InputStream inputStream) {
        return objectMapper.readValue(inputStream, SchedulerConfigurationModel.class);
    }
}

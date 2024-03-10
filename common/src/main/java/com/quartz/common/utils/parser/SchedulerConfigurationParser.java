package com.quartz.common.utils.parser;

import com.quartz.common.config.SchedulerConfiguration;

import java.io.InputStream;

public interface SchedulerConfigurationParser {
    SchedulerConfiguration parse(final InputStream inputStream);
}

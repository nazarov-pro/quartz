package com.quartz.plain;

import com.quartz.common.scheduler.QuartzScheduler;
import com.quartz.common.utils.parser.SchedulerConfigurationJsonParser;
import org.quartz.impl.StdSchedulerFactory;

import java.time.Duration;

public class Main {
    public static void main(String[] args) {
        final var parser = new SchedulerConfigurationJsonParser();
        final var config = parser.parse(Main.class.getClassLoader().getResourceAsStream("scheduler.json"));
        try (final var scheduler = new QuartzScheduler(StdSchedulerFactory.getDefaultScheduler(), config)) {
            scheduler.start();
            Thread.sleep(Duration.ofMinutes(1).toMillis());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

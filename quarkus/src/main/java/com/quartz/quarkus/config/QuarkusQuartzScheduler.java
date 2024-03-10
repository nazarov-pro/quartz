package com.quartz.quarkus.config;

import com.quartz.common.scheduler.QuartzScheduler;
import com.quartz.common.utils.parser.SchedulerConfigurationJsonParser;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

@ApplicationScoped
public class QuarkusQuartzScheduler {
    @Inject
    Scheduler scheduler;
    @ConfigProperty(name = "app.quartz.config-path")
    String schedulerPath;


    void onStart(@Observes StartupEvent event) throws SchedulerException {
        final var inputStream = getClass().getClassLoader().getResourceAsStream(schedulerPath);
        if (inputStream == null) {
            throw new RuntimeException("Scheduler configuration file not found or not readable: " + schedulerPath);
        }

        final var parser = new SchedulerConfigurationJsonParser();
        final var config = parser.parse(inputStream);
        final QuartzScheduler quartzScheduler = new QuartzScheduler(scheduler, config);
        quartzScheduler.start();
    }

    void onStop(@Observes ShutdownEvent ev) throws SchedulerException {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}

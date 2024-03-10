package com.quartz.spring.config;

import com.quartz.common.scheduler.QuartzScheduler;
import com.quartz.common.utils.parser.SchedulerConfigurationJsonParser;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@ConditionalOnExpression("'${app.quartz-scheduler.enabled}'=='true'")
@RequiredArgsConstructor
public class QuartzConfiguration {
    private final ResourceLoader resourceLoader;

    @Bean(name = "quartzDataSource")
    @QuartzDataSource
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource quartzDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @SneakyThrows
    public SchedulerFactoryBean schedulerFactoryBean(@Qualifier("quartzDataSource") final DataSource dataSource) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        Properties properties = new Properties();
        properties.load(resourceLoader.getResource("classpath:quartz-config.properties").getInputStream());
        factory.setDataSource(dataSource);
        factory.setQuartzProperties(properties);
        return factory;
    }


    @Bean("quartzScheduler")
    @SneakyThrows
    public QuartzScheduler scheduler(@Value("${app.quartz-scheduler.configPath}") final String configPath,
                                     final SchedulerFactoryBean factory) {
        final Scheduler scheduler = factory.getScheduler();
        final Resource resource = resourceLoader.getResource(configPath);
        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("Scheduler configuration file not found or not readable: " + configPath);
        }

        final var parser = new SchedulerConfigurationJsonParser();
        final var config = parser.parse(resource.getInputStream());
        final QuartzScheduler quartzScheduler = new QuartzScheduler(scheduler, config);
        quartzScheduler.start();
        return quartzScheduler;
    }

}

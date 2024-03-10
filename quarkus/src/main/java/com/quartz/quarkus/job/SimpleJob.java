package com.quartz.quarkus.job;

import com.quartz.common.scheduler.QuartzJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public class SimpleJob implements QuartzJob {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("SimpleJob is running");
    }
}

package com.quartz.quarkus.listener;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

@Slf4j
public class EventListener implements JobListener {

    @Override
    public String getName() {
        return "EventListener";
    }


    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        log.info("Job to be executed: {} by {}", context.getJobDetail().getKey(), context.getTrigger().getKey());
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        log.info("Job execution vetoed: {}", context.getJobDetail().getKey());
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        log.info("Job was executed: {}", context.getJobDetail().getKey());
    }
}

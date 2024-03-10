package com.quartz.common.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public abstract class QuartzWithThreadInterruptionJob implements QuartzInterruptibleJob {

    private Thread currentThread;

    public abstract void executeWithThreadInterruption(JobExecutionContext context);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        this.currentThread = Thread.currentThread();
        executeWithThreadInterruption(context);
    }

    @Override
    public void interrupt() {
        if (this.currentThread != null && !this.currentThread.isInterrupted()) {
            log.info("Trying to interrupt the job");
            this.currentThread.interrupt();
        }
    }
}

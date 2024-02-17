package com.quartz.common.scheduler;

import com.quartz.common.config.SchedulerConfiguration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.plugins.interrupt.JobInterruptMonitorPlugin;

import java.util.Date;
import java.util.stream.Collectors;

/**
 * Quartz Scheduler is for initializing job/triggers, starting and stopping gracefully quartz scheduler
 */
@Slf4j
public class QuartzScheduler implements AutoCloseable {
    private Scheduler scheduler;
    private SchedulerConfiguration schedulerConfiguration;


    /**
     * If scheduler is enabled then it will generate jobs -> triggers and start the quartz scheduler
     *
     */
    public void init() throws SchedulerException {
        if (!schedulerConfiguration.enabled()) {
            log.info("Job/trigger initialization and scheduler was disabled");
            return;
        }
        schedulerConfiguration.groups().forEach(
                (jobGroupKey, jobGroup) -> {
                    final var jobGroupName = jobGroup.name().orElse(jobGroupKey);
                    log.info("Job Group: {} with {} jobs will be created", jobGroupName, jobGroup.jobs().size());
                    jobGroup.jobs().forEach((jobKey, job) -> scheduleJob(jobGroupName, jobKey, job));
                }
        );
        scheduler.start();
        log.info("Scheduler started");
    }

    /**
     * It will generate jobs and triggers, then save in the database
     *
     * @param jobGroupName name of the job group
     * @param jobKey       name/key of the job
     * @param job          details of the job
     */
    @SneakyThrows
    private void scheduleJob(String jobGroupName, String jobKey, SchedulerConfiguration.JobGroup.JobDetail job) {
        final var jobDetail = generateJobDetail(jobGroupName, jobKey, job);
        final var triggers = job.triggers().entrySet().stream().map(
                entry -> generateTrigger(jobDetail, entry.getKey(), entry.getValue())
        ).collect(Collectors.toSet());
        log.info("Job: {} with {} triggers will be created or updated.", jobDetail.getKey(), triggers.size());

        scheduler.addJob(jobDetail, true, true);
        scheduler.scheduleJob(jobDetail, triggers, true);
    }

    /**
     * For generating job details
     *
     * @param jobGroupName name of the job group
     * @param jobKey       name/key of the job
     * @param job          details of the job
     * @return new job detail
     */
    private JobDetail generateJobDetail(String jobGroupName, String jobKey, SchedulerConfiguration.JobGroup.JobDetail job) {
        var jobDataMap = new JobDataMap();
        if (job.interruptionEnabled()) {
            jobDataMap.put(JobInterruptMonitorPlugin.AUTO_INTERRUPTIBLE, "true");
        }

        return JobBuilder.newJob(job.jobClass())
                .setJobData(jobDataMap)
                .withIdentity(job.name().orElse(jobKey), jobGroupName)
                .withDescription(job.description().orElse(null))
                .build();
    }

    /**
     * For generating trigger
     *
     * @param jobDetail  job details
     * @param triggerKey name/key of the trigger
     * @param trigger    details of the trigger
     * @return new trigger
     */
    private Trigger generateTrigger(JobDetail jobDetail, String triggerKey, SchedulerConfiguration.JobGroup.JobDetail.Trigger trigger) {
        return TriggerBuilder
                .newTrigger()
                .forJob(jobDetail)
                .endAt(trigger.endDate().map(offsetDateTime -> Date.from(offsetDateTime.toInstant())).orElse(null))
                .startAt(trigger.startDate().map(offsetDateTime -> Date.from(offsetDateTime.toInstant()))
                        .orElseGet(Date::new)) // if the start date is null, it will start immediately
                .withIdentity(trigger.name().orElse(triggerKey), jobDetail.getKey().getGroup())
                .withDescription(trigger.description().orElse(null))
                .withSchedule(
                        CronScheduleBuilder.cronSchedule(trigger.cron())
                                .withMisfireHandlingInstructionDoNothing()
                ).build();
    }

    /**
     * If quartz scheduler is started and currently running then scheduler will wait for the last job to complete and after that it will stop scheduler
     *
     * @throws Exception likely @{SchedulerException}
     */
    @Override
    public void close() throws Exception {
        if (scheduler.isStarted()) {
            final var currentlyExecutingJobs = scheduler.getCurrentlyExecutingJobs().size();
            log.info("Scheduler will wait for {} running job(s) to complete.", currentlyExecutingJobs);
            scheduler.shutdown(true);
            log.info("Scheduler stopped successfully");
        }
    }
}

package com.quartz.common.scheduler;

import com.quartz.common.config.SchedulerConfiguration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.plugins.interrupt.JobInterruptMonitorPlugin;

import java.util.Date;
import java.util.stream.Collectors;

/**
 * Quartz Scheduler is for initializing job/triggers, starting and stopping gracefully quartz scheduler
 * <p>
 *     If scheduler is enabled then it will generate jobs -> triggers and start the quartz scheduler
 *     Cron: <a href="https://productresources.collibra.com/docs/collibra/latest/Content/Cron/co_quartz-cron-syntax.htm">Quartz CRON Syntax</a>
 */
@RequiredArgsConstructor
@Slf4j
public class QuartzScheduler implements AutoCloseable {
    @Getter
    private final Scheduler scheduler;
    private final SchedulerConfiguration schedulerConfiguration;

    /**
     * If scheduler is enabled then it will generate jobs -> triggers and start the quartz scheduler
     */
    public void start() throws SchedulerException {
        if (!schedulerConfiguration.enabled()) {
            log.info("Job/trigger initialization and scheduler was disabled");
            return;
        }
        schedulerConfiguration.groups().forEach(
                (jobGroupKey, jobGroup) -> {
                    final String jobGroupName = jobGroup.name().orElse(jobGroupKey);
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
        try {
            scheduler.scheduleJob(jobDetail, triggers, true);
        } catch (SchedulerException e) {
            log.error("Error occurred while scheduling job: {}", jobDetail.getKey(), e);
        }
    }

    /**
     * For generating job details
     *
     * @param jobGroupName name of the job group
     * @param jobKey       name/key of the job
     * @param job          details of the job
     * @return new job detail
     */
    private JobDetail generateJobDetail(String jobGroupName, String
            jobKey, SchedulerConfiguration.JobGroup.JobDetail job) {
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
    private Trigger generateTrigger(JobDetail jobDetail, String
            triggerKey, SchedulerConfiguration.JobGroup.JobDetail.Trigger trigger) {
        return TriggerBuilder
                .newTrigger()
                .forJob(jobDetail)
                .endAt(trigger.endDate().map(offsetDateTime -> Date.from(offsetDateTime.toInstant())).orElse(null))
                .startAt(trigger.startDate().map(offsetDateTime -> Date.from(offsetDateTime.toInstant()))
                        .orElseGet(Date::new)) // if the start date is null, it will start immediately
                .withIdentity(trigger.name().orElse(triggerKey), jobDetail.getKey().getGroup())
                .withDescription(trigger.description().orElse(null))
                .withSchedule(generate(trigger.schedule()))
                .withPriority(trigger.priority())
                .build();
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

    private ScheduleBuilder<?> generate(SchedulerConfiguration.JobGroup.JobDetail.Trigger.Schedule schedule) {
        return switch (schedule.type()) {
            case CRON -> generate((SchedulerConfiguration.JobGroup.JobDetail.Trigger.CronSchedule) schedule);
            default -> throw new IllegalStateException("Not implemented scheduler type: " + schedule.type());
        };
    }

    private ScheduleBuilder<?> generate(SchedulerConfiguration.JobGroup.JobDetail.Trigger.CronSchedule schedule) {
        final CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder
                .cronSchedule(schedule.expression())
                .inTimeZone(schedule.timezone());

        switch (schedule.misfireInstruction()) {
            case IGNORE -> cronScheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
            case DO_NOTHING -> cronScheduleBuilder.withMisfireHandlingInstructionDoNothing();
            case FIRE_NOW -> cronScheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
        }
        return cronScheduleBuilder;
    }
}

package com.quartz.common.config;

import com.quartz.common.scheduler.QuartzInterruptibleJob;
import com.quartz.common.scheduler.QuartzJob;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * SchedulerConfiguration model contains configuration for scheduler, jobs and triggers
 */
public interface SchedulerConfiguration {
    float MAX_ADDITIONAL_WAIT_TIME_IN_PERCENTAGE = 0.1f;

    /**
     * Contains key and {@link JobGroup} pairs.
     * If the name property was not provided in {@link JobGroup} then key will be used as a name
     *
     * @return all job groups of the service
     */
    Map<String, JobGroup> groups();

    /**
     * If it is enabled then all jobs and triggers will be configured and scheduler will start
     * By default, scheduler is disabled
     *
     * @return if scheduler and job/trigger configuration management is enabled or not
     */
    default boolean enabled() {
        return false;
    }

    /**
     * Timeout in seconds for the jobs (it can be used in job implementation)
     *
     * @return max time out for the job execution
     */
    Duration maxExecutionTime();

    /**
     * It will wait additionally 10% of max timeout (for recovering and failing gracefully)
     *
     * @return max execution time + 10% of max execution time
     */
    default Duration maxWaitTime() {
        return Duration.of(
                (long) (maxExecutionTime().toMillis() * (1 + MAX_ADDITIONAL_WAIT_TIME_IN_PERCENTAGE)),
                ChronoUnit.MILLIS
        );
    }


    /**
     * JobGroup wrap jobs in a single group
     */
    interface JobGroup {
        /**
         * Defines the name of the job group
         * If name is not present then key of the job group will be used in {@link SchedulerConfiguration}.groups
         *
         * @return name of the job group
         */
        Optional<String> name();

        /**
         * Contains key and {@link JobDetail} pairs.
         * If the name property was not provided in {@link JobDetail} then key will be used as a name
         * Use a single job for each of your job which can run independently
         *
         * @return all jobs of the specific job group
         */
        Map<String, JobDetail> jobs();

        /**
         * JobDetail defines detailed information about the job
         */
        interface JobDetail {
            /**
             * Defines the name of the job
             * If name is not present then key of the job will be used in {@link JobGroup}.jobs
             *
             * @return name of the job
             */
            Optional<String> name();

            /**
             * Defines the description of the job (it will also store in the database) regarding what it does etc.
             *
             * @return description about the job
             */
            Optional<String> description();

            /**
             * It defines in which class you have implemented {@link QuartzJob}
             * Note: please note that it should implement {@link QuartzJob} or its children
             *
             * @return class of the job (implementation)
             */
            Class<? extends QuartzJob> jobClass();

            /**
             * Defines if you have implemented and used interruption plugin
             * Please make sure if you have implemented {@link QuartzInterruptibleJob} not {@link QuartzJob} in case you have enabled
             * By default, it is true.
             *
             * @return interruption implemented in job implementation or not
             */
            default boolean interruptionEnabled() {
                return true;
            }

            /**
             * Contains key and {@link Trigger} pairs.
             * If the name property was not provided in {@link Trigger} then key will be used as a name
             *
             * @return all triggers of the specific job
             */
            Map<String, Trigger> triggers();

            /**
             * Trigger details
             */
            interface Trigger {
                /**
                 * Defines the name of the trigger
                 * If name is not present then key of the trigger will be used in {@link JobDetail}.triggers
                 *
                 * @return name of the trigger
                 */
                Optional<String> name();

                /**
                 * Defines the description of the trigger (it will also store in the database) regarding when it will start/end in which periods it will fired etc.
                 *
                 * @return description about the trigger
                 */
                Optional<String> description();

                /**
                 * Defines the cron value of the trigger
                 *
                 * @return cron value
                 */
                String cron();

                /**
                 * It defines when the trigger will finalize and marked as completed.
                 * If end date is null it will run forever
                 *
                 * @return end date of the trigger
                 */
                Optional<OffsetDateTime> endDate();

                /**
                 * It defines when the trigger will start firing jobs.
                 * If it is null, it will start immediately
                 *
                 * @return start date of the trigger
                 */
                Optional<OffsetDateTime> startDate();

            }
        }
    }
}

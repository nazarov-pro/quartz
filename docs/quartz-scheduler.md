# Quartz Scheduler (Trace implementation) #

## Database structure ##

All quartz tables (**QRTZ_** prefix) are available in **scheduler** schema (trace db).

| Table name | Description |
| ---------- | ----------- |
| QRTZ_CALENDARS | Stores Quartz calendar information as blobs |
| QRTZ_CRON_TRIGGERS | Stores cron triggers, including cron expression and time zone information |
| QRTZ_FIRED_TRIGGERS | Stores status information relating to triggers that have fired and the relevant execution information about the related job. |
| QRTZ_PAUSED_TRIGGER_GRPS | Stores the trigger groups that have been paused |
| QRTZ_SCHEDULER_STATE | Stores a few pieces of information about the state of the Scheduler and other Scheduler instances (if used within a cluster) |
| QRTZ_LOCKS | Stores pessimistic lock information for the application (if pessimistic locking is used) |
| QRTZ_JOB_DETAILS | Stores detailed information for every configured Job |
| QRTZ_JOB_LISTENERS | Stores information about configured JobListeners |
| QRTZ_SIMPLE_TRIGGERS | Stores simple triggers, including repeat count, internal, and number of times triggered |
| QRTZ_BLOB_TRIGGERS | Triggers stores as blobs (this is used when Quartz users create their own custom trigger typeswith JDBC, JobStore does not have specific knowledge about how to store instances) |
| QRTZ_TRIGGER_LISTENERS | Stores information about configured triggerListeners |
| QRTZ_TRIGGERS | Stores information about configured triggers |

Note: [Migration File](../infra/docker/quartz_sheduler_ddl.sql)

## Quartz Scheduler #

There are 5 classes in trace scheduler implementation:

- **TraceJob** - The interface to be implemented by classes which represent a 'job' to be performed.
- **TraceInterruptableJob** - The interface to be implemented by classes which represent a 'job' to be performed, also additional interrupt method should be implemented for interrupting job by JobMonitorPlugin.
- **TraceJobWithThreadInterruption** - The abstract class with tread interruption based implementation to be implemented by classes which represent a 'job' to be performed.
- **SchedulerConfiguration** - The interface that will be mapped provided with the configuration in the properties file (more details are available below)
- **TraceQuartzScheduler** - The class has 2 responsibilities, 1- Creating or updating JOBS/TRIGGERS and Managing Quartz Scheduler instance

## Scheduler Configuration ##

All available parameters described below:

| Property name | Group | Mandatory | Type | Default value | Description |
| ------------- | ----- | --------- | ---- | ------------- | ----------- |
| quarkus.quartz.clustered | QUARTZ | [ ] | boolean | false | Enable cluster mode or not. If enabled make sure to set the appropriate cluster properties |
| quarkus.quartz.cluster-checkin-interval | QUARTZ | [ ] | int64 | 15000 | The frequency (in milliseconds) at which the scheduler instance checks-in with other instances of the cluster |
| quarkus.quartz.instance-name | QUARTZ | [x] | string | QuartzScheduler | name of the instance, should be the same for each service |
| quarkus.quartz.store-type | QUARTZ | [ ] | ram, jdbc-tx, jdbc-cmt | NONE | The type of store to use. When using StoreType#JDBC_CMT or StoreType#JDBC_TX configuration values make sure that you have the datasource configured |
| quarkus.quartz.start-mode | QUARTZ | [ ] | normal, forced, halted | normal | Scheduler can be started in different modes: normal, forced or halted |
| quarkus.quartz.datasource | QUARTZ | [ ] | string | default | name of the datasource |
| quarkus.quartz.plugins.job-interrupter.class | QUARTZ | [ ] | class |  | class name of the plugin (fixed: **org.quartz.plugins.interrupt.JobInterruptMonitorPlugin**) |
| quarkus.quartz.plugins.job-interrupter.properties.defaultMaxRunTime | QUARTZ | [ ] | int64 |  | time limit for the execution |
| quarkus.quartz.plugins.trigger-history.class | QUARTZ | [ ] | class |  | class name of the plugin (fixed: **org.quartz.plugins.history.LoggingTriggerHistoryPlugin**) |
| quarkus.quartz.plugins.trigger-history.properties.triggerFiredMessage | QUARTZ | [ ] | string with format |  | Log format when trigger started: Trigger {1}.{0} fired job {6}.{5} at: {4, date, HH:mm:ss} |
| quarkus.quartz.plugins.trigger-history.properties.triggerCompleteMessage | QUARTZ | [ ] | string with format |  | Log format when trigger completed: Trigger {1}.{0} completed firing job {6}.{5} at {4, date, HH:mm:ss} |
| trace.app.scheduler.enabled | TRACE-QUARTZ | [x] | boolean | false | If it is enabled then all jobs and triggers will be configured and scheduler will start |
| trace.app.scheduler.max-execution-time-in-millis | TRACE-QUARTZ | [x] | int32 | reference to **quarkus.quartz.plugins.job-interrupter.properties.defaultMaxRunTime** property | max execution time (when to use custom interruption mechanism) |
| trace.app.scheduler.groups.[group-name].name | TRACE-QUARTZ | [ ] | string | it will use [group-name] | name of the job group |
| trace.app.scheduler.groups.[group-name].jobs.[job-name].name | TRACE-QUARTZ | [x] | string | it will use [job-name] | name of the job |
| trace.app.scheduler.groups.[group-name].jobs.[job-name].description | TRACE-QUARTZ | [ ] | string |  | description of the job |
| trace.app.scheduler.groups.[group-name].jobs.[job-name].interruption-enabled | TRACE-QUARTZ | [ ] | string | false | if it is enabled then JobInterruptPlugin will be activated for this job |
| trace.app.scheduler.groups.[group-name].jobs.[job-name].class | TRACE-QUARTZ | [x] | class |  | full class path of the job implementation |
| trace.app.scheduler.groups.[group-name].jobs.[job-name].triggers.[trigger-name].name | TRACE-QUARTZ | [ ] | string | it will use [trigger-name] | name of the trigger |
| trace.app.scheduler.groups.[group-name].jobs.[job-name].triggers.[trigger-name].description | TRACE-QUARTZ | [ ] | string |  | description of the trigger |
| trace.app.scheduler.groups.[group-name].jobs.[job-name].triggers.[trigger-name].cron | TRACE-QUARTZ | [x] | string as cron |  | cron definition of the trigger |
| trace.app.scheduler.groups.[group-name].jobs.[job-name].triggers.[trigger-name].start-date | TRACE-QUARTZ | [ ] | string as date(yyyy-MM-dd'T'HH:mm:ssZ) | now | start date of the trigger, if nothing provided it will start immediately. example: 2022-05-23T00:00:00+0400 |
| trace.app.scheduler.groups.[group-name].jobs.[job-name].triggers.[trigger-name].end-date | TRACE-QUARTZ | [ ] | string as date(yyyy-MM-dd'T'HH:mm:ssZ) | null | end date of the trigger, if nothing provided it will run forever. example: 2022-05-23T00:00:00+0400 |

Note: All quartz and trace version of quartz related properties are described in QUARTZ and TRACE-QUARTZ groups respectively.

## How to implement trace quartz scheduler in apps ##

All steps are described below for implementing trace scheduler:

1. Make sure you have included **scheduler** schema and its migration scripts to flyway configurations
2. Create dedicated configuration for scheduler and import this configuration from main application.properties
3. Make sure you have created a dedicated datasource (with current schema property also set in jdbc url as Quartz fetch tables without specifying schema explicitly) for quartz (and datasource property)
4. Check scheduler name property is correct (it will work only once at the same time for the instances which are running with the same scheduler name)
5. Make sure you have implemented **TraceJob**, **TraceInterruptableJob** or **TraceJobWithThreadInterruption** any of them and **@DisallowConcurrentExecution** annotation also added to class
6. Try to provide all details with clear description about the job and the trigger
7. Make sure you have disabled scheduler in test environment
# Quartz Scheduler #

## Database structure ##

All quartz tables (**QRTZ_** prefix) are available in **scheduler** schema.

| Table name               | Description                                                                                                                                                                      |
|--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| QRTZ_CALENDARS           | Stores Quartz calendar information as blobs                                                                                                                                      |
| QRTZ_CRON_TRIGGERS       | Stores cron triggers, including cron expression and time zone information                                                                                                        |
| QRTZ_FIRED_TRIGGERS      | Stores status information relating to triggers that have fired and the relevant execution information about the related job.                                                     |
| QRTZ_PAUSED_TRIGGER_GRPS | Stores the trigger groups that have been paused                                                                                                                                  |
| QRTZ_SCHEDULER_STATE     | Stores a few pieces of information about the state of the Scheduler and other Scheduler instances (if used within a cluster)                                                     |
| QRTZ_LOCKS               | Stores pessimistic lock information for the application (if pessimistic locking is used)                                                                                         |
| QRTZ_JOB_DETAILS         | Stores detailed information for every configured Job                                                                                                                             |
| QRTZ_JOB_LISTENERS       | Stores information about configured JobListeners                                                                                                                                 |
| QRTZ_SIMPLE_TRIGGERS     | Stores simple triggers, including repeat count, internal, and number of times triggered                                                                                          |
| QRTZ_BLOB_TRIGGERS       | Triggers stores as blobs (this is used when Quartz users create their own custom trigger typeswith JDBC, JobStore does not have specific knowledge about how to store instances) |
| QRTZ_TRIGGER_LISTENERS   | Stores information about configured triggerListeners                                                                                                                             |
| QRTZ_TRIGGERS            | Stores information about configured triggers                                                                                                                                     |

Note: [Migration File](../infra/docker/quartz_sheduler_ddl.sql)

## Quartz Scheduler #

Class structure

- [QuartzJob.java](..%2Fcommon%2Fsrc%2Fmain%2Fjava%2Fcom%2Fquartz%2Fcommon%2Fscheduler%2FQuartzJob.java) - The
  interface to be implemented by classes which represent a 'job' to be performed.
- [QuartzInterruptibleJob.java](..%2Fcommon%2Fsrc%2Fmain%2Fjava%2Fcom%2Fquartz%2Fcommon%2Fscheduler%2FQuartzInterruptibleJob.java) -
  The interface to be implemented by classes which represent a 'job' to be performed, also additional interrupt
  method should be implemented for interrupting job by JobMonitorPlugin.
- [QuartzWithThreadInterruptionJob.java](..%2Fcommon%2Fsrc%2Fmain%2Fjava%2Fcom%2Fquartz%2Fcommon%2Fscheduler%2FQuartzWithThreadInterruptionJob.java) -
  The abstract class with tread interruption based implementation to be implemented by classes which represent a '
  job' to be performed.
- [SchedulerConfiguration.java](..%2Fcommon%2Fsrc%2Fmain%2Fjava%2Fcom%2Fquartz%2Fcommon%2Fconfig%2FSchedulerConfiguration.java) -
  The interface that will be mapped provided with the configuration in the properties file (more details are
  available below)
- [QuartzScheduler.java](..%2Fcommon%2Fsrc%2Fmain%2Fjava%2Fcom%2Fquartz%2Fcommon%2Fscheduler%2FQuartzScheduler.java)-
  The
  class has 2 responsibilities, 1- Creating or updating JOBS/TRIGGERS and Managing Quartz Scheduler instance

## Scheduler Configuration ##

Describe all quartz parameters in quartz.properties file is enough for quartz to use those properties for more details
about
properties [All Configurations](https://www.quartz-scheduler.org/documentation/quartz-2.1.7/configuration/).

| Property name                                                                              | Mandatory | Type                                   | Default value                                                                             | Description                                                                                                                          |
|--------------------------------------------------------------------------------------------|-----------|----------------------------------------|-------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| enabled                                                                                    | [x]       | boolean                                | false                                                                                     | If it is enabled then all jobs and triggers will be configured and scheduler will start                                              |
| maxExecutionTime                                                                           | [x]       | string(java.time.Duration format)      | reference to **org.quartz.plugins.job-interrupter.properties.defaultMaxRunTime** property | max execution time (when to use custom interruption mechanism)                                                                       |
| groups.[group-name].name                                                                   | [ ]       | string                                 | it will use [group-name]                                                                  | name of the job group                                                                                                                |
| groups.[group-name].jobs.[job-name].name                                                   | [x]       | string                                 | it will use [job-name]                                                                    | name of the job                                                                                                                      |
| groups.[group-name].jobs.[job-name].description                                            | [ ]       | string                                 |                                                                                           | description of the job                                                                                                               |
| groups.[group-name].jobs.[job-name].interruption-enabled                                   | [ ]       | string                                 | false                                                                                     | if it is enabled then JobInterruptPlugin will be activated for this job                                                              |
| groups.[group-name].jobs.[job-name].class                                                  | [x]       | class                                  |                                                                                           | full class path of the job implementation                                                                                            |
| groups.[group-name].jobs.[job-name].triggers.[trigger-name].name                           | [ ]       | string                                 | it will use [trigger-name]                                                                | name of the trigger                                                                                                                  |
| groups.[group-name].jobs.[job-name].triggers.[trigger-name].description                    | [ ]       | string                                 |                                                                                           | description of the trigger                                                                                                           |
| groups.[group-name].jobs.[job-name].triggers.[trigger-name].schedule                       | [x]       | type of the scedule implementation     |                                                                                           | schedule definition of the trigger                                                                                                   |
| groups.[group-name].jobs.[job-name].triggers.[trigger-name].schedule(type=CRON).expression | [x]       | expression of a cron type scheduler    |                                                                                           | expression of a cron type scheduler [Syntax](https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html) |
| groups.[group-name].jobs.[job-name].triggers.[trigger-name].start-date                     | [ ]       | string as date(yyyy-MM-dd'T'HH:mm:ssZ) | now                                                                                       | start date of the trigger, if nothing provided it will start immediately. example: 2022-05-23T00:00:00+0400                          |
| groups.[group-name].jobs.[job-name].triggers.[trigger-name].end-date                       | [ ]       | string as date(yyyy-MM-dd'T'HH:mm:ssZ) | null                                                                                      | end date of the trigger, if nothing provided it will run forever. example: 2022-05-23T00:00:00+0400                                  |

## How to implement quartz scheduler in apps ##

All steps are described below for implementing quartz scheduler:

1. Make sure all db migration scripts are executed
2. Based on the implementation please follow the rules by framework
3. Make sure you have implemented **QuartzJob**, **QuartzInterruptibleJob** or **QuartzWithThreadInterruptionJob** any
   of
   them and **@DisallowConcurrentExecution** annotation also added to class
4. Try to provide all details with clear description about the job and the trigger, so it will make maintenance easier
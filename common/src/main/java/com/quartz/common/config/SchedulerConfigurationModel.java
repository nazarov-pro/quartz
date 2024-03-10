package com.quartz.common.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.quartz.common.scheduler.QuartzJob;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchedulerConfigurationModel implements SchedulerConfiguration {
    private Map<String, JobGroupModel> groups;
    private Boolean enabled;
    private String maxExecutionDuration;
    private String maxWaitDuration;

    @Override
    public Map<String, JobGroupModel> groups() {
        return this.groups;
    }

    @Override
    public Duration maxExecutionDuration() {
        return Duration.parse(this.maxExecutionDuration);
    }

    @Override
    public boolean enabled() {
        if (this.enabled == null) {
            return SchedulerConfiguration.super.enabled();
        }
        return this.enabled;
    }

    @Override
    public Duration maxWaitDuration() {
        if (this.maxWaitDuration == null) {
            return SchedulerConfiguration.super.maxWaitDuration();
        }
        return Duration.parse(this.maxWaitDuration);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class JobGroupModel implements JobGroup {
        private String name;
        private Map<String, JobDetailModel> jobs;

        @Override
        public Optional<String> name() {
            return Optional.ofNullable(this.name);
        }

        @Override
        public Map<String, JobDetailModel> jobs() {
            return this.jobs;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        @Data
        public static class JobDetailModel implements JobDetail {
            private String name;
            private String description;
            private String jobClass;
            private Boolean interruptionEnabled;
            private Map<String, TriggerModel> triggers;

            @Override
            public Optional<String> name() {
                return Optional.ofNullable(this.name);
            }

            @Override
            public Optional<String> description() {
                return Optional.ofNullable(this.description);
            }

            @Override
            @SneakyThrows
            public Class<? extends QuartzJob> jobClass() {
                final Class<?> jobClass = Class.forName(this.jobClass);
                if (QuartzJob.class.isAssignableFrom(jobClass)) {
                    //noinspection unchecked
                    return (Class<? extends QuartzJob>) jobClass;
                }
                throw new IllegalArgumentException("Job class should implement " + QuartzJob.class.getName());
            }

            @Override
            public boolean interruptionEnabled() {
                if (this.interruptionEnabled == null) {
                    return JobDetail.super.interruptionEnabled();
                }
                return this.interruptionEnabled;
            }

            @Override
            public Map<String, TriggerModel> triggers() {
                return this.triggers;
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            @Data
            public static class TriggerModel implements Trigger {
                private String name;
                private String description;
                private ScheduleModel schedule;
                private String startDate;
                private String endDate;
                private Integer priority;

                @Override
                public Optional<String> name() {
                    return Optional.ofNullable(this.name);
                }

                @Override
                public Optional<String> description() {
                    return Optional.ofNullable(this.description);
                }

                @Override
                public ScheduleModel schedule() {
                    return this.schedule;
                }

                @Override
                public Optional<OffsetDateTime> endDate() {
                    return Optional.ofNullable(this.endDate).map(OffsetDateTime::parse);
                }

                @Override
                public Optional<OffsetDateTime> startDate() {
                    return Optional.ofNullable(this.startDate).map(OffsetDateTime::parse);
                }

                @Override
                public int priority() {
                    if (this.priority == null) {
                        return Trigger.super.priority();
                    }
                    return this.priority;
                }

                @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
                @JsonSubTypes({
                        @JsonSubTypes.Type(value = CronScheduleModel.class, name = "CRON"),
                })
                public abstract static class ScheduleModel implements Schedule {
                }


                @JsonIgnoreProperties(ignoreUnknown = true)
                @Data
                @EqualsAndHashCode(callSuper = false)
                public static class CronScheduleModel extends ScheduleModel implements CronSchedule {
                    private String expression;
                    private TimeZone timezone;
                    private MisfireInstructions misfireInstruction;

                    @Override
                    public String expression() {
                        return expression;
                    }

                    @Override
                    public TimeZone timezone() {
                        if (this.timezone == null) {
                            return CronSchedule.super.timezone();
                        }
                        return this.timezone;
                    }

                    @Override
                    public MisfireInstructions misfireInstruction() {
                        if (this.misfireInstruction == null) {
                            return CronSchedule.super.misfireInstruction();
                        }
                        return this.misfireInstruction;
                    }
                }
            }
        }
    }
}

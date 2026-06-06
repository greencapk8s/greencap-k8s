package io.greencap.k8s.kubernetes.dto;

public record CronJobInfo(
        String name,
        String namespace,
        String schedule,
        boolean suspended,
        int active,
        String lastScheduleTime,
        String age
) {}

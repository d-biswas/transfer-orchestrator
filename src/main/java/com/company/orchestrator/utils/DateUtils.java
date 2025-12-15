package com.company.orchestrator.utils;

import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class DateUtils {
    /**
     * Converts a LocalDate to the start of the day (00:00:00) in UTC.
     */
    public static Instant toStartOfDayUtc(LocalDate date) {
        if (date != null) {
            return date.atStartOfDay(ZoneOffset.UTC).toInstant();
        }
        return null;
    }

    /**
     * Converts a LocalDate to the end of the day (23:59:59.999999999) in UTC.
     */
    public static Instant toEndOfDayUtc(LocalDate date) {
        if (date != null) {
            return date.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();
        }
        return null;
    }
}

package com.campusbill.app.chat.ai.assistant;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** 本地时区下的周一至周日，含首尾两天。 */
public final class WeekRange {

    public final LocalDate start;
    public final LocalDate end;

    private WeekRange(LocalDate start, LocalDate end) {
        this.start = start;
        this.end = end;
    }

    public static WeekRange of(LocalDate today) {
        LocalDate start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new WeekRange(start, start.plusDays(6));
    }

    public boolean contains(LocalDate date) {
        return date != null && !date.isBefore(start) && !date.isAfter(end);
    }
}

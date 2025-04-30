package com.nutria.app.utilities;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;


@Component
public class DateUtils {
    public Date getFirstDayOfWeek(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate firstDayOfWeek = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return java.sql.Date.valueOf(firstDayOfWeek);
    }

    public Date getLastDayOfWeek(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate lastDayOfWeek = localDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return java.sql.Date.valueOf(lastDayOfWeek);
    }

    public Date getFirstDayOfMonth(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate firstDayOfMonth = localDate.with(TemporalAdjusters.firstDayOfMonth());
        return java.sql.Date.valueOf(firstDayOfMonth);
    }

    public Date getLastDayOfMonth(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate lastDayOfMonth = localDate.with(TemporalAdjusters.lastDayOfMonth());
        return java.sql.Date.valueOf(lastDayOfMonth);
    }
}

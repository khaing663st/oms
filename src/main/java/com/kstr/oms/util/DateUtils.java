package com.kstr.oms.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    private DateUtils() {
    }

    public static String getCurrentUTCDateTime() {
        ZonedDateTime zonedDateTime=ZonedDateTime.now(ZoneId.of("UTC"));
        DateTimeFormatter formatter=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return zonedDateTime.format(formatter);
    }

    public static String toStartOfDay(String date) {
        return date + " 00:00:00";
    }

    public static String toEndOfDay(String date) {
        return date + " 23:59:59";
    }
}

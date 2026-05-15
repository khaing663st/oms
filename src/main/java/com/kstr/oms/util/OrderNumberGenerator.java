package com.kstr.oms.util;

import lombok.experimental.UtilityClass;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class OrderNumberGenerator {

    private static final String ORDER_PREFIX = "ORD";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmssSSS");

    public static String generate() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        String dateStr = now.format(DATE_FORMATTER);
        String timeStr = now.format(TIME_FORMATTER);
        return ORDER_PREFIX + "-" + dateStr + "-" + timeStr;
    }
}

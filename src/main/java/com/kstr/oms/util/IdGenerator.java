package com.kstr.oms.util;

import lombok.experimental.UtilityClass;

import java.time.Year;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class IdGenerator {

    private static final String USER_PREFIX      = "U";
    private static final String ORDER_PREFIX     = "O";
    private static final String ORDER_ITEM_PREFIX = "OI";

    public static String generateUserId() {
        return generate(USER_PREFIX);
    }

    public static String generateOrderId() {
        return generate(ORDER_PREFIX);
    }

    public static String generateOrderItemId() {
        return generate(ORDER_ITEM_PREFIX);
    }

    private static String generate(String prefix) {
        String yy = twoDigitYear();
        char letter = randomLetter();
        String number = randomFiveDigits();
        return prefix + "-" + yy + "-" + letter + number;
    }

    private static String twoDigitYear() {
        int year = Year.now().getValue() % 100;
        return String.format("%02d", year);
    }

    private static char randomLetter() {
        return (char) ('A' + ThreadLocalRandom.current().nextInt(26));
    }

    private static String randomFiveDigits() {
        int n = ThreadLocalRandom.current().nextInt(1, 100_000); // 1–99999
        return String.format("%05d", n);
    }
}


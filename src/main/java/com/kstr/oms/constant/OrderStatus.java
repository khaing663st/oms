package com.kstr.oms.constant;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING(1, "Pending"),
    PROCESSING(2, "Processing"),
    SHIPPED(3, "Shipped"),
    DELIVERED(4, "Delivered"),
    COMPLETED(5, "Completed"),
    CANCELLED(6, "Cancelled");

    private final int code;
    private final String displayName;

    OrderStatus(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static OrderStatus fromCode(int code) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid OrderStatus code ::: " + code);
    }
}


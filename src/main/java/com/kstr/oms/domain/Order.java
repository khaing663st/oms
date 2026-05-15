package com.kstr.oms.domain;

import com.kstr.oms.constant.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    private String pk; // "USER#{userId}"
    private String sk; // "ORDER#{orderId}"
    private String entityType; // ORDER

    // UserOrderDateIndex (GSI1)
    private String gsi1pk; // "USER#{userId}"
    private String gsi1sk; // "ORDER_DATE#{createdAt}"

    // UserOrderStatusIndex (GSI2)
    private String gsi2pk; // "USER#{userId}"
    private String gsi2sk; // "ORDER_STATUS#{status}"

    // OrderNumberIndex (LSI1)
    private String lsi1sk; // "ORDER_NUMBER#{orderNumber}"

    private String orderId;
    private String userId;
    private String orderNumber;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String paymentMethod;

    // Dates for each order status
    private String createdAt; // OrderDate
    private String updatedAt;
    private String processingAt;
    private String shippedAt;
    private String completedAt;
    private String deliveredAt;
    private String cancelledAt;

    private List<OrderItem> items;
}

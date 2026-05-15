package com.kstr.oms.dto;

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
public class OrderDTO {
    private String orderId;
    private String userId;
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private List<OrderItemDTO> items;

    private String createdAt;
    private String updatedAt;
    private String processingAt;
    private String shippedAt;
    private String completedAt;
    private String deliveredAt;
    private String cancelledAt;
}

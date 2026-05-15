package com.kstr.oms.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    private String pk; // "ORDER#{orderId}"
    private String sk; // "ITEM#{itemId}"
    private String entityType; // "ORDER_ITEM"

    // UserProductNameIndex (GSI3)
    private String gsi3pk; // "USER#{userId}"
    private String gsi3sk; // "PROD_NAME#{productName}"

    private String itemId; // UUID
    private String userId;
    private String orderId;
    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String createdAt;
}

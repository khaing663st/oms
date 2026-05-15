package com.kstr.oms.repository;

import com.kstr.oms.domain.OrderItem;

import java.util.List;

public interface OrderItemRepository {

    List<OrderItem> saveBatchOrderItems(List<OrderItem> orderItems);

    List<OrderItem> findByOrderId(String orderId);

    void deleteByOrderId(String orderId);
}

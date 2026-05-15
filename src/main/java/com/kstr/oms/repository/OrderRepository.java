package com.kstr.oms.repository;

import com.kstr.oms.constant.OrderStatus;
import com.kstr.oms.domain.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> updateOrderStatus(String userId, String orderId, OrderStatus newStatus);

    boolean delete(String userId, String orderId);

    Optional<Order> findByUserIdAndOrderId(String userId, String orderId);

    List<Order> findByUserId(String userId);

    List<Order> findByUserIdAndOrderStatus(String userId, OrderStatus status);

    List<Order> findByUserIdAndOrderDate(String userId, String fromDate, String toDate);

    List<Order> findByUserIdAndProductName(String userId, String productName);

    Optional<Order> findByUserIdAndOrderNumber(String userId, String orderNumber);
}

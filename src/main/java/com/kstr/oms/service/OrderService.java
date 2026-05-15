package com.kstr.oms.service;

import com.kstr.oms.dto.OrderDTO;

import java.util.List;
import java.util.Optional;

public interface OrderService {

    String saveOrder(OrderDTO orderDTO);

    Optional<OrderDTO> updateOrderStatus(String userId, String orderId, String status);

    boolean deleteOrder(String userId, String orderId);

    List<OrderDTO> getOrdersByUserId(String userId);

    Optional<OrderDTO> getOrdersByUserIdAndOrderId(String userId, String orderId);

    List<OrderDTO> getOrdersByUserIdAndOrderStatus(String userId, String status);

    List<OrderDTO> getOrdersByUserIdAndOrderDate(String userId, String fromDate, String toDate);

    List<OrderDTO> getOrdersByUserIdAndProductName(String userId, String productName);

    Optional<OrderDTO> getOrderByUserIdAndOrderNumber(String userId, String orderNumber);

}

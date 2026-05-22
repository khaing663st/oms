package com.kstr.oms.service.impl;

import com.kstr.oms.constant.OrderStatus;
import com.kstr.oms.domain.Order;
import com.kstr.oms.dto.OrderDTO;
import com.kstr.oms.mapper.OrderMapper;
import com.kstr.oms.repository.OrderRepository;
import com.kstr.oms.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    private final OrderMapper orderMapper;

    @Override
    public String saveOrder(OrderDTO orderDTO) {
        try {
            Order order = orderMapper.toEntity(orderDTO);
            Order savedOrder = orderRepository.save(order);
            log.info("Order created — orderId ::: {}", savedOrder.getOrderId());
            return savedOrder.getOrderId();
        } catch (Exception e) {
            log.error("Error creating order ::: ", e);
            throw e;
        }
    }

    @Override
    public Optional<OrderDTO> updateOrderStatus(String userId, String orderId, String status) {
        try {
            OrderStatus newStatus = OrderStatus.fromCode(Integer.parseInt(status));
            Optional<Order> updated = orderRepository.updateOrderStatus(userId, orderId, newStatus);
            log.info("Order status updated — userId: {}, orderId: {}, status: {}", userId, orderId, status);
            return updated.map(orderMapper::toDTO);
        } catch (IllegalArgumentException e) {
            log.error("Invalid order status ::: {}", status);
            throw new IllegalArgumentException("Invalid order status ::: " + status, e);
        } catch (ConditionalCheckFailedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating order status ::: ", e);
            throw e;
        }
    }

    @Override
    public boolean deleteOrder(String userId, String orderId) {
        try {
            boolean deleted = orderRepository.delete(userId, orderId);
            log.info("Order deleted — userId: {}, orderId: {}", userId, orderId);
            return deleted;
        } catch (Exception e) {
            log.error("Error deleting order ::: ", e);
            throw e;
        }
    }

    @Override
    public List<OrderDTO> getOrdersByUserId(String userId) {
        try {
            List<Order> orders = orderRepository.findByUserId(userId);
            log.info("Found {} orders for userId: {}", orders.size(), userId);
            return orders.stream().map(orderMapper::toDTO).toList();
        } catch (Exception e) {
            log.error("Error retrieving orders by userId ::: ", e);
            throw e;
        }
    }

    @Override
    public Optional<OrderDTO> getOrdersByUserIdAndOrderId(String userId, String orderId) {
        try {
            Optional<Order> order = orderRepository.findByUserIdAndOrderId(userId, orderId);
            log.info("Order lookup — userId: {}, orderId: {}, found: {}", userId, orderId, order.isPresent());
            return order.map(orderMapper::toDTO);
        } catch (Exception e) {
            log.error("Error retrieving order by userId and orderId ::: ", e);
            throw e;
        }
    }

    @Override
    public List<OrderDTO> getOrdersByUserIdAndOrderStatus(String userId, String status) {
        try {
            OrderStatus orderStatus = OrderStatus.fromCode(Integer.parseInt(status));
            List<Order> orders = orderRepository.findByUserIdAndOrderStatus(userId, orderStatus);
            log.info("Found {} orders for userId: {} with status: {}", orders.size(), userId, status);
            return orders.stream().map(orderMapper::toDTO).toList();
        } catch (Exception e) {
            log.error("Error retrieving orders by userId and status ::: ", e);
            throw e;
        }
    }

    @Override
    public List<OrderDTO> getOrdersByUserIdAndProductName(String userId, String productName) {
        try {
            List<Order> orders=orderRepository.findByUserIdAndProductName(userId, productName);
            log.info("Found {} orders for userId: {} with productName: {}", orders.size(), userId, productName);
            return orders.stream().map(orderMapper::toDTO).toList();
        } catch (Exception e) {
            log.error("Error retrieving orders by userId and productName ::: ", e);
            throw e;
        }
    }

    @Override
    public Optional<OrderDTO> getOrderByUserIdAndOrderNumber(String userId, String orderNumber) {
        try {
            Optional<Order> order=orderRepository.findByUserIdAndOrderNumber(userId, orderNumber);
            log.info("Order lookup — userId: {}, orderNumber: {}, found: {}", userId, orderNumber, order.isPresent());
            return order.map(orderMapper::toDTO);
        } catch (Exception e) {
            log.error("Error retrieving order by userId and orderNumber ::: ", e);
            throw e;
        }
    }

    @Override
    public List<OrderDTO> getOrdersByUserIdAndOrderDate(String userId, String fromDate, String toDate) {
        try {
            List<Order> orders=orderRepository.findByUserIdAndOrderDate(userId, fromDate, toDate);
            log.info("Found {} orders for userId: {} between dates: {} and {}", orders.size(), userId, fromDate, toDate);
            return orders.stream().map(orderMapper::toDTO).toList();
        } catch (Exception e) {
            log.error("Error retrieving orders by userId and date range ::: ", e);
            throw e;
        }
    }
}

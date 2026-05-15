package com.kstr.oms.controller;

import com.kstr.oms.dto.OrderDTO;
import com.kstr.oms.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<String> saveOrder(@RequestBody OrderDTO orderDTO) {
        try {
            String orderId = orderService.saveOrder(orderDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(orderId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/user/{userId}/order/{orderId}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable String userId,
            @PathVariable String orderId,
            @RequestParam String status) {
        try {
            Optional<OrderDTO> updatedOrder = orderService.updateOrderStatus(userId, orderId, status);
            return updatedOrder.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/user/{userId}/order/{orderId}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable String userId,
            @PathVariable String orderId) {
        try {
            boolean deleted = orderService.deleteOrder(userId, orderId);
            return deleted
                    ? ResponseEntity.noContent().build()
                    : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error deleting order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDTO>> getOrdersByUserId(@PathVariable String userId) {
        try {
            List<OrderDTO> orders = orderService.getOrdersByUserId(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error retrieving orders by userId", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{userId}/order/{orderId}")
    public ResponseEntity<OrderDTO> getOrdersByUserIdAndOrderId(
            @PathVariable String userId,
            @PathVariable String orderId) {
        try {
            Optional<OrderDTO> order = orderService.getOrdersByUserIdAndOrderId(userId, orderId);
            return order.map(ResponseEntity::ok)
                        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } catch (Exception e) {
            log.error("Error retrieving order by userId and orderId", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<List<OrderDTO>> getOrdersByUserIdAndOrderStatus(
            @PathVariable String userId,
            @PathVariable String status) {
        try {
            List<OrderDTO> orders = orderService.getOrdersByUserIdAndOrderStatus(userId, status);
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException e) {
            log.error("Invalid order status: {}", status);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error retrieving orders by userId and status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{userId}/product")
    public ResponseEntity<List<OrderDTO>> getOrdersByUserIdAndProductName(
            @PathVariable String userId,
            @RequestParam String productName) {
        try {
            List<OrderDTO> orders = orderService.getOrdersByUserIdAndProductName(userId, productName);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error retrieving orders by userId and productName", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{userId}/date")
    public ResponseEntity<List<OrderDTO>> getOrdersByUserIdAndOrderDate(
            @PathVariable String userId,
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        try {
            List<OrderDTO> orders=orderService.getOrdersByUserIdAndOrderDate(userId, fromDate, toDate);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error retrieving orders by userId and date range", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{userId}/orderNumber/{orderNumber}")
    public ResponseEntity<OrderDTO> getOrderByUserIdAndOrderNumber(
            @PathVariable String userId,
            @PathVariable String orderNumber) {
        try {
            Optional<OrderDTO> order=orderService.getOrderByUserIdAndOrderNumber(userId, orderNumber);
            return order.map(ResponseEntity::ok)
                    .orElseGet(()->ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } catch (Exception e) {
            log.error("Error retrieving order by userId and orderNumber", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

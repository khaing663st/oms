package com.kstr.oms.repository.impl;

import com.kstr.oms.constant.CommonConstants.OrderColumn;
import com.kstr.oms.constant.CommonConstants.OrderItemColumn;
import com.kstr.oms.constant.OrderStatus;
import com.kstr.oms.domain.Order;
import com.kstr.oms.domain.OrderItem;
import com.kstr.oms.repository.OrderItemRepository;
import com.kstr.oms.repository.OrderRepository;
import com.kstr.oms.util.DateUtils;
import com.kstr.oms.util.DynamoDBKeyBuilder;
import com.kstr.oms.util.IdGenerator;
import com.kstr.oms.util.OrderNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.kstr.oms.util.CommonUtils.*;
@Slf4j
@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final DynamoDbClient dynamoDbClient;

    private final DynamoDBKeyBuilder dynamoDBKeyBuilder;

    private final OrderItemRepository orderItemRepository;

    @Override
    public Order save(Order order) {
        try {
            String currentUTCDateTime = DateUtils.getCurrentUTCDateTime();

            order.setOrderId(IdGenerator.generateOrderId());
            order.setOrderNumber(OrderNumberGenerator.generate());
            order.setEntityType(DynamoDBKeyBuilder.ENTITY_TYPE_ORDER);
            order.setStatus(OrderStatus.PENDING);
            order.setCreatedAt(currentUTCDateTime);

            order.setPk(DynamoDBKeyBuilder.buildOrderPK(order.getUserId()));
            order.setSk(DynamoDBKeyBuilder.buildOrderSK(order.getOrderId()));

            order.setGsi1pk(DynamoDBKeyBuilder.buildOrderGSI1PK(order.getUserId()));
            order.setGsi1sk(DynamoDBKeyBuilder.buildOrderGSI1SK(currentUTCDateTime));

            order.setGsi2pk(DynamoDBKeyBuilder.buildOrderGSI2PK(order.getUserId()));
            order.setGsi2sk(DynamoDBKeyBuilder.buildOrderGSI2SK(String.valueOf(order.getStatus().getCode())));

            order.setLsi1sk(DynamoDBKeyBuilder.buildOrderLSI1SK(order.getOrderNumber()));

            List<OrderItem> orderItems = order.getItems() != null ? order.getItems() : new ArrayList<>();
            orderItems.forEach(item -> {
                item.setOrderId(order.getOrderId());
                item.setUserId(order.getUserId());
            });

            // Save items as separate records
            orderItemRepository.saveBatchOrderItems(orderItems);

            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(dynamoDBKeyBuilder.getTableName())
                    .item(toAttributeMap(order))
                    .build());


            log.info("Order saved — orderId: {}, orderNumber: {}, userId ::: {}", order.getOrderId(), order.getOrderNumber(), order.getUserId());
            return order;
        } catch (Exception e) {
            log.error("Error saving order ::: ", e);
            throw e;
        }
    }

    @Override
    public Optional<Order> updateOrderStatus(String userId, String orderId, OrderStatus newStatus) {
        try {
            String currentUTCDateTime = DateUtils.getCurrentUTCDateTime();

            StringBuilder updateExpression = new StringBuilder(
                    "SET #status = :status"
                    + ", " + DynamoDBKeyBuilder.GSI2SK + " = :gsi2sk"
                    + ", " + OrderColumn.UPDATED_AT + " = :updatedAt");

            String assignNow = " = :currentUTCDateTime";
            switch (newStatus) {
                case PROCESSING -> updateExpression.append(", ").append(OrderColumn.PROCESSING_AT).append(assignNow);
                case SHIPPED    -> updateExpression.append(", ").append(OrderColumn.SHIPPED_AT).append(assignNow);
                case COMPLETED  -> updateExpression.append(", ").append(OrderColumn.COMPLETED_AT).append(assignNow);
                case DELIVERED  -> updateExpression.append(", ").append(OrderColumn.DELIVERED_AT).append(assignNow);
                case CANCELLED  -> updateExpression.append(", ").append(OrderColumn.CANCELLED_AT).append(assignNow);
                default -> { /* PENDING — no extra date field */ }
            }

            Map<String, AttributeValue> attrMap = new HashMap<>();
            attrMap.put(":status", buildNumAttribute(String.valueOf(newStatus.getCode())));
            attrMap.put(":gsi2sk", buildStrAttribute(DynamoDBKeyBuilder.buildOrderGSI2SK(String.valueOf(newStatus.getCode()))));
            attrMap.put(":updatedAt", buildStrAttribute(currentUTCDateTime));
            if (newStatus != OrderStatus.PENDING) {
                attrMap.put(":currentUTCDateTime", buildStrAttribute(currentUTCDateTime));
            }

            UpdateItemResponse response = dynamoDbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(dynamoDBKeyBuilder.getTableName())
                    .key(buildKey(userId, orderId))
                    .conditionExpression("attribute_exists(pk) AND attribute_exists(sk) AND #status < :status")
                    .updateExpression(updateExpression.toString())
                    .expressionAttributeNames(Map.of("#status", OrderColumn.STATUS))
                    .expressionAttributeValues(attrMap)
                    .returnValues(ReturnValue.ALL_NEW)
                    .build());

            log.info("Order status updated — orderId: {}, status: {}", orderId, newStatus);
            return Optional.of(fromAttributeMap(response.attributes()));
        } catch (ConditionalCheckFailedException e) {
            log.warn("Order not found or Invalid status update — userId: {}, orderId: {}, status: {}", userId, orderId, newStatus);
            throw e;
        } catch (Exception e) {
            log.error("Error updating order status ::: ", e);
            throw e;
        }
    }

    @Override
    public boolean delete(String userId, String orderId) {
        try {
            // Delete all related OrderItem records first
            orderItemRepository.deleteByOrderId(orderId);

            dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                    .tableName(dynamoDBKeyBuilder.getTableName())
                    .key(buildKey(userId, orderId))
                    .build());
            log.info("Order and related items deleted — userId: {}, orderId: {}", userId, orderId);
            return true;
        } catch (Exception e) {
            log.error("Error deleting order ::: ", e);
            throw e;
        }
    }

    @Override
    public Optional<Order> findByUserIdAndOrderId(String userId, String orderId) {
        try {
            GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                    .tableName(dynamoDBKeyBuilder.getTableName())
                    .key(buildKey(userId, orderId))
                    .build());

            if (response.item().isEmpty()) {
                log.info("Order not found — userId: {}, orderId: {}", userId, orderId);
                return Optional.empty();
            }
            return Optional.of(fromAttributeMap(response.item()));
        } catch (Exception e) {
            log.error("Error retrieving order ::: ", e);
            throw e;
        }
    }

    @Override
    public List<Order> findByUserId(String userId) {
        try {
            QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                    .tableName(dynamoDBKeyBuilder.getTableName())
                    .keyConditionExpression("pk = :pk AND begins_with(sk, :skPrefix)")
                    .expressionAttributeValues(Map.of(
                            ":pk",       buildStrAttribute(DynamoDBKeyBuilder.buildOrderPK(userId)),
                            ":skPrefix", buildStrAttribute(DynamoDBKeyBuilder.ORDER_PREFIX)
                    ))
                    .build());

            List<Order> orders = response.items().stream()
                    .map(this::fromAttributeMap).toList();
            log.info("Found {} orders for userId: {}", orders.size(), userId);
            return orders;
        } catch (Exception e) {
            log.error("Error querying orders by userId ::: ", e);
            throw e;
        }
    }

    @Override
    public List<Order> findByUserIdAndOrderStatus(String userId, OrderStatus status) {
        try {
            QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                    .tableName(dynamoDBKeyBuilder.getTableName())
                    .indexName(DynamoDBKeyBuilder.GSI2_NAME)
                    .keyConditionExpression("gsi2pk = :gsi2pk AND gsi2sk = :gsi2sk")
                    .expressionAttributeValues(Map.of(
                            ":gsi2pk", buildStrAttribute(DynamoDBKeyBuilder.buildOrderGSI2PK(userId)),
                            ":gsi2sk", buildStrAttribute(DynamoDBKeyBuilder.buildOrderGSI2SK(String.valueOf(status.getCode())))
                    ))
                    .build());

            List<Order> orders = response.items().stream()
                    .map(this::fromAttributeMap).toList();
            log.info("GSI2 Query — {} orders; userId: {}, status: {}", orders.size(), userId, status);
            return orders;
        } catch (Exception e) {
            log.error("Error querying orders by userId and status ::: ", e);
            throw e;
        }
    }

    @Override
    public List<Order> findByUserIdAndOrderDate(String userId, String fromDate, String toDate) {
        try {
            String startDateTime=DateUtils.toStartOfDay(fromDate);
            String endDateTime=DateUtils.toEndOfDay(toDate);

            QueryResponse response=dynamoDbClient.query(QueryRequest.builder()
                    .tableName(dynamoDBKeyBuilder.getTableName())
                    .indexName(DynamoDBKeyBuilder.GSI1_NAME)
                    .keyConditionExpression("gsi1pk = :gsi1pk AND gsi1sk BETWEEN :fromDate AND :toDate")
                    .filterExpression(DynamoDBKeyBuilder.ENTITY_TYPE + " = :entityType")
                    .expressionAttributeValues(Map.of(
                            ":gsi1pk", buildStrAttribute(DynamoDBKeyBuilder.buildOrderGSI1PK(userId)),
                            ":fromDate", buildStrAttribute(DynamoDBKeyBuilder.buildOrderGSI1SK(startDateTime)),
                            ":toDate", buildStrAttribute(DynamoDBKeyBuilder.buildOrderGSI1SK(endDateTime)),
                            ":entityType", buildStrAttribute(DynamoDBKeyBuilder.ENTITY_TYPE_ORDER)
                    ))
                    .build());

            List<Order> orders=response.items().stream()
                    .map(this::fromAttributeMap).toList();
            log.info("GSI1 Query — {} orders; userId: {}, fromDate: {}, toDate: {}", orders.size(), userId, fromDate, toDate);
            return orders;
        } catch (Exception e) {
            log.error("Error querying orders by userId and date range ::: ", e);
            throw e;
        }
    }

    @Override
    public List<Order> findByUserIdAndProductName(String userId, String productName) {
        try {
            QueryResponse gsi1Response = dynamoDbClient.query(QueryRequest.builder()
                    .tableName(dynamoDBKeyBuilder.getTableName())
                    .indexName(DynamoDBKeyBuilder.GSI3_NAME)
                    .keyConditionExpression("gsi3pk = :gsi3pk AND gsi3sk = :gsi3sk")
                    .filterExpression(DynamoDBKeyBuilder.ENTITY_TYPE + " = :entityType")
                    .projectionExpression(OrderColumn.ORDER_ID)
                    .expressionAttributeValues(Map.of(
                            ":gsi3pk",      buildStrAttribute(DynamoDBKeyBuilder.buildOrderItemGSI3PK(userId)),
                            ":gsi3sk", buildStrAttribute(DynamoDBKeyBuilder.buildOrderItemGSI3SK(productName)),
                            ":entityType",  buildStrAttribute(DynamoDBKeyBuilder.ENTITY_TYPE_ORDER_ITEM)
                    ))
                    .build());

            Set<String> orderIds = gsi1Response.items().stream()
                    .map(item -> item.getOrDefault(OrderColumn.ORDER_ID, buildStrAttribute("")).s())
                    .filter(id -> !id.isBlank())
                    .collect(Collectors.toSet());

            if (orderIds.isEmpty()) {
                log.info("BatchGetItem — no OrderItems found; userId: {}, productName: {}", userId, productName);
                return Collections.emptyList();
            }

            List<Map<String, AttributeValue>> keys = orderIds.stream()
                    .map(oid -> Map.of(
                            DynamoDBKeyBuilder.PK, buildStrAttribute(DynamoDBKeyBuilder.buildOrderPK(userId)),
                            DynamoDBKeyBuilder.SK, buildStrAttribute(DynamoDBKeyBuilder.buildOrderSK(oid))
                    ))
                    .toList();

            BatchGetItemResponse batchResponse = dynamoDbClient.batchGetItem(
                    BatchGetItemRequest.builder()
                            .requestItems(Map.of(dynamoDBKeyBuilder.getTableName(),
                                    KeysAndAttributes.builder()
                                            .keys(keys)
                                            .build()))
                            .build());

            List<Order> orders = batchResponse.responses()
                    .getOrDefault(dynamoDBKeyBuilder.getTableName(), Collections.emptyList())
                    .stream()
                    .map(this::fromAttributeMap)
                    .toList();

            log.info("BatchGetItem — {} orders; userId: {}, productName: {}", orders.size(), userId, productName);
            return orders;
        } catch (Exception e) {
            log.error("Error in BatchGetItem for orders by productName ::: ", e);
            throw e;
        }
    }

    @Override
    public Optional<Order> findByUserIdAndOrderNumber(String userId, String orderNumber) {
        try {
            QueryResponse response=dynamoDbClient.query(QueryRequest.builder()
                    .tableName(dynamoDBKeyBuilder.getTableName())
                    .indexName(DynamoDBKeyBuilder.LSI1_NAME)
                    .keyConditionExpression("pk = :pk AND lsi1sk = :lsi1sk")
                    .expressionAttributeValues(Map.of(
                            ":pk", buildStrAttribute(DynamoDBKeyBuilder.buildOrderPK(userId)),
                            ":lsi1sk", buildStrAttribute(DynamoDBKeyBuilder.buildOrderLSI1SK(orderNumber))
                    ))
                    .build());

            if (response.items().isEmpty()) {
                log.info("Order not found — userId: {}, orderNumber: {}", userId, orderNumber);
                return Optional.empty();
            }
            return Optional.of(fromAttributeMap(response.items().get(0)));
        } catch (Exception e) {
            log.error("Error querying order by userId and orderNumber ::: ", e);
            throw e;
        }
    }

    private Map<String, AttributeValue> toAttributeMap(Order order) {
        Map<String, AttributeValue> attrMap = new HashMap<>();

        attrMap.put(DynamoDBKeyBuilder.PK, buildStrAttribute(order.getPk()));
        attrMap.put(DynamoDBKeyBuilder.SK, buildStrAttribute(order.getSk()));

        attrMap.put(DynamoDBKeyBuilder.GSI1PK, buildStrAttribute(order.getGsi1pk()));
        attrMap.put(DynamoDBKeyBuilder.GSI1SK, buildStrAttribute(order.getGsi1sk()));
        attrMap.put(DynamoDBKeyBuilder.GSI2PK, buildStrAttribute(order.getGsi2pk()));
        attrMap.put(DynamoDBKeyBuilder.GSI2SK, buildStrAttribute(order.getGsi2sk()));
        attrMap.put(DynamoDBKeyBuilder.LSI1SK, buildStrAttribute(order.getLsi1sk()));

        attrMap.put(DynamoDBKeyBuilder.ENTITY_TYPE, buildStrAttribute(order.getEntityType()));
        attrMap.put(OrderColumn.CREATED_AT, buildStrAttribute(order.getCreatedAt()));
        attrMap.put(OrderColumn.ORDER_ID, buildStrAttribute(order.getOrderId()));
        attrMap.put(OrderColumn.USER_ID, buildStrAttribute(order.getUserId()));
        attrMap.put(OrderColumn.ORDER_NUMBER, buildStrAttribute(order.getOrderNumber()));
        attrMap.put(OrderColumn.STATUS, buildNumAttribute(String.valueOf(order.getStatus().getCode())));
        attrMap.put(OrderColumn.TOTAL_AMOUNT, buildNumAttribute(order.getTotalAmount().toString()));
        attrMap.put(OrderColumn.PAYMENT_METHOD, buildStrAttribute(order.getPaymentMethod()));

        // Store items as a List of Maps
        if (!CollectionUtils.isEmpty(order.getItems())) {
            List<AttributeValue> itemAttrs = order.getItems().stream()
                    .map(item -> AttributeValue.builder().m(itemToMap(item)).build())
                    .toList();
            attrMap.put(OrderColumn.ITEMS, AttributeValue.builder().l(itemAttrs).build());
        }

        return attrMap;
    }

    private Order fromAttributeMap(Map<String, AttributeValue> attrMap) {
        return Order.builder()
                .pk(getStrValue(attrMap, DynamoDBKeyBuilder.PK))
                .sk(getStrValue(attrMap, DynamoDBKeyBuilder.SK))
                .entityType(getStrValue(attrMap, DynamoDBKeyBuilder.ENTITY_TYPE))
                .gsi1pk(getStrValue(attrMap, DynamoDBKeyBuilder.GSI1PK))
                .gsi1sk(getStrValue(attrMap, DynamoDBKeyBuilder.GSI1SK))
                .gsi2pk(getStrValue(attrMap, DynamoDBKeyBuilder.GSI2PK))
                .gsi2sk(getStrValue(attrMap, DynamoDBKeyBuilder.GSI2SK))
                .lsi1sk(getStrValue(attrMap, DynamoDBKeyBuilder.LSI1SK))
                .orderId(getStrValue(attrMap, OrderColumn.ORDER_ID))
                .userId(getStrValue(attrMap, OrderColumn.USER_ID))
                .orderNumber(getStrValue(attrMap, OrderColumn.ORDER_NUMBER))
                .status(parseStatus(getStrValue(attrMap, OrderColumn.STATUS)))
                .totalAmount(parseTotalAmount(getStrValue(attrMap, OrderColumn.TOTAL_AMOUNT)))
                .paymentMethod(getStrValue(attrMap, OrderColumn.PAYMENT_METHOD))
                .createdAt(getStrValue(attrMap, OrderColumn.CREATED_AT))
                .updatedAt(getStrValue(attrMap, OrderColumn.UPDATED_AT))
                .processingAt(getStrValue(attrMap, OrderColumn.PROCESSING_AT))
                .shippedAt(getStrValue(attrMap, OrderColumn.SHIPPED_AT))
                .completedAt(getStrValue(attrMap, OrderColumn.COMPLETED_AT))
                .deliveredAt(getStrValue(attrMap, OrderColumn.DELIVERED_AT))
                .cancelledAt(getStrValue(attrMap, OrderColumn.CANCELLED_AT))
                .items(parseItems(attrMap.get(OrderColumn.ITEMS)))
                .build();
    }

    private Map<String, AttributeValue> itemToMap(OrderItem item) {
        Map<String, AttributeValue> attrMap = new HashMap<>();
        attrMap.put(OrderItemColumn.PRODUCT_NAME, buildStrAttribute(item.getProductName()));
        attrMap.put(OrderItemColumn.QUANTITY, buildNumAttribute(item.getQuantity() != null ? item.getQuantity().toString() : "0"));
        attrMap.put(OrderItemColumn.UNIT_PRICE, buildNumAttribute(item.getUnitPrice() != null ? item.getUnitPrice().toString() : "0"));
        return attrMap;
    }

    private Map<String, AttributeValue> buildKey(String userId, String orderId) {
        return Map.of(
                DynamoDBKeyBuilder.PK, buildStrAttribute(DynamoDBKeyBuilder.buildOrderPK(userId)),
                DynamoDBKeyBuilder.SK, buildStrAttribute(DynamoDBKeyBuilder.buildOrderSK(orderId))
        );
    }

    private static BigDecimal parseTotalAmount(String str) {
        return StringUtils.hasText(str) ? new BigDecimal(str) : BigDecimal.ZERO;
    }

    private static OrderStatus parseStatus(String str) {
        return StringUtils.hasText(str) ? OrderStatus.fromCode(Integer.parseInt(str)) : OrderStatus.PENDING;
    }

    private List<OrderItem> parseItems(AttributeValue listAttr) {
        if (listAttr == null || listAttr.l() == null || listAttr.l().isEmpty()) {
            return new ArrayList<>();
        }
        return listAttr.l().stream()
                .filter(attributeValue -> attributeValue.m() != null)
                .map(attributeValue -> {
                    Map<String, AttributeValue> attrMap = attributeValue.m();
                    return OrderItem.builder()
                            .productName(getStrValue(attrMap, OrderItemColumn.PRODUCT_NAME))
                            .quantity(Integer.parseInt(getNumValue(attrMap, OrderItemColumn.QUANTITY)))
                            .unitPrice(new BigDecimal(getNumValue(attrMap, OrderItemColumn.UNIT_PRICE)))
                            .build();
                })
                .toList();
    }
}

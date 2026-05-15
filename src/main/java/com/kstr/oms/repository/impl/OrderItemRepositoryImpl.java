package com.kstr.oms.repository.impl;

import com.kstr.oms.constant.CommonConstants.OrderItemColumn;
import com.kstr.oms.domain.OrderItem;
import com.kstr.oms.repository.OrderItemRepository;
import com.kstr.oms.util.DateUtils;
import com.kstr.oms.util.DynamoDBKeyBuilder;
import com.kstr.oms.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.util.*;

import static com.kstr.oms.util.CommonUtils.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OrderItemRepositoryImpl implements OrderItemRepository {

    private final DynamoDbClient dynamoDbClient;

    private final DynamoDBKeyBuilder dynamoDBKeyBuilder;

    @Override
    public List<OrderItem> saveBatchOrderItems(List<OrderItem> orderItems) {
        try {
            for (OrderItem orderItem : orderItems) {
                orderItem.setItemId(IdGenerator.generateOrderItemId());
                orderItem.setEntityType(DynamoDBKeyBuilder.ENTITY_TYPE_ORDER_ITEM);
                orderItem.setPk(DynamoDBKeyBuilder.buildOrderItemPK(orderItem.getOrderId()));
                orderItem.setSk(DynamoDBKeyBuilder.buildOrderItemSK(orderItem.getItemId()));
                orderItem.setGsi3pk(DynamoDBKeyBuilder.buildOrderItemGSI3PK(orderItem.getUserId()));
                orderItem.setGsi3sk(DynamoDBKeyBuilder.buildOrderItemGSI3SK(orderItem.getProductName()));
                orderItem.setCreatedAt(DateUtils.getCurrentUTCDateTime());
            }

            List<WriteRequest> writeRequests = orderItems.stream()
                    .map(item -> WriteRequest.builder()
                            .putRequest(PutRequest.builder()
                                    .item(toAttributeMap(item))
                                    .build())
                            .build())
                    .toList();

            Map<String, List<WriteRequest>> requestItems = new HashMap<>();
            requestItems.put(dynamoDBKeyBuilder.getTableName(), writeRequests);

            dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                    .requestItems(requestItems)
                    .build());

            log.info("OrderItems batch saved — count ::: {}", orderItems.size());
            return orderItems;
        } catch (Exception e) {
            log.error("Error saving order orderItems in batch", e);
            throw e;
        }
    }

    private Map<String, AttributeValue> toAttributeMap(OrderItem orderItem) {
        Map<String, AttributeValue> attrMap = new HashMap<>();

        attrMap.put(DynamoDBKeyBuilder.PK, buildStrAttribute(orderItem.getPk()));
        attrMap.put(DynamoDBKeyBuilder.SK, buildStrAttribute(orderItem.getSk()));

        attrMap.put(DynamoDBKeyBuilder.GSI3PK, buildStrAttribute(orderItem.getGsi3pk()));
        attrMap.put(DynamoDBKeyBuilder.GSI3SK, buildStrAttribute(orderItem.getGsi3sk()));

        attrMap.put(DynamoDBKeyBuilder.ENTITY_TYPE, buildStrAttribute(orderItem.getEntityType()));
        attrMap.put(OrderItemColumn.ITEM_ID, buildStrAttribute(orderItem.getItemId()));
        attrMap.put(OrderItemColumn.ORDER_ID, buildStrAttribute(orderItem.getOrderId()));
        attrMap.put(OrderItemColumn.USER_ID, buildStrAttribute(orderItem.getUserId()));
        attrMap.put(OrderItemColumn.PRODUCT_ID, buildStrAttribute(orderItem.getProductId()));
        attrMap.put(OrderItemColumn.PRODUCT_NAME, buildStrAttribute(orderItem.getProductName()));
        attrMap.put(OrderItemColumn.QUANTITY, buildNumAttribute(orderItem.getQuantity().toString()));
        attrMap.put(OrderItemColumn.UNIT_PRICE, buildNumAttribute(orderItem.getUnitPrice().toString()));
        attrMap.put(OrderItemColumn.TOTAL_PRICE, buildNumAttribute(orderItem.getTotalPrice().toString()));
        attrMap.put(OrderItemColumn.CREATED_AT, buildStrAttribute(orderItem.getCreatedAt()));
        return attrMap;
    }

    @Override
    public List<OrderItem> findByOrderId(String orderId) {
        try {
            QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                    .tableName(dynamoDBKeyBuilder.getTableName())
                    .keyConditionExpression("pk = :pk AND begins_with(sk, :skPrefix)")
                    .expressionAttributeValues(Map.of(
                            ":pk",       buildStrAttribute(DynamoDBKeyBuilder.buildOrderItemPK(orderId)),
                            ":skPrefix", buildStrAttribute(DynamoDBKeyBuilder.ITEM_PREFIX)
                    ))
                    .build());

            List<OrderItem> items = response.items().stream()
                    .map(this::fromAttributeMap)
                    .toList();
            log.info("Found {} items for orderId: {}", items.size(), orderId);
            return items;
        } catch (Exception e) {
            log.error("Error querying order items by orderId ::: ", e);
            throw e;
        }
    }

    @Override
    public void deleteByOrderId(String orderId) {
        try {
            QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                    .tableName(dynamoDBKeyBuilder.getTableName())
                    .keyConditionExpression("pk = :pk AND begins_with(sk, :skPrefix)")
                    .projectionExpression("pk, sk")
                    .expressionAttributeValues(Map.of(
                            ":pk",       buildStrAttribute(DynamoDBKeyBuilder.buildOrderItemPK(orderId)),
                            ":skPrefix", buildStrAttribute(DynamoDBKeyBuilder.ITEM_PREFIX)
                    ))
                    .build());

            if (response.items().isEmpty()) {
                log.info("No OrderItems to delete for orderId: {}", orderId);
                return;
            }

            List<Map<String, AttributeValue>> keys = response.items();
            for (int i = 0; i < keys.size(); i += 25) {
                List<WriteRequest> deleteRequests = keys.subList(i, Math.min(i + 25, keys.size()))
                        .stream()
                        .map(key -> WriteRequest.builder()
                                .deleteRequest(DeleteRequest.builder()
                                        .key(Map.of(
                                                DynamoDBKeyBuilder.PK, key.get(DynamoDBKeyBuilder.PK),
                                                DynamoDBKeyBuilder.SK, key.get(DynamoDBKeyBuilder.SK)
                                        ))
                                        .build())
                                .build())
                        .toList();

                dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                        .requestItems(Map.of(dynamoDBKeyBuilder.getTableName(), deleteRequests))
                        .build());
            }

            log.info("OrderItems batch deleted — count: {}, orderId: {}", keys.size(), orderId);
        } catch (Exception e) {
            log.error("Error deleting order items for orderId: {} ::: ", orderId, e);
            throw e;
        }
    }

    private OrderItem fromAttributeMap(Map<String, AttributeValue> attrMap) {
        return OrderItem.builder()
                .pk(getStrValue(attrMap, DynamoDBKeyBuilder.PK))
                .sk(getStrValue(attrMap, DynamoDBKeyBuilder.SK))
                .entityType(getStrValue(attrMap, DynamoDBKeyBuilder.ENTITY_TYPE))
                .gsi3pk(getStrValue(attrMap, DynamoDBKeyBuilder.GSI3PK))
                .gsi3sk(getStrValue(attrMap, DynamoDBKeyBuilder.GSI3SK))
                .itemId(getStrValue(attrMap, OrderItemColumn.ITEM_ID))
                .orderId(getStrValue(attrMap, OrderItemColumn.ORDER_ID))
                .userId(getStrValue(attrMap, OrderItemColumn.USER_ID))
                .productId(getStrValue(attrMap, OrderItemColumn.PRODUCT_ID))
                .productName(getStrValue(attrMap, OrderItemColumn.PRODUCT_NAME))
                .quantity(Integer.parseInt(getNumValue(attrMap, OrderItemColumn.QUANTITY)))
                .unitPrice(new BigDecimal(getNumValue(attrMap, OrderItemColumn.UNIT_PRICE)))
                .totalPrice(new BigDecimal(getNumValue(attrMap, OrderItemColumn.TOTAL_PRICE)))
                .createdAt(getStrValue(attrMap, OrderItemColumn.CREATED_AT))
                .build();
    }
}

package com.kstr.oms.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DynamoDBKeyBuilder {

    // Entity Types
    public static final String ENTITY_TYPE_USER = "USER";
    public static final String ENTITY_TYPE_ORDER = "ORDER";
    public static final String ENTITY_TYPE_ORDER_ITEM = "ORDER_ITEM";

    // Primary Key and Sort Key Prefixes
    public static final String USER_PREFIX = "USER#";
    public static final String ORDER_PREFIX = "ORDER#";
    public static final String ITEM_PREFIX = "ITEM#";
    public static final String METADATA = "METADATA";
    public static final String ORDER_DATE_PREFIX = "ORDER_DATE#";
    public static final String ORDER_STATUS_PREFIX = "ORDER_STATUS#";
    public static final String PROD_NAME_PREFIX = "PROD_NAME#";
    public static final String ORDER_NUMBER_PREFIX = "ORDER_NUMBER#";

    // GSI Names
    public static final String GSI1_NAME = "UserOrderDateIndex";
    public static final String GSI2_NAME = "UserOrderStatusIndex";
    public static final String GSI3_NAME = "UserProductNameIndex";

    // LSI Names
    public static final String LSI1_NAME = "UserOrderNumberIndex";

    // Attribute Names
    public static final String PK = "pk";
    public static final String SK = "sk";
    public static final String GSI1PK = "gsi1pk";
    public static final String GSI1SK = "gsi1sk";
    public static final String GSI2PK = "gsi2pk";
    public static final String GSI2SK = "gsi2sk";
    public static final String GSI3PK = "gsi3pk";
    public static final String GSI3SK = "gsi3sk";
    public static final String LSI1SK = "lsi1sk";
    public static final String ENTITY_TYPE = "entityType";

    @Value("${app.table.prefix:local}")
    private String tablePrefix;

    public String getTableName() {
        return tablePrefix + "-oms";
    }

    public static String buildUserPK(String userId) {
        return USER_PREFIX + userId;
    }

    public static String buildUserSK() {
        return METADATA;
    }

    public static String buildOrderPK(String userId) {
        return USER_PREFIX + userId;
    }

    public static String buildOrderSK(String orderId) {
        return ORDER_PREFIX + orderId;
    }

    public static String buildOrderGSI1PK(String userId) {
        return USER_PREFIX + userId;
    }

    public static String buildOrderGSI1SK(String dateStr) {
        return ORDER_DATE_PREFIX + dateStr;
    }

    public static String buildOrderGSI2PK(String userId) {
        return USER_PREFIX + userId;
    }

    public static String buildOrderGSI2SK(String status) {
        return ORDER_STATUS_PREFIX + status;
    }

    public static String buildOrderItemPK(String orderId) {
        return ORDER_PREFIX + orderId;
    }

    public static String buildOrderItemSK(String itemId) {
        return ITEM_PREFIX + itemId;
    }

    public static String buildOrderItemGSI3PK(String userId) {
        return USER_PREFIX + userId;
    }

    public static String buildOrderItemGSI3SK(String productName) {
        return PROD_NAME_PREFIX + productName;
    }

    public static String buildOrderLSI1SK(String orderNumber) {
        return ORDER_NUMBER_PREFIX + orderNumber;
    }

}

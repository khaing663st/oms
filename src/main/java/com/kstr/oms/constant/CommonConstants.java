package com.kstr.oms.constant;

import lombok.experimental.UtilityClass;

public class CommonConstants {

    public static final String EMPTY_STRING = "";

    public static final String EMPTY_NUMBER = "0";

    @UtilityClass
    public class UserColumn {
        public static final String USER_ID = "userId";
        public static final String NAME = "name";
        public static final String PHONE = "phone";
        public static final String EMAIL = "email";
        public static final String ADDRESS = "address";
        public static final String CREATED_AT = "createdAt";
        public static final String UPDATED_AT = "updatedAt";
    }

    @UtilityClass
    public class OrderColumn {
        public static final String ORDER_ID = "orderId";
        public static final String USER_ID = "userId";
        public static final String ORDER_NUMBER = "orderNumber";
        public static final String STATUS = "status";
        public static final String TOTAL_AMOUNT = "totalAmount";
        public static final String PAYMENT_METHOD = "paymentMethod";
        public static final String ITEMS = "items";
        public static final String CREATED_AT = "createdAt";
        public static final String UPDATED_AT = "updatedAt";
        public static final String PROCESSING_AT = "processingAt";
        public static final String SHIPPED_AT = "shippedAt";
        public static final String COMPLETED_AT = "completedAt";
        public static final String DELIVERED_AT = "deliveredAt";
        public static final String CANCELLED_AT = "cancelledAt";
    }

    @UtilityClass
    public class OrderItemColumn {
        public static final String ITEM_ID = "itemId";
        public static final String ORDER_ID = "orderId";
        public static final String USER_ID = "userId";
        public static final String PRODUCT_ID = "productId";
        public static final String PRODUCT_NAME = "productName";
        public static final String QUANTITY = "quantity";
        public static final String UNIT_PRICE = "unitPrice";
        public static final String TOTAL_PRICE = "totalPrice";
        public static final String CREATED_AT = "createdAt";
    }
}

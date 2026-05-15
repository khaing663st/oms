package com.kstr.oms.config;

import com.kstr.oms.util.DynamoDBKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamoDBTableInitializer implements ApplicationRunner {

    private final DynamoDbClient dynamoDbClient;

    private final DynamoDBKeyBuilder dynamoDBKeyBuilder;

    @Override
    public void run(ApplicationArguments args) {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String tableName = dynamoDBKeyBuilder.getTableName();

        if (tableExists(tableName)) {
            log.info("DynamoDB table '{}' already exists — skipping creation.", tableName);
            return;
        }

        log.info("DynamoDB table '{}' not found — creating...", tableName);
        createOmsTable(tableName);
        log.info("DynamoDB table '{}' created successfully.", tableName);
    }

    private boolean tableExists(String tableName) {
        try {
            dynamoDbClient.describeTable(DescribeTableRequest.builder()
                    .tableName(tableName)
                    .build());
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    private void createOmsTable(String tableName) {
        /*
         * Single-table design:
         *
         * Primary key  : pk  (S)  +  sk  (S)
         * GSI1         : gsi1pk (S)  +  gsi1sk (S)  — UserOrderDateIndex  (Order)
         * GSI2         : gsi2pk (S)  +  gsi2sk (S)  — UserOrderStatusIndex (Order)
         * GSI3         : gsi3pk (S)  +  gsi3sk (S)  — UserProductNameIndex (OrderItem)
         * LSI1         : pk  (S)  +  lsi1sk (S)     — OrderNumberIndex    (Order)
         */

        // ----- Attribute definitions -----
        List<AttributeDefinition> attributeDefinitions = List.of(
                attrDef(DynamoDBKeyBuilder.PK,     ScalarAttributeType.S),
                attrDef(DynamoDBKeyBuilder.SK,     ScalarAttributeType.S),
                attrDef(DynamoDBKeyBuilder.GSI1PK, ScalarAttributeType.S),
                attrDef(DynamoDBKeyBuilder.GSI1SK, ScalarAttributeType.S),
                attrDef(DynamoDBKeyBuilder.GSI2PK, ScalarAttributeType.S),
                attrDef(DynamoDBKeyBuilder.GSI2SK, ScalarAttributeType.S),
                attrDef(DynamoDBKeyBuilder.GSI3PK, ScalarAttributeType.S),
                attrDef(DynamoDBKeyBuilder.GSI3SK, ScalarAttributeType.S),
                attrDef(DynamoDBKeyBuilder.LSI1SK, ScalarAttributeType.S)
        );

        // ----- Primary key schema -----
        List<KeySchemaElement> keySchema = List.of(
                keySchema(DynamoDBKeyBuilder.PK, KeyType.HASH),
                keySchema(DynamoDBKeyBuilder.SK, KeyType.RANGE)
        );

        // ----- GSI1: UserOrderDateIndex (Order) -----
        GlobalSecondaryIndex gsi1 = GlobalSecondaryIndex.builder()
                .indexName(DynamoDBKeyBuilder.GSI1_NAME)
                .keySchema(
                        keySchema(DynamoDBKeyBuilder.GSI1PK, KeyType.HASH),
                        keySchema(DynamoDBKeyBuilder.GSI1SK, KeyType.RANGE)
                )
                .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                .provisionedThroughput(defaultThroughput())
                .build();

        // ----- GSI2: UserOrderStatusIndex (Order) -----
        GlobalSecondaryIndex gsi2 = GlobalSecondaryIndex.builder()
                .indexName(DynamoDBKeyBuilder.GSI2_NAME)
                .keySchema(
                        keySchema(DynamoDBKeyBuilder.GSI2PK, KeyType.HASH),
                        keySchema(DynamoDBKeyBuilder.GSI2SK, KeyType.RANGE)
                )
                .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                .provisionedThroughput(defaultThroughput())
                .build();

        // ----- GSI3: UserProductNameIndex (OrderItem) -----
        GlobalSecondaryIndex gsi3 = GlobalSecondaryIndex.builder()
                .indexName(DynamoDBKeyBuilder.GSI3_NAME)
                .keySchema(
                        keySchema(DynamoDBKeyBuilder.GSI3PK, KeyType.HASH),
                        keySchema(DynamoDBKeyBuilder.GSI3SK, KeyType.RANGE)
                )
                .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                .provisionedThroughput(defaultThroughput())
                .build();

        // ----- LSI1: OrderNumberIndex -----
        LocalSecondaryIndex lsi1 = LocalSecondaryIndex.builder()
                .indexName(DynamoDBKeyBuilder.LSI1_NAME)
                .keySchema(
                        keySchema(DynamoDBKeyBuilder.PK,     KeyType.HASH),
                        keySchema(DynamoDBKeyBuilder.LSI1SK, KeyType.RANGE)
                )
                .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                .build();

        // ----- Create table request -----
        CreateTableRequest request = CreateTableRequest.builder()
                .tableName(tableName)
                .attributeDefinitions(attributeDefinitions)
                .keySchema(keySchema)
                .globalSecondaryIndexes(gsi1, gsi2, gsi3)
                .localSecondaryIndexes(lsi1)
                .provisionedThroughput(defaultThroughput())
                .build();

        dynamoDbClient.createTable(request);

        // Wait until the table becomes ACTIVE
        waitUntilActive(tableName);
    }

    private void waitUntilActive(String tableName) {
        log.info("Waiting for table '{}' to become ACTIVE...", tableName);
        int maxAttempts = 20;
        int waitMs = 1_000;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                DescribeTableResponse response = dynamoDbClient.describeTable(
                        DescribeTableRequest.builder().tableName(tableName).build());
                TableStatus status = response.table().tableStatus();

                if (TableStatus.ACTIVE.equals(status)) {
                    log.info("Table '{}' is now ACTIVE.", tableName);
                    return;
                }

                log.debug("Table '{}' status: {} (attempt {}/{})", tableName, status, attempt, maxAttempts);
                Thread.sleep(waitMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for table '{}' to become ACTIVE.", tableName);
                return;
            }
        }

        log.warn("Table '{}' did not reach ACTIVE status within the expected time.", tableName);
    }

    private static AttributeDefinition attrDef(String name, ScalarAttributeType type) {
        return AttributeDefinition.builder()
                .attributeName(name)
                .attributeType(type)
                .build();
    }

    private static KeySchemaElement keySchema(String name, KeyType keyType) {
        return KeySchemaElement.builder()
                .attributeName(name)
                .keyType(keyType)
                .build();
    }

    private static ProvisionedThroughput defaultThroughput() {
        return ProvisionedThroughput.builder()
                .readCapacityUnits(5L)
                .writeCapacityUnits(5L)
                .build();
    }
}


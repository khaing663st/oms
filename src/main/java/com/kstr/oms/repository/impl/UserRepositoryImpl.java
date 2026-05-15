package com.kstr.oms.repository.impl;

import com.kstr.oms.constant.CommonConstants;
import com.kstr.oms.domain.User;
import com.kstr.oms.repository.UserRepository;
import com.kstr.oms.util.DateUtils;
import com.kstr.oms.util.DynamoDBKeyBuilder;
import com.kstr.oms.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import static com.kstr.oms.util.CommonUtils.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final DynamoDbClient dynamoDbClient;

    private final DynamoDBKeyBuilder dynamoDBKeyBuilder;

    @Override
    public User save(User user) {
        try {
            user.setUserId(IdGenerator.generateUserId());
            user.setPk(DynamoDBKeyBuilder.buildUserPK(user.getUserId()));
            user.setSk(DynamoDBKeyBuilder.buildUserSK());
            user.setEntityType(DynamoDBKeyBuilder.ENTITY_TYPE_USER);
            user.setCreatedAt(DateUtils.getCurrentUTCDateTime());

             PutItemRequest putItemRequest = PutItemRequest.builder()
                     .tableName(dynamoDBKeyBuilder.getTableName())
                     .item(toAttributeMap(user))
                     .build();

            dynamoDbClient.putItem(putItemRequest);
            log.info("User saved successfully with userId ::: {}", user.getUserId());
            return user;

        } catch (Exception e) {
            log.error("Error saving user ::: ", e);
            throw e;
        }
    }

    @Override
    public Optional<User> findById(String userId) {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put(DynamoDBKeyBuilder.PK, AttributeValue.builder()
                    .s(DynamoDBKeyBuilder.buildUserPK(userId))
                    .build());
            key.put(DynamoDBKeyBuilder.SK, AttributeValue.builder()
                    .s(DynamoDBKeyBuilder.buildUserSK())
                    .build());

            GetItemRequest getItemRequest = GetItemRequest.builder()
                    .tableName(dynamoDBKeyBuilder.getTableName())
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(getItemRequest);

            if (response.item().isEmpty()) {
                log.info("User not found with userId ::: {}", userId);
                return Optional.empty();
            }

            User user = fromAttributeMap(response.item());
            log.info("User found with userId ::: {}", userId);
            return Optional.of(user);

        } catch (Exception e) {
            log.error("Error retrieving user ::: ", e);
            throw e;
        }
    }

    @Override
    public Optional<User> update(String userId, User user) {
        try {
            user.setUpdatedAt(DateUtils.getCurrentUTCDateTime());

            Map<String, AttributeValue> key = new HashMap<>();
            key.put(DynamoDBKeyBuilder.PK, AttributeValue.builder()
                    .s(DynamoDBKeyBuilder.buildUserPK(userId))
                    .build());
            key.put(DynamoDBKeyBuilder.SK, AttributeValue.builder()
                    .s(DynamoDBKeyBuilder.buildUserSK())
                    .build());

            StringBuilder updateExpression = new StringBuilder("SET ");
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

            updateExpression.append("updatedAt = :updatedAt");
            expressionAttributeValues.put(":updatedAt", buildStrAttribute(user.getUpdatedAt()));

            if (user.getName() != null) {
                updateExpression.append(", ").append("#name = :name");
                expressionAttributeValues.put(":name", buildStrAttribute(user.getName()));
            }

            if (user.getEmail() != null) {
                updateExpression.append(", ").append("email = :email");
                expressionAttributeValues.put(":email", buildStrAttribute(user.getEmail()));
            }

            if (user.getPhone() != null) {
                updateExpression.append(", ").append("phone = :phone");
                expressionAttributeValues.put(":phone", buildStrAttribute(user.getPhone()));
            }

            if (user.getAddress() != null) {
                updateExpression.append(", ").append("address = :address");
                expressionAttributeValues.put(":address", buildStrAttribute(user.getAddress()));
            }

             UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                     .tableName(dynamoDBKeyBuilder.getTableName())
                     .key(key)
                     .updateExpression(updateExpression.toString())
                     .expressionAttributeNames(Map.of("#name", "name"))
                     .expressionAttributeValues(expressionAttributeValues)
                     .returnValues(ReturnValue.ALL_NEW)
                     .build();

            UpdateItemResponse response = dynamoDbClient.updateItem(updateItemRequest);
            User updatedUser = fromAttributeMap(response.attributes());
            log.info("User updated successfully with userId ::: {}", userId);

            return Optional.of(updatedUser);

        } catch (Exception e) {
            log.error("Error updating user ::: ", e);
            throw e;
        }
    }

    @Override
    public boolean delete(String userId) {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put(DynamoDBKeyBuilder.PK, AttributeValue.builder()
                    .s(DynamoDBKeyBuilder.buildUserPK(userId))
                    .build());
            key.put(DynamoDBKeyBuilder.SK, AttributeValue.builder()
                    .s(DynamoDBKeyBuilder.buildUserSK())
                    .build());

            DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder()
                    .tableName(dynamoDBKeyBuilder.getTableName())
                    .key(key)
                    .build();

            dynamoDbClient.deleteItem(deleteItemRequest);
            log.info("User deleted successfully with userId ::: {}", userId);
            return true;
        } catch (Exception e) {
            log.error("Error deleting user ::: ", e);
            throw e;
        }
    }

    @Override
    public List<User> findAll() {
        try {
             ScanRequest scanRequest = ScanRequest.builder()
                     .tableName(dynamoDBKeyBuilder.getTableName())
                     .filterExpression("entityType = :entityType")
                     .expressionAttributeValues(Map.of(
                             ":entityType",
                             buildStrAttribute(DynamoDBKeyBuilder.ENTITY_TYPE_USER)))
                     .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);
            List<User> users = new ArrayList<>();

            response.items().forEach(item -> users.add(fromAttributeMap(item)));

            log.info("Scan operation completed. Found {} users", users.size());
            return users;

        } catch (Exception e) {
            log.error("Error scanning users ::: ", e);
            throw e;
        }
    }

    private Map<String, AttributeValue> toAttributeMap(User user) {
        Map<String, AttributeValue> attrMap = new HashMap<>();

        attrMap.put(DynamoDBKeyBuilder.PK, buildStrAttribute(user.getPk()));
        attrMap.put(DynamoDBKeyBuilder.SK, buildStrAttribute(user.getSk()));

        attrMap.put(DynamoDBKeyBuilder.ENTITY_TYPE, buildStrAttribute(user.getEntityType()));
        attrMap.put(CommonConstants.UserColumn.USER_ID, buildStrAttribute(user.getUserId()));
        attrMap.put(CommonConstants.UserColumn.NAME, buildStrAttribute(user.getName()));
        attrMap.put(CommonConstants.UserColumn.PHONE, buildStrAttribute(user.getPhone()));
        attrMap.put(CommonConstants.UserColumn.EMAIL, buildStrAttribute(user.getEmail()));
        attrMap.put(CommonConstants.UserColumn.ADDRESS, buildStrAttribute(user.getAddress()));
        attrMap.put(CommonConstants.UserColumn.CREATED_AT, buildStrAttribute(user.getCreatedAt()));
        return attrMap;
    }

    private User fromAttributeMap(Map<String, AttributeValue> attrMap) {
        return User.builder()
                .pk(getStrValue(attrMap, DynamoDBKeyBuilder.PK))
                .sk(getStrValue(attrMap, DynamoDBKeyBuilder.SK))
                .entityType(getStrValue(attrMap, DynamoDBKeyBuilder.ENTITY_TYPE))
                .userId(getStrValue(attrMap, CommonConstants.UserColumn.USER_ID))
                .name(getStrValue(attrMap, CommonConstants.UserColumn.NAME))
                .phone(getStrValue(attrMap, CommonConstants.UserColumn.PHONE))
                .email(getStrValue(attrMap, CommonConstants.UserColumn.EMAIL))
                .address(getStrValue(attrMap, CommonConstants.UserColumn.ADDRESS))
                .createdAt(getStrValue(attrMap, CommonConstants.UserColumn.CREATED_AT))
                .updatedAt(getStrValue(attrMap, CommonConstants.UserColumn.UPDATED_AT))
                .build();
    }
}



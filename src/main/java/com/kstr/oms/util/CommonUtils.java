package com.kstr.oms.util;

import lombok.experimental.UtilityClass;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import static com.kstr.oms.constant.CommonConstants.EMPTY_NUMBER;
import static com.kstr.oms.constant.CommonConstants.EMPTY_STRING;

@UtilityClass
public class CommonUtils {

    public static AttributeValue buildStrAttribute(String str) {
        return AttributeValue.builder().s(str != null ? str : EMPTY_STRING).build();
    }

    public static AttributeValue buildNumAttribute(String str) {
        return AttributeValue.builder().n(str != null ? str : EMPTY_NUMBER).build();
    }

    public static String getStrValue(Map<String, AttributeValue> attrMap, String key) {
        AttributeValue attributeValue = attrMap.get(key);
        return (attributeValue != null && attributeValue.s() != null) ? attributeValue.s() : EMPTY_STRING;
    }

    public static String getNumValue(Map<String, AttributeValue> attrMap, String key) {
        AttributeValue attributeValue = attrMap.get(key);
        return (attributeValue != null && attributeValue.n() != null) ? attributeValue.n() : EMPTY_NUMBER;
    }
}

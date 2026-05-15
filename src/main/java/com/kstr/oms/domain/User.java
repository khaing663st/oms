package com.kstr.oms.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private String pk; // "USER#{userId}"
    private String sk; // "METADATA"
    private String entityType; // "USER"

    private String userId; // UUID
    private String name;
    private String phone;
    private String email;
    private String address;
    private String createdAt;
    private String updatedAt;
}



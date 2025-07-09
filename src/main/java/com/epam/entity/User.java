package com.epam.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class User {

    private String email;
    private String firstName;
    private String lastName;
    private String password;
    private String role;
    private String createdAt;
    private String imageUrl;

    @DynamoDbPartitionKey
    public String getEmail() {
        return email;
    }
}
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
public class Feedback {

    private String id;
    private String locationId;
    private String type;
    private double rate;
    private String comment;
    private String userName;
    private String userAvatarUrl;
    private String date;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}

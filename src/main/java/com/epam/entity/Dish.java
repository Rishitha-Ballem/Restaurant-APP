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
public class Dish {

    private String calories;
    private String carbohydrates;
    private String description;
    private String dishType;
    private String fats;
    private String id;
    private String imageUrl;
    private String name;
    private String price;
    private String proteins;
    private String state;
    private String vitamins;
    private String weight;
    private int order;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}
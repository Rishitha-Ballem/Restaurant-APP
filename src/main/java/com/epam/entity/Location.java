package com.epam.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Location {

    private String id;
    private String description;
    private List<String> specialDishes;
    private List<String> feedbacks;
    private String address;
    private int totalCapacity;
    private double averageOccupancy;
    private String imageUrl;
    private double rating;
    private List<String> tables;
    private List<String> listOfWaiters;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}

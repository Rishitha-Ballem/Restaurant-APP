package com.epam.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Reservation {
    private String id;
    private String userId; // either customer email or "Visitor"
    private String status;
    private String locationAddress;
    private String tableId;
    private String date;
    private String timeSlot;
    private List<String> preOrder;
    private Integer noOfGuests;
    private String feedbackId;
    private LocalTime bookedAt;
    private String waiterId;
    private String bookedBy;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}

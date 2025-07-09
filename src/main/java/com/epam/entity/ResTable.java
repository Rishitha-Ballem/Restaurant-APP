package com.epam.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class ResTable {
    private String id;
    private Integer capacity;
    private String date;
    private Map<String,Boolean> slots =
            Map.of(
                    "slot1",true,
                    "slot2",true,
                    "slot3",true,
                    "slot4",true,
                    "slot5",true,
                    "slot6",true,
                    "slot7",true
            );

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

}


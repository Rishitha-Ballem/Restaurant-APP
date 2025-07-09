package com.epam.repository;

import com.epam.edai.run8.team15.entity.Location;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class LocationRepository {

    private final DynamoDbTable<Location> locationTable;

    public List<Location> findAll(){
        return locationTable.scan().items().stream().toList();
    }

    public Location findById(String id){
        return locationTable.getItem(Key.builder().partitionValue(id).build());
    }
}

package com.epam.repository;


import com.epam.edai.run8.team15.entity.Dish;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DishRepository {

    private final DynamoDbTable<Dish> dishTable;

    public Optional<Dish> findById(String id){
        Key key = Key.builder().partitionValue(id).build();
        Dish dish = dishTable.getItem(key);
        return Optional.ofNullable(dish);
    }

    public List<Dish> findAllSortedByPopularity() {
        return dishTable.scan().items().stream()
                .sorted((d1, d2) -> Integer.compare(d2.getOrder(), d1.getOrder()))
                .limit(4)
                .collect(Collectors.toList());
    }

    public List<Dish> findByDishType(String type) {
        return dishTable.scan().items().stream()
                .filter(dish -> dish.getDishType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

}
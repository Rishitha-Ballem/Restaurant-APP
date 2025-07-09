package com.epam.repository;

import com.epam.edai.run8.team15.entity.Feedback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Repository
@RequiredArgsConstructor
public class FeedbackRepository {

    private final DynamoDbTable<Feedback> feedbackTable;

    public void save(Feedback feedback) {
        feedbackTable.putItem(feedback);
    }

    public Feedback findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        return feedbackTable.getItem(key);
    }
}

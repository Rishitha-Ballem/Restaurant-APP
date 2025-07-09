package com.epam.dto;

import com.epam.edai.run8.team15.entity.Feedback;
import lombok.Data;

@Data
public class FeedbackDto {

    private String id;
    private double rate;
    private String comment;
    private String userName;
    private String userAvatarUrl;
    private String date;
    private String type;
    private String locationId;

    public static FeedbackDto fromEntity(Feedback feedback) {
        FeedbackDto dto = new FeedbackDto();
        dto.setId(feedback.getId());
        dto.setRate(feedback.getRate());
        dto.setComment(feedback.getComment());
        dto.setUserName(feedback.getUserName());
        dto.setUserAvatarUrl(feedback.getUserAvatarUrl());
        dto.setDate(feedback.getDate());
        dto.setType(feedback.getType());
        dto.setLocationId(feedback.getLocationId());
        return dto;
    }
}

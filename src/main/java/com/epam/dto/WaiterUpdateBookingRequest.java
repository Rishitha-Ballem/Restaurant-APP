package com.epam.dto;

import lombok.Data;

@Data
public class WaiterUpdateBookingRequest {
    private String tableNumber;
    private String date;
    private String timeFrom;
    private String timeTo;

}
package com.epam.dto;

import lombok.Data;

@Data
public class WaiterBookingServiceRequest {

    private String locationId;
    private String tableNumber;
    private String date;
    private Integer guestsNumber;
    private String timeFrom;
    private String timeTo;
    private String clientType;
    private String customerEmail;

}
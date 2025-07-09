package com.epam.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BookingServiceRequest {
    private String locationId;
    private String tableNumber;
    private String date;
    private String guestsNumber;
    private String timeFrom;
    private String timeTo;

}
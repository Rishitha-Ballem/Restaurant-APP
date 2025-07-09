package com.epam.dto;

import com.epam.edai.run8.team15.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingServiceResponse {
    private String id;
    private String status;
    private String locationAddress;
    private String date;
    private String timeSlot;
    private String preOrder;
    private String guestNumber;
    private String feedbackId;


    public static BookingServiceResponse toBookingService(Reservation reservation, String locationAddress) {
        return new BookingServiceResponse(reservation.getId(),
                reservation.getStatus(), locationAddress, reservation.getDate()
                , reservation.getTimeSlot(), reservation.getPreOrder().toString(),
                String.valueOf(reservation.getNoOfGuests()), reservation.getFeedbackId());

    }
}
package com.epam.dto;

import com.epam.edai.run8.team15.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WaiterBookingServiceResponse {

    private String id;
    private String status;
    private String locationAddress;
    private String date;
    private String timeSlot;
    private String preOrder;
    private String tableNumber;
    private String guestNumber;
    private String feedbackId;
    private String userInfo;

    public static WaiterBookingServiceResponse toBookingService(Reservation reservation, String locationAddress, String userInfo) {
        return new WaiterBookingServiceResponse(
                reservation.getId(),
                reservation.getStatus(),
                locationAddress,
                reservation.getDate(),
                reservation.getTimeSlot(),
                reservation.getPreOrder().toString(),
                reservation.getTableId(),
                String.valueOf(reservation.getNoOfGuests()),
                reservation.getFeedbackId(),
                userInfo
        );
    }
}
package com.epam.dto;

import com.epam.edai.run8.team15.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReservationHistoryResponse {
    private String id;
    private String status;
    private String locationAddress;
    private String date;
    private String timeSlot;
    private List<String> preOrder;
    private String guestNumber;
    private String feedbackId;

    public static ReservationHistoryResponse toReservationHistory(Reservation reservation){
        return new ReservationHistoryResponse(reservation.getId(),
                reservation.getStatus(),
                reservation.getLocationAddress(),
                reservation.getDate(),
                reservation.getTimeSlot(),
                reservation.getPreOrder(),
                reservation.getNoOfGuests().toString(),
                reservation.getFeedbackId());
    }
}
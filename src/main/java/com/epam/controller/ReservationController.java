package com.epam.controller;


import com.epam.edai.run8.team15.dto.BookingServiceRequest;
import com.epam.edai.run8.team15.dto.ReservationHistoryResponse;
import com.epam.edai.run8.team15.service.ClientBookingService;
import com.epam.edai.run8.team15.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private  final ClientBookingService clientBookingService;

    @GetMapping
    public ResponseEntity<?> getAllReservations() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        Map<String,Object> response=reservationService.getReservationHistory(email);
        int statusCode=(Integer)response.get("statusCode");
        if(statusCode==200){
            return ResponseEntity.ok((List<ReservationHistoryResponse>)response.get("body"));
        }
        return createResponse((String) response.get("message"), statusCode);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editReservationByClient(@PathVariable String id, @RequestBody BookingServiceRequest bookingServiceRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            if (id == null || id.isEmpty()) {
                return createResponse("Missing reservation ID in path", 400);
            }

            Map<String, Object> response = clientBookingService.updateReservationById(id, bookingServiceRequest, email);
            int statusCode = (Integer) response.get("statusCode");
            String message = (String) response.get("message");

            return createResponse(message, statusCode);
        } catch (Exception e) {
            return createResponse("Internal Server Error: " + e.getMessage(), 500);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> cancelReservation(@PathVariable String id) {
        try{
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            if(id == null || id.isBlank()) return createResponse("Missing reservation id in path parameter", 400);

            Map<String, Object> response = reservationService.deleteReservationById(id, email);

            int statusCode =(Integer) response.get("statusCode");
            String message = (String) response.get("message");

            return createResponse(message, statusCode);

        } catch (Exception e) {
            return createResponse("Internal Server error", 500);
        }
    }

    private ResponseEntity<Map<String, Object>> createResponse(String message, int statusCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);

        return ResponseEntity
                .status(HttpStatus.valueOf(statusCode))
                .body(body);
    }

}

package com.epam.controller;

import com.epam.edai.run8.team15.dto.*;
import com.epam.edai.run8.team15.service.ClientBookingService;
import com.epam.edai.run8.team15.service.ReservationService;
import com.epam.edai.run8.team15.service.WaiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final WaiterService waiterService;
    private final ReservationService reservationService;
    private final ClientBookingService clientBookingService;


    @PostMapping("/waiter")
    public ResponseEntity<?> createBookingByWaiter(@RequestBody WaiterBookingServiceRequest waiterBookingServiceRequest) {
        try {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String waiterId = authentication.getName();

            Map<String, Object> response = waiterService.saveBooking(waiterBookingServiceRequest, waiterId);
            int statusCode = (int) response.get("statusCode");

            if (statusCode == 200) {
                WaiterBookingServiceResponse waiterReservationServiceResponse = (WaiterBookingServiceResponse) response.get("body");
                return ResponseEntity.ok(waiterReservationServiceResponse);
            } else return createResponse((String) response.get("message"), statusCode);
        } catch (Exception e) {
            return createResponse("Error while making booking", 500);
        }
    }

    @PutMapping("/waiter/{id}")
    public ResponseEntity<?> updateBookingByWaiter(@PathVariable String id, @RequestBody WaiterUpdateBookingRequest waiterUpdateBookingRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String waiterId = authentication.getName();

        Map<String, Object> response = waiterService.postponeBooking(id, waiterUpdateBookingRequest, waiterId);
        int statusCode = (Integer) response.get("statusCode");
        String message = (String) response.get("message");

        return createResponse(message, statusCode);
    }

    @GetMapping("/tables")
    public ResponseEntity<?> getAvailableTables(@RequestParam(value = "locationId") String locationId,
                                               @RequestParam(value = "date") String date,
                                               @RequestParam(value = "time", required = false) String time,
                                               @RequestParam(value = "guests") String guests) {
        // Validate required parameters
        if (locationId == null || date == null || guests == null) {
            return createResponse("Missing required query parameters: locationId, date, or guests", 400);
        }

        // Optional: Validate time format (if provided)
        if (time != null && !time.isEmpty()) {
            try {
                LocalTime parsedTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
                // No need for range check since LocalTime.parse only allows 00:00 to 23:59
            } catch (DateTimeParseException e) {
                return createResponse("Invalid time format. Expected format is HH:mm between 00:00 and 23:59", 400);
            }
        }

        int capacity;
        try {
            capacity = Integer.parseInt(guests);
        } catch (NumberFormatException e) {
            return createResponse("Invalid number format for 'guests'", 400);
        }

        // Call service to get available tables
        Map<String, Object> response = reservationService.getAvailableTables(locationId, date, capacity);
        int statusCode = (Integer) response.get("statusCode");

        if (statusCode == 200) {
            List<AvailableTableResponse> availableTableResponses = (List<AvailableTableResponse>) response.get("body");
            return ResponseEntity.ok(availableTableResponses);
        } else {
            return createResponse((String) response.get("message"), statusCode);
        }
    }

    @PostMapping("/client")
    public ResponseEntity<?> createBookingByClient(@RequestBody BookingServiceRequest bookingServiceRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            Map<String, Object> response = clientBookingService.saveResponseForBooking(bookingServiceRequest, email);
            int statusCode = (Integer) response.get("statusCode");

            // Prepare appropriate response based on status code
            if (statusCode == 200) {
                List<BookingServiceResponse> bookingServiceResponses =
                        (List<BookingServiceResponse>) response.get("body");
                return ResponseEntity.ok(bookingServiceResponses);
            } else {
                return createResponse((String) response.get("message"), statusCode);
            }
        } catch (Exception e) {
            return createResponse("Internal Server Error: " + e.getMessage(), 500);
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

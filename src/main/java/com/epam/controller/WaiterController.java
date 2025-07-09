package com.epam.controller;

import com.epam.edai.run8.team15.dto.WaiterReservationResponse;
import com.epam.edai.run8.team15.entity.Waiter;
import com.epam.edai.run8.team15.service.WaiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/waiter")
public class WaiterController {
    private final DynamoDbTable<Waiter> waiterDynamoDbTable;
    private final WaiterService waiterService;

    @GetMapping("/reservations")
    public ResponseEntity<?> getWaiterReservations(@RequestParam(value = "date", required = false) String date,
                                                   @RequestParam(value = "time", required = false) String time,
                                                   @RequestParam(value = "table", defaultValue = "Any Table") String table) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String waiterId = authentication.getName();

            if (date == null) {
                date = LocalDate.now().toString();
            }
            Map<String, Object> response = waiterService.getAllReservations(waiterId, date, time, table);
            int statusCode = (Integer) response.get("statusCode");

            if (statusCode == 200) {
                List<WaiterReservationResponse> waiterReservationResponses = (List<WaiterReservationResponse>) response.get("body");
                return ResponseEntity.ok(waiterReservationResponses);
            } else {
                String message = (String) response.get("message");
                return createResponse(message, statusCode);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving reservations");
        }

    }

    @DeleteMapping("/{reservationId}")
    public ResponseEntity<Map<String, Object>> cancelReservation(@PathVariable String reservationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String waiterId = authentication.getName();

        if(reservationId == null) {
            return createResponse("Missing reservation ID in path parameters", 400);
        }

        Map<String , Object> response = waiterService.cancelReservationById(reservationId, waiterId);
        int statusCode = (int) response.get("statusCode");
        return createResponse((String) response.get("message"), statusCode);
    }

    private ResponseEntity<Map<String, Object>> createResponse(String message, int statusCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);

        return ResponseEntity
                .status(HttpStatus.valueOf(statusCode))
                .body(body);
    }
}
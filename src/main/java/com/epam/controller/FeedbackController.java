package com.epam.controller;

import com.epam.edai.run8.team15.dto.FeedbackRequest;
import com.epam.edai.run8.team15.entity.Feedback;
import com.epam.edai.run8.team15.service.FeedbackService;
import com.epam.edai.run8.team15.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.POST})
@RequiredArgsConstructor
@RestController
@RequestMapping("/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<?> submitFeedback(@RequestBody FeedbackRequest feedbackRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            Map<String, Object> response = feedbackService.saveFeedback(feedbackRequest, userEmail);
            int statusCode = (Integer) response.get("statusCode");

            if (response.containsKey("error")) {
                return createResponse((String) response.get("error"), statusCode); // Use "error" key
            }
            return createResponse((String) response.get("message"), statusCode);

        } catch (IllegalArgumentException e) {
            return createResponse("Invalid request", 400);
        } catch (Exception e) {
            return createResponse("Internal Server Error", 500);
        }

    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFeedback(@PathVariable String id) {
        try{
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            if (id == null || id.isEmpty()) {
                return createResponse("Missing reservation ID in path parameters", 400);
            }

            String feedbackId = reservationService.getFeedbackId(id);
            Feedback cusineFeedback = feedbackService.getFeedbackById(feedbackId + "c");
            Feedback serviceFeedback = feedbackService.getFeedbackById(feedbackId + "s");
            Map<String, Object> response = createFeedbackResponse(cusineFeedback, serviceFeedback, id);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createResponse("Incomplete feedback data", 404);
        }

    }

    private ResponseEntity<Map<String, Object>> createResponse(String message, int statusCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);

        return ResponseEntity
                .status(HttpStatus.valueOf(statusCode))
                .body(body);
    }

    public Map<String, Object> createFeedbackResponse(Feedback cuisineFeedback, Feedback serviceFeedback, String reservationId) {
        return Map.ofEntries(
                Map.entry("cuisineComment", cuisineFeedback != null ? cuisineFeedback.getComment() : null),
                Map.entry("cuisineRating", cuisineFeedback != null ?
                        String.valueOf(cuisineFeedback.getRate()) : null),  // Convert double to int string
                Map.entry("serviceComment", serviceFeedback != null ? serviceFeedback.getComment() : null),
                Map.entry("serviceRating", serviceFeedback != null ?
                        String.valueOf(serviceFeedback.getRate()) : null),  // Convert double to int string
                Map.entry("reservationId", reservationId)
        );
    }
}

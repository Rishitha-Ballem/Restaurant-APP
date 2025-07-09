package com.epam.service;

import com.epam.edai.run8.team15.dto.FeedbackRequest;
import com.epam.edai.run8.team15.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final DynamoDbTable<Feedback> feedbackDynamoDbTable;
    private final DynamoDbTable<Reservation> reservationDynamoDbTable;
    private final DynamoDbTable<Location> locationDynamoDbTable;
    private final DynamoDbTable<Report> reportDynamoDbTable;
    private final DynamoDbTable<User> userDynamoDbTable;

    public Map<String, Object> saveFeedback(FeedbackRequest feedbackRequest,String email) {
        try {
            User user = userDynamoDbTable.getItem(Key.builder().partitionValue(email).build());
            String userName = user.getFirstName() + " " + user.getLastName();
            if (feedbackRequest.getServiceRating().isEmpty() ||
                    feedbackRequest.getCuisineRating().isEmpty() ||
                    feedbackRequest.getCuisineRating().equals("") ||
                    feedbackRequest.getServiceRating().equals("")) {
                return Map.of("statusCode", 400,
                        "message", "Must provide ratings");
            }

            /* Validations after 20th april report START*/
            String reservationId = feedbackRequest.getReservationId();
            if (reservationId == null || reservationId.trim().isEmpty()) {
                return Map.of("statusCode", 400, "message", "Missing field: reservationId");
            }
            /* Validations after 20th april report END*/


            Key key = Key.builder().partitionValue(feedbackRequest.getReservationId()).build();

            if (key == null) {
                System.out.println("key is null");
                return Map.of("statusCode", 400,
                        "message", "Reservation doesn't exist");
            }

            Reservation reservation = reservationDynamoDbTable.getItem(key);

            /* Validations after 20th april report START*/
            if (reservation == null) {
                return Map.of("statusCode", 400, "message", "Reservation doesn't exist");
            }
            /* Validations after 20th april report END*/

            if (!reservation.getUserId().equals(email)) {
                return Map.of("statusCode", 401,
                        "message", "Unauthorized to make feedback");
            }

            Feedback cuisineFeedback;
            Feedback serviceFeedback;
            boolean isUpdated = false;
            if (!reservation.getFeedbackId().isEmpty() && reservation.getFeedbackId().length() != 0) {

                /* Validations after 20th april report START */
                String CuisineRatingCheck = feedbackRequest.getCuisineRating();

                if (CuisineRatingCheck != null && !CuisineRatingCheck.trim().isEmpty()) {
                    try {
                        double cuisineRatingDouble = Double.parseDouble(CuisineRatingCheck);

                        if (cuisineRatingDouble <= 0 || cuisineRatingDouble > 5) {
                            return Map.of("statusCode", 400, "message", "Rating Should be within 1 to 5");
                        }
                    } catch (NumberFormatException e) {
                        return Map.of("statusCode", 400, "message", "Invalid Cuisine Rating");
                    }
                }
                /* Validations after 20th april report END */

                cuisineFeedback = toCuisineFeedback(feedbackRequest, userName, reservation.getFeedbackId(), reservation.getLocationAddress());

                /* Validations after 20th april report START */
                String ServiceRatingCheck = feedbackRequest.getServiceRating();

                if (ServiceRatingCheck != null && !ServiceRatingCheck.trim().isEmpty()) {
                    try {
                        double serviceRatingDouble = Double.parseDouble(ServiceRatingCheck);

                        if (serviceRatingDouble <= 0 || serviceRatingDouble > 5) {
                            return Map.of("statusCode", 400, "message", "Rating Should be within 1 to 5");
                        }
                    } catch (NumberFormatException e) {
                        return Map.of("statusCode", 400, "message", "Invalid Service Rating");
                    }
                }
                /* Validations after 20th april report END */

                serviceFeedback = toServiceFeedback(feedbackRequest, userName, reservation.getFeedbackId(), reservation.getLocationAddress());
                isUpdated = true;
            } else {
                if (reservation.getStatus().equalsIgnoreCase("Reserved") || reservation.getStatus().equalsIgnoreCase("Cancelled")) {
                    return Map.of("statusCode", 400,
                            "message", "Can't provide feedback at this moment");
                }
                String feedbackId = UUID.randomUUID().toString();
                cuisineFeedback = toCuisineFeedback(feedbackRequest, userName, feedbackId, reservation.getLocationAddress());
                serviceFeedback = toServiceFeedback(feedbackRequest, userName, feedbackId, reservation.getLocationAddress());
                reservation.setFeedbackId(feedbackId);
                Key keyLoc = Key.builder().partitionValue(reservation.getLocationAddress()).build();
                Location location = locationDynamoDbTable.getItem(keyLoc);
                List<String> feedbacks = location.getFeedbacks();
                feedbacks.add(feedbackId + "c");
                feedbacks.add(feedbackId + "s");
                location.setFeedbacks(feedbacks);
//                locationDynamoDbTable.putItem(PutItemEnhancedRequest.builder(Location.class).item(location).build());
                locationDynamoDbTable.updateItem(location);
            }

            feedbackDynamoDbTable.putItem(cuisineFeedback);
            feedbackDynamoDbTable.putItem(serviceFeedback);
            //reservationDynamoDbTable.putItem(PutItemEnhancedRequest.builder(Reservation.class).item(reservation).build());
            reservationDynamoDbTable.updateItem(reservation);

//            if(reservation.getStatus().equalsIgnoreCase("Finished")){
//                Report report = reportDynamoDbTable.getItem(Key.builder().partitionValue(reservation.getId()).build());
//                report.setFeedbackCuisineRating(String.valueOf(cuisineFeedback.getRate()));
//                report.setFeedbackServiceRating(String.valueOf(serviceFeedback.getRate()));
//                reportDynamoDbTable.updateItem(report);
//            }
            if (reservation.getStatus().equalsIgnoreCase("Finished")) {
                System.out.println("Entered finished thing");
                printAllReportIds();
                System.out.println("the reservation id is :" + reservation.getId());
                Report report2 = reportDynamoDbTable.getItem(Key.builder().partitionValue("R2").build());
                System.out.println("It is fetching report2");
                Report report = reportDynamoDbTable.getItem(Key.builder().partitionValue(reservation.getId()).build());

                System.out.println("Report is fetched");

                report.setFeedbackCuisineRating(String.valueOf(cuisineFeedback.getRate()));

                report.setFeedbackServiceRating(String.valueOf(serviceFeedback.getRate()));

                System.out.println("trying to update ");

                reportDynamoDbTable.updateItem(report);

                System.out.println("feedback updated successfully");

            }


            if (isUpdated) {
                return Map.of("statusCode", 200,
                        "message", "Feedback updated successfully");
            }
            return Map.of("statusCode", 201,
                    "message", "Feedback successfully created");
        } catch (Exception e) {
            System.out.println("Exception entered");
            return Map.of("statusCode", 500,
                    "message", "Error saving feedback");
        }
    }

    public Feedback toCuisineFeedback(FeedbackRequest feedbackRequest, String fullName, String id, String locationId) {

        String cuisineComment = feedbackRequest.getCuisineComment();
        String cuisineRating = feedbackRequest.getCuisineRating();
        String reservationId = feedbackRequest.getReservationId();
        String serviceComment = feedbackRequest.getServiceComment();
        String serviceRating = feedbackRequest.getServiceRating();

        Feedback feedback = new Feedback();


        feedback.setId(id + "c");
        feedback.setLocationId(locationId);
        feedback.setType("cuisine");
        feedback.setRate(Double.parseDouble(cuisineRating));  // Convert String to double
        feedback.setComment(cuisineComment);
        feedback.setUserName(fullName);
        feedback.setUserAvatarUrl("https://img.freepik.com/free-vector/blue-circle-with-white-user_78370-4707.jpg?t=st=1744558527~exp=1744562127~hmac=11f647c670b4339fcee890e8eda99e5e408e7c1c0e27b228ba7d8f2e70645a67&w=900");  // Empty string as per requirement
        feedback.setDate(LocalDateTime.now().toString());  // ISO-8601 format
        return feedback;
    }

    public Feedback toServiceFeedback(FeedbackRequest feedbackRequest, String fullName, String id, String locationId) {

        String cuisineComment = feedbackRequest.getCuisineComment();
        String cuisineRating = feedbackRequest.getCuisineRating();
        String reservationId = feedbackRequest.getReservationId();
        String serviceComment = feedbackRequest.getServiceComment();
        String serviceRating = feedbackRequest.getServiceRating();

        Feedback feedback = new Feedback();
        feedback.setId(id + "s");
        feedback.setLocationId(locationId);
        feedback.setType("service");
        feedback.setRate(Double.parseDouble(serviceRating));
        feedback.setComment(serviceComment);
        feedback.setUserName(fullName);
        feedback.setUserAvatarUrl("https://img.freepik.com/free-vector/blue-circle-with-white-user_78370-4707.jpg?t=st=1744558527~exp=1744562127~hmac=11f647c670b4339fcee890e8eda99e5e408e7c1c0e27b228ba7d8f2e70645a67&w=900");
        feedback.setDate(LocalDateTime.now().toString());
        return feedback;
    }

    public Feedback getFeedbackById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        Feedback feedback = feedbackDynamoDbTable.getItem(key);
        return feedback;
    }

    public void printAllReportIds() {
        try {
            reportDynamoDbTable.scan()
                    .items()
                    .forEach(report -> System.out.println("Report ID: " + report.getId()));
        } catch (Exception e) {
            System.out.println("Failed to scan Report table: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

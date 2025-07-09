package com.epam.dto;

public class FeedbackRequest {
    private String cuisineComment;
    private String cuisineRating;
    private String reservationId;
    private String serviceComment;
    private String serviceRating;

    // Getters and Setters
    public String getCuisineComment() {
        return cuisineComment;
    }

    public void setCuisineComment(String cuisineComment) {
        this.cuisineComment = cuisineComment;
    }

    public String getCuisineRating() {
        return cuisineRating;
    }

    public void setCuisineRating(String cuisineRating) {
        this.cuisineRating = cuisineRating;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getServiceComment() {
        return serviceComment;
    }

    public void setServiceComment(String serviceComment) {
        this.serviceComment = serviceComment;
    }

    public String getServiceRating() {
        return serviceRating;
    }

    public void setServiceRating(String serviceRating) {
        this.serviceRating = serviceRating;
    }
}
package com.epam.dto;


import com.epam.edai.run8.team15.entity.Location;
import lombok.Data;

@Data
public class LocationDto {
    private String id;
    private String address;
    private String description;
    private int totalCapacity;
    private double averageOccupancy;
    private String imageUrl;
    private double rating;

    public static LocationDto fromEntity(Location location) {
        LocationDto dto = new LocationDto();
        dto.setId(location.getId());
        dto.setAddress(location.getAddress());
        dto.setDescription(location.getDescription());
        dto.setTotalCapacity(location.getTotalCapacity());
        dto.setAverageOccupancy(location.getAverageOccupancy());
        dto.setImageUrl(location.getImageUrl());
        dto.setRating(location.getRating());
        return dto;
    }
}

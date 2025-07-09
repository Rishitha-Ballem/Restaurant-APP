package com.epam.controller;

import com.epam.edai.run8.team15.dto.LocationDto;
import com.epam.edai.run8.team15.dto.PageFeedbackDto;
import com.epam.edai.run8.team15.dto.ShortLocationDto;
import com.epam.edai.run8.team15.dto.SpecialDishDto;
import com.epam.edai.run8.team15.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/locations")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET})
@RequiredArgsConstructor
public class LocationController{

    private final LocationService locationService;

    @GetMapping
    public ResponseEntity<?> getAllLocations() {
        List<LocationDto> locations = locationService.getAllLocations();
        if (locations.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(Map.of("message", "No locations found"));
        }
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/select-options")
    public ResponseEntity<?> getShortLocations() {
        List<ShortLocationDto> locations = locationService.getShortLocations();
        if (locations.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(Map.of("message", "No locations found"));
        }
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/{id}/speciality-dishes")
    public ResponseEntity<?> getLocationSpecialDishes(@PathVariable String id) {
        List<SpecialDishDto> specialDishes = locationService.getLocationSpecialDishes(id);
        if (specialDishes.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(Map.of("message", "No special dishes found for location id: " + id));
        }
        return ResponseEntity.ok(specialDishes);
    }

    @GetMapping("/{id}/tables")
    public ResponseEntity<?> getAllTablesOfLocation(@PathVariable String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Missing location ID in path");
        }
        List<String> tables = locationService.getAllTablesOfLocation(id);
        if (tables.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(Map.of("message", "No tables found for location id: " + id));
        }
        return ResponseEntity.ok(tables);
    }

    @GetMapping("/{id}/feedbacks")
    public ResponseEntity<PageFeedbackDto> getPagedFeedbackContent(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam String type,
            @RequestParam(defaultValue = "rating,asc") String sort) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Missing type parameter");
        }
        PageFeedbackDto response = locationService.getPagedFeedbackContent(id, page, size, type, sort);
        return ResponseEntity.ok(response);
    }

}
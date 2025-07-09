package com.epam.service;

import com.epam.edai.run8.team15.dto.*;
import com.epam.edai.run8.team15.entity.Feedback;
import com.epam.edai.run8.team15.entity.Location;
import com.epam.edai.run8.team15.exception.ResourceNotFoundException;
import com.epam.edai.run8.team15.repository.DishRepository;
import com.epam.edai.run8.team15.repository.FeedbackRepository;
import com.epam.edai.run8.team15.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationService {
    private final LocationRepository locationRepository;
    private final FeedbackRepository feedbackRepository;
    private final DishRepository dishRepository;

    public List<LocationDto> getAllLocations() {
        List<Location> locations = locationRepository.findAll();
        return locations.stream()
                .map(LocationDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ShortLocationDto> getShortLocations() {
        List<Location> locations = locationRepository.findAll();
        return locations.stream()
                .map(ShortLocationDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<SpecialDishDto> getLocationSpecialDishes(String id) {
        Location location = locationRepository.findById(id);
        if (location == null) {
            throw new ResourceNotFoundException("Location doesn't exist with id: " + id);
        }
        return location.getSpecialDishes().stream()
                .map(dishRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(SpecialDishDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<String> getAllTablesOfLocation(String id) {
        Location location = locationRepository.findById(id);
        if (location == null) {
            throw new ResourceNotFoundException("Location doesn't exist with id: " + id);
        }
        return location.getTables() != null ? location.getTables() : List.of();
    }

    public PageFeedbackDto getPagedFeedbackContent(String locationId, int page, int size, String type, String sort) {

        // Validate type
        String normalizedType = type.trim().toLowerCase();
        if (!normalizedType.equals("service") && !normalizedType.equals("cuisine")) {
            throw new IllegalArgumentException("type must be either 'service' or 'cuisine'");
        }

        // Validate pagination
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Invalid pagination parameters: page=" + page + ", size=" + size);
        }

        // Fetch location
        Location location = locationRepository.findById(locationId);
        if (location == null) {
            throw new ResourceNotFoundException("Location not found with id: " + locationId);
        }

        // Fetch feedbacks
        List<Feedback> feedbacks = location.getFeedbacks() != null
                ? location.getFeedbacks().stream()
                .map(feedbackRepository::findById)
                .filter(f -> f != null && f.getType().equalsIgnoreCase(normalizedType))
                .collect(Collectors.toList())
                : List.of();

        // Sort feedbacks
        List<Feedback> sortedFeedbacks = sortFeedbacks(feedbacks, sort);

        // Paginate manually
        int start = page * size;
        if (start >= sortedFeedbacks.size()) {
            start = sortedFeedbacks.size();
        }
        int end = Math.min(start + size, sortedFeedbacks.size());
        List<Feedback> pagedFeedbacks = start < sortedFeedbacks.size()
                ? sortedFeedbacks.subList(start, end)
                : List.of();

        // Map to DTOs
        List<FeedbackDto> feedbackDtos = pagedFeedbacks.stream()
                .map(FeedbackDto::fromEntity)
                .collect(Collectors.toList());

        // Build pagination metadata
        int totalElements = feedbacks.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean isFirst = page == 0;
        boolean isLast = page >= totalPages - 1;
        boolean isEmpty = feedbackDtos.isEmpty();

        List<SortDto> sortDtos = buildSortDtos(sort);
        PageableDto pageableDto = new PageableDto();
        pageableDto.setOffset((long) start);
        pageableDto.setSort(sortDtos);
        pageableDto.setPaged(true);
        pageableDto.setPageSize(size);
        pageableDto.setPageNumber(page);
        pageableDto.setUnpaged(false);

        PageFeedbackDto response = new PageFeedbackDto();
        response.setTotalPages(totalPages);
        response.setTotalElements(totalElements);
        response.setSize(size);
        response.setContent(feedbackDtos);
        response.setNumber(page);
        response.setSort(sortDtos);
        response.setFirst(isFirst);
        response.setLast(isLast);
        response.setNumberOfElements(feedbackDtos.size());
        response.setPageable(pageableDto);
        response.setEmpty(isEmpty);

        return response;
    }

    private List<Feedback> sortFeedbacks(List<Feedback> feedbacks, String sortCriteria) {
        if (sortCriteria == null || sortCriteria.isEmpty()) {
            sortCriteria = "rating,asc";
        }

        String[] parts = sortCriteria.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("sort criteria must be in format 'basis,order'");
        }

        String basis = parts[0].trim().toLowerCase();
        String order = parts[1].trim().toLowerCase();

        if (!basis.equals("rating") && !basis.equals("date")) {
            throw new IllegalArgumentException("sort basis must be 'rating' or 'date'");
        }
        if (!order.equals("asc") && !order.equals("desc")) {
            throw new IllegalArgumentException("sort order must be 'asc' or 'desc'");
        }

        Comparator<Feedback> comparator;
        switch (basis) {
            case "rating":
                comparator = Comparator.comparingDouble(Feedback::getRate);
                break;
            case "date":
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.n]");
                comparator = Comparator.comparing(f -> LocalDateTime.parse(f.getDate(), formatter));
                break;
            default:
                throw new IllegalStateException("Unexpected sort basis: " + basis);
        }

        if ("desc".equals(order)) {
            comparator = comparator.reversed();
        }

        return feedbacks.stream().sorted(comparator).collect(Collectors.toList());
    }

    private List<SortDto> buildSortDtos(String sortCriteria) {
        if (sortCriteria == null || sortCriteria.isEmpty()) {
            sortCriteria = "rating,asc";
        }

        String[] parts = sortCriteria.split(",");
        String property = parts[0].trim();
        String direction = parts[1].trim().toUpperCase();
        boolean ascending = direction.equalsIgnoreCase("ASC");

        SortDto sortDto = new SortDto();
        sortDto.setDirection(direction);
        sortDto.setNullHandling("NONE");
        sortDto.setAscending(ascending);
        sortDto.setProperty(property);
        sortDto.setIgnoreCase(true);

        return List.of(sortDto);
    }
}
package com.epam.service;

import com.epam.edai.run8.team15.dto.DishDto;
import com.epam.edai.run8.team15.dto.DishSelectionDto;
import com.epam.edai.run8.team15.dto.PopularDishDto;
import com.epam.edai.run8.team15.entity.Dish;
import com.epam.edai.run8.team15.repository.DishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DishService{

    private final DishRepository dishRepository;

    public DishDto getDishById(String id){
        return dishRepository.findById(id)
                .map(this::toDishDto)
                .orElseThrow(()->new RuntimeException("Dish not found with ID: " + id));

    }

    public List<PopularDishDto> getPopularDishes(){
        return dishRepository.findAllSortedByPopularity()
                .stream().map(this::toPopularDishDto)
                .collect(Collectors.toList());
    }

    public List<DishSelectionDto> getSelectedDishes(String type, String sortCriteria){
        String normalizedType = type.trim().toLowerCase();

        if (!normalizedType.equals("appetizers") && !normalizedType.equals("main courses") && !normalizedType.equals("desserts")) {
            throw new IllegalArgumentException("DishType must be one of : Appetizers, Main Courses, Desserts");
        }

        List<Dish> dishes = dishRepository.findByDishType(normalizedType);
        if(dishes.isEmpty()){
            return List.of();
        }

        String[] params = sortCriteria.split(",");
        if(params.length!=2){
            throw new IllegalArgumentException("Sort criteria must be in format 'basis,order'");
        }

        String basis = params[0].trim().toLowerCase();
        String order = params[1].trim().toLowerCase();

        if (!basis.equals("price") && !basis.equals("popularity")) {
            throw new IllegalArgumentException("sort basis must be 'price' or 'popularity'");
        }
        if (!order.equals("asc") && !order.equals("desc")) {
            throw new IllegalArgumentException("sort order must be 'asc' or 'desc'");
        }

        Comparator<Dish> comparator;

        switch (basis){
            case "popularity":
                comparator = Comparator.comparing(Dish::getOrder);
                break;
            case "price":
                comparator = Comparator.comparing(dish -> Double.parseDouble(dish.getPrice()));
                break;
            default:
                throw new IllegalStateException("Unexpected sort basis: " + basis);
        }

        if ("desc".equals(order)) {
            comparator = comparator.reversed();
        }

        return dishes.stream()
                .sorted(comparator)
                .map(this::toDishSelectionDto)
                .collect(Collectors.toList());
    }

    private DishDto toDishDto(Dish dish) {
        DishDto dto = new DishDto();
        dto.setId(dish.getId());
        dto.setName(dish.getName());
        dto.setDescription(dish.getDescription());
        dto.setDishType(dish.getDishType());
        dto.setPrice(dish.getPrice());
        dto.setWeight(dish.getWeight());
        dto.setImageUrl(dish.getImageUrl());
        dto.setCalories(dish.getCalories());
        dto.setCarbohydrates(dish.getCarbohydrates());
        dto.setFats(dish.getFats());
        dto.setProteins(dish.getProteins());
        dto.setVitamins(dish.getVitamins());
        dto.setState(dish.getState());
        return dto;
    }

    private PopularDishDto toPopularDishDto(Dish dish) {
        PopularDishDto dto = new PopularDishDto();
        dto.setName(dish.getName());
        dto.setPrice(dish.getPrice());
        dto.setWeight(dish.getWeight());
        dto.setImageUrl(dish.getImageUrl());
        return dto;
    }

    private DishSelectionDto toDishSelectionDto(Dish dish) {
        DishSelectionDto dto = new DishSelectionDto();
        dto.setId(dish.getId());
        dto.setName(dish.getName());
        dto.setState(dish.getState());
        dto.setPrice(dish.getPrice());
        dto.setWeight(dish.getWeight());
        dto.setPreviewImageUrl(dish.getImageUrl());
        return dto;
    }



}
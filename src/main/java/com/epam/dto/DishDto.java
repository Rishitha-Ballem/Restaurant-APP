package com.epam.dto;

import lombok.Data;

@Data
public class DishDto {
    private String id;
    private String name;
    private String description;
    private String dishType;
    private String price;
    private String weight;
    private String imageUrl;
    private String calories;
    private String carbohydrates;
    private String fats;
    private String proteins;
    private String vitamins;
    private String state;
}


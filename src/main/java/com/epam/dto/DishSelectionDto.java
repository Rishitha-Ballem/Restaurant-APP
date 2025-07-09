package com.epam.dto;

import lombok.Data;

@Data
public class DishSelectionDto {

    private String id;
    private String name;
    private String state;
    private String price;
    private String weight;
    private String previewImageUrl;
}

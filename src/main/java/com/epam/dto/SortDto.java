package com.epam.dto;

import lombok.Data;

@Data
public class SortDto {
    private String direction;
    private String nullHandling;
    private boolean ascending;
    private String property;
    private boolean ignoreCase;
}

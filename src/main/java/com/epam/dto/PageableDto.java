package com.epam.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageableDto {
    private long offset;
    private List<SortDto> sort;
    private boolean paged;
    private int pageSize;
    private int pageNumber;
    private boolean unpaged;
}

package com.epam.dto;


import lombok.Data;

import java.util.List;

@Data
public class PageFeedbackDto {

    private int totalPages;
    private long totalElements;
    private int size;
    private List<FeedbackDto> content;
    private int number;
    private List<SortDto> sort;
    private boolean first;
    private boolean last;
    private int numberOfElements;
    private PageableDto pageable;
    private boolean empty;
}

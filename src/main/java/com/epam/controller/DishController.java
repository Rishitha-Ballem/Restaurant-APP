package com.epam.controller;


import com.epam.edai.run8.team15.dto.DishDto;
import com.epam.edai.run8.team15.dto.DishSelectionDto;
import com.epam.edai.run8.team15.dto.PopularDishDto;
import com.epam.edai.run8.team15.service.DishService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dishes")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET})
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    @GetMapping("/{id}")
    public ResponseEntity<DishDto> getDishById(@PathVariable String id){
        DishDto dishDto = dishService.getDishById(id);
        return ResponseEntity.status(HttpStatus.OK).body(dishDto);
    }

    @GetMapping("/popular")
    public ResponseEntity<List<PopularDishDto>> getPopularDishes() {
        List<PopularDishDto> popularDishes = dishService.getPopularDishes();
        return ResponseEntity.status(HttpStatus.OK).body(popularDishes);
    }

    @GetMapping ResponseEntity<?> getSelectedDishes(
            @RequestParam String dishType,
            @RequestParam(defaultValue = "price,asc") String sort){
        List<DishSelectionDto> dishes = dishService.getSelectedDishes(dishType,sort);
        if(dishes.isEmpty()){
            Map<String,String> response = Map.of("message","No dish of type" + dishType);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
        }
        Map<String,List<DishSelectionDto>> response = Map.of("content",dishes);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
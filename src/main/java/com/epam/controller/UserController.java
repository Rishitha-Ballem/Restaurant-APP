package com.epam.controller;

import com.epam.edai.run8.team15.dto.SigninDTO;
import com.epam.edai.run8.team15.dto.SignupDTO;
import com.epam.edai.run8.team15.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class UserController {

    private final UserService userService;

    @PostMapping("/sign-up")
    public ResponseEntity<Map<String, Object>> signUpUser(@RequestBody SignupDTO user) {
        Map<String, Object> response = userService.registerUser(user);
        int statusCode = (Integer) response.get("statusCode");

        return createResponse((String) response.get("message"), statusCode);
    }

    @PostMapping("/sign-in")
    public ResponseEntity<Map<String, Object>> signInUser(@RequestBody SigninDTO user) {
        try {

            Map<String, Object> response = userService.signInUser(user);
            int statusCode = (Integer) response.get("statusCode");

            if (response.containsKey("message")) {
                return createResponse((String) response.get("message"), statusCode);
            }

            Map<String, Object> body = (Map<String, Object>) response.get("body");
            return new ResponseEntity<>(body, HttpStatus.valueOf(statusCode));
        } catch (Exception e) {
            return createResponse("Error while sign in", 404);
        }
    }

    private ResponseEntity<Map<String, Object>> createResponse(String message, int statusCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);

        return ResponseEntity
                .status(HttpStatus.valueOf(statusCode))
                .body(body);
    }
}

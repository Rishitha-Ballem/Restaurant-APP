package com.epam.service;

import com.epam.edai.run8.team15.dto.SigninDTO;
import com.epam.edai.run8.team15.dto.SignupDTO;
import com.epam.edai.run8.team15.entity.User;
import com.epam.edai.run8.team15.entity.Waiter;
import com.epam.edai.run8.team15.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    private final DynamoDbTable<User> userDynamoDbTable;
    private final DynamoDbTable<Waiter> waiterDynamoDbTable;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    public Map<String, Object> registerUser(SignupDTO request) {
        try {
            log.info("Inside registerUser method");

            if (!isValidName(request.getFirstName())) {
                log.warn("Invalid first name: {}", request.getFirstName());
                return Map.of(
                        "statusCode", 400,
                        "message", "Invalid first name"
                );
            }
            if (!isValidName(request.getLastName())) {
                log.warn("Invalid last name: {}", request.getLastName());
                return Map.of(
                        "statusCode", 400,
                        "message", "Invalid last name"
                );
            }

            if (!isValidEmail(request.getEmail())) {
                log.warn("Invalid email format: {}", request.getEmail());
                return Map.of(
                        "statusCode", 400,
                        "message", "Invalid email"
                );
            }

            if (!isValidPassword(request.getPassword())) {
                log.warn("Invalid password provided for email: {}", request.getEmail());
                return Map.of(
                        "statusCode", 400,
                        "message", "Password must be 8-16 characters long and include at least one uppercase letter, one lowercase letter, one number, and one special character."
                );
            }

            log.info("Validations passed for email: {}", request.getEmail());

            User user = getUser(request.getEmail());
            if (user != null && user.getPassword() != null && !user.getPassword().isBlank()) {
                log.warn("User already exists with email: {}", request.getEmail());
                return Map.of(
                        "statusCode", 409,
                        "message", "A user with this email address already exists."
                );
            }

            String role = "CUSTOMER";

            if (checkIfWaiter(request.getEmail())) {
                role = "WAITER";
                log.info("User identified as WAITER for email: {}", request.getEmail());
            } else {
                log.info("User identified as CUSTOMER for email: {}", request.getEmail());
            }

            user = User.builder()
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .role(role)
                    .createdAt(Instant.now().toString())
                    .imageUrl("")
                    .build();

            userDynamoDbTable.putItem(PutItemEnhancedRequest.builder(User.class).item(user).build());
            log.info("User saved to DynamoDB table: {}", request.getEmail());

            if (role.equalsIgnoreCase("WAITER")) {
                Waiter w = waiterDynamoDbTable.getItem(Key.builder().partitionValue(request.getEmail()).build());
                if (w == null) {
                    log.info("No existing waiter found for email: {}. Creating new waiter entry.", user.getEmail());
                    Waiter waiter = Waiter.builder()
                            .emailId(user.getEmail())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .reservationIds(new ArrayList<>())
                            .imageUrl(user.getImageUrl())
                            .build();
                    waiterDynamoDbTable.putItem(waiter);
                    log.info("New waiter created and saved: {}", waiter.getEmailId());
                } else {
                    log.info("Existing waiter found. Updating waiter information for email: {}", w.getEmailId());
                    w.setFirstName(user.getFirstName());
                    w.setLastName(user.getLastName());
                    w.setImageUrl(user.getImageUrl());
                    waiterDynamoDbTable.updateItem(w);
                    log.info("Waiter updated successfully: {}", w.getEmailId());
                }
            }

            log.info("User registration completed successfully for email: {}", request.getEmail());

            return Map.of(
                    "statusCode", 201,
                    "message", "User registered successfully!"
            );

        } catch (Exception e) {
            log.error("Error occurred during user registration for email: {}", request.getEmail(), e);
            return Map.of(
                    "statusCode", 500,
                    "message", "Internal server error during user registration."
            );
        }
    }


    public Map<String, Object> signInUser(SigninDTO request) {
        try {
            log.info("Inside signInUser method");

            // Validate email
            if (!isValidEmail(request.getEmail())) {
                log.warn("Invalid email format: {}", request.getEmail());
                return Map.of(
                        "statusCode", 400,
                        "message", "email is not valid"
                );
            }
            log.info("Email format validated: {}", request.getEmail());

            // Validate password
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                log.warn("Password is empty for email: {}", request.getEmail());
                return Map.of(
                        "statusCode", 400,
                        "message", "Password is empty!"
                );
            }
            log.info("Password is provided for email: {}", request.getEmail());

            // Fetch user
            User user = getUser(request.getEmail());
            if (user == null) {
                log.warn("No user found with email: {}", request.getEmail());
                return Map.of(
                        "statusCode", 403,
                        "message", "Invalid email or password"
                );
            }
            log.info("User found: {}", user.getEmail());

            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            log.info("Authentication successful for user: {}", user.getEmail());

            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            log.info("UserDetails loaded for user: {}", userDetails.getUsername());

            // Generate JWT token
            String accessToken = jwtUtil.generateToken(userDetails.getUsername());
            log.info("Access token generated for user: {}", user.getEmail());

            // Check for WAITER role
            if (user.getRole().equalsIgnoreCase("WAITER")) {
                log.info("User has WAITER role: {}", user.getEmail());
                Waiter waiter = waiterDynamoDbTable.getItem(Key.builder().partitionValue(request.getEmail()).build());
                if (waiter == null) {
                    log.warn("Waiter record not found for email: {}", request.getEmail());
                    return Map.of(
                            "statusCode", 403,
                            "message", "Invalid email or password"
                    );
                }
                log.info("Waiter details fetched successfully for: {}", request.getEmail());

                return Map.of(
                        "statusCode", 200,
                        "body", Map.of(
                                "username", user.getFirstName() + " " + user.getLastName(),
                                "accessToken", accessToken,
                                "role", user.getRole(),
                                "locationId", waiter.getLocationId()
                        )
                );
            }

            // For other roles
            log.info("User role is not WAITER. Returning standard response for: {}", user.getEmail());
            return Map.of(
                    "statusCode", 200,
                    "body", Map.of(
                            "username", user.getFirstName() + " " + user.getLastName(),
                            "accessToken", accessToken,
                            "role", user.getRole()
                    )
            );

        } catch (Exception e) {
            log.error("Error occurred during user sign-in: {}", e.getMessage(), e);
            return Map.of(
                    "statusCode", 403,
                    "message", "Invalid email or password"
            );
        }
    }


    public static boolean isValidName(String name) {
        return name != null && !name.isBlank() && name.matches("^[A-Za-z'\\-]{1,50}$");
    }

    public static boolean isValidEmail(String email) {
        return email != null && !email.isBlank() && email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public static boolean isValidPassword(String password) {
        return password != null && !password.isBlank() && password.length() >= 8 && password.length() <= 16 &&
                password.chars().anyMatch(Character::isUpperCase) &&
                password.chars().anyMatch(Character::isLowerCase) &&
                password.chars().anyMatch(Character::isDigit) &&
                password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
    }

    public boolean checkIfWaiter(String email) {
        User user = getUser(email);
        return user != null && "WAITER".equalsIgnoreCase(user.getRole());

    }

    public User getUser(String email) {
        Key key = Key.builder().partitionValue(email).build();
        return userDynamoDbTable.getItem(key);
    }
}
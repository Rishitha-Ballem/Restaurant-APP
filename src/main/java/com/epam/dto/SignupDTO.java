package com.epam.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SignupDTO {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
}

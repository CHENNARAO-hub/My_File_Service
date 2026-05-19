package com.enterprise.fileservice.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserFileRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;
}
package com.fpt.glasseshop.entity.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String name;

    private String phone;

    @Email(message = "Invalid email format")
    private String email;


    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}

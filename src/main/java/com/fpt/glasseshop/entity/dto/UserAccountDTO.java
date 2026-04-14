package com.fpt.glasseshop.entity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Information about a user account")
public class UserAccountDTO {
    @Schema(description = "Unique identifier of the user", example = "5")
    private Long userId;

    @Schema(description = "Full name of the user", example = "John Doe")
    private String name;

    @Schema(description = "Email address of the user", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Phone number of the user", example = "0987654321")
    private String phone;

    @Schema(description = "Role of the user in the system", example = "CUSTOMER")
    private String role;

    @Schema(description = "Password for the account (write-only)", example = "password123")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String password;

    @Schema(description = "Current status of the account", example = "ACTIVE")
    private String accountStatus;

    @Schema(description = "Timestamp when the account was created")
    private LocalDateTime createdAt;
}

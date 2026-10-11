
package com.jubayer.cholobodlai.DTO;

import com.jubayer.cholobodlai.Model.Emums.Role;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Pattern(regexp = "01[3-9][0-9]{8}") String phone,
        @NotBlank @Email @Size(max = 180) String email,
        @NotBlank @Size(min = 10, max = 100) String password,
        @NotNull Role role,
        @Size(max = 35) String truckRegistrationNo,
        String truckTypeId
) {}

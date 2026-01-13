package com.yourproject.tcm.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class InviteRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String role; // "QA", "BA", "TESTER"

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}

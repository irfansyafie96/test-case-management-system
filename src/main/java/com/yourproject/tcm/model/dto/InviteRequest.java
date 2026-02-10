package com.yourproject.tcm.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class InviteRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String role; // "QA", "BA", "TESTER"

    private boolean external = false;
    private Long projectId;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isExternal() { return external; }
    public void setExternal(boolean external) { this.external = external; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
}

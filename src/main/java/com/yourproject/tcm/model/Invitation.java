package com.yourproject.tcm.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invitations")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String role; // "QA", "BA", "TESTER"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
    private Organization organization;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "is_external")
    private Boolean external = false;

    @Column(name = "project_id")
    private Long projectId;

    private boolean accepted = false;

    public Invitation() {}

    public Invitation(String email, String role, Organization organization) {
        this.token = UUID.randomUUID().toString();
        this.email = email;
        this.role = role;
        this.organization = organization;
        this.expiryDate = LocalDateTime.now().plusDays(7); // Invites valid for 7 days
        this.external = false;
    }

    public Invitation(String email, String role, Organization organization, Boolean external, Long projectId) {
        this(email, role, organization);
        this.external = external;
        this.projectId = projectId;
    }

    public boolean isValid() {
        return !accepted && LocalDateTime.now().isBefore(expiryDate);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public boolean isExternal() { 
        return external != null && external; 
    }
    
    public void setExternal(Boolean external) { 
        this.external = external; 
    }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }

    // Helper for frontend since organization is ignored
    public String getOrganizationName() {
        return organization != null ? organization.getName() : null;
    }
}

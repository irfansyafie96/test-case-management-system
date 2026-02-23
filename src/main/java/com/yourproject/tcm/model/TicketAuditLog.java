package com.yourproject.tcm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * TicketAuditLog Entity - Tracks changes to Redmine tickets
 * 
 * Records all actions taken on a ticket: creation, status changes, updates.
 * Provides complete audit trail for compliance and tracking.
 */
@Entity
@Table(name = "ticket_audit_logs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TicketAuditLog {
    
    public static final String ACTION_CREATED = "CREATED";
    public static final String ACTION_STATUS_CHANGED = "STATUS_CHANGED";
    public static final String ACTION_UPDATED = "UPDATED";
    
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_CLOSED = "CLOSED";
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redmine_issue_id", nullable = false)
    @JsonIgnoreProperties({"execution", "auditLogs"})
    private RedmineIssue redmineIssue;
    
    @Column(nullable = false, length = 50)
    private String action;
    
    @Column(length = 255)
    private String oldValue;
    
    @Column(length = 255)
    private String newValue;
    
    @Column(nullable = false)
    private String changedBy;
    
    @Column(nullable = false)
    private LocalDateTime changedAt;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public RedmineIssue getRedmineIssue() {
        return redmineIssue;
    }
    
    public void setRedmineIssue(RedmineIssue redmineIssue) {
        this.redmineIssue = redmineIssue;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getOldValue() {
        return oldValue;
    }
    
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }
    
    public String getNewValue() {
        return newValue;
    }
    
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
    
    public String getChangedBy() {
        return changedBy;
    }
    
    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
    
    public LocalDateTime getChangedAt() {
        return changedAt;
    }
    
    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}

package com.yourproject.tcm.service.domain;

import com.yourproject.tcm.model.RedmineIssue;
import com.yourproject.tcm.model.TicketAuditLog;
import com.yourproject.tcm.model.dto.TicketAuditLogDTO;
import com.yourproject.tcm.model.dto.TicketDTO;
import com.yourproject.tcm.repository.RedmineIssueRepository;
import com.yourproject.tcm.repository.TicketAuditLogRepository;
import com.yourproject.tcm.service.SecurityHelper;
import com.yourproject.tcm.service.UserContextService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TicketService {
    
    private final RedmineIssueRepository redmineIssueRepository;
    private final TicketAuditLogRepository ticketAuditLogRepository;
    private final SecurityHelper securityHelper;
    private final UserContextService userContextService;
    
    public TicketService(
            RedmineIssueRepository redmineIssueRepository,
            TicketAuditLogRepository ticketAuditLogRepository,
            SecurityHelper securityHelper,
            UserContextService userContextService) {
        this.redmineIssueRepository = redmineIssueRepository;
        this.ticketAuditLogRepository = ticketAuditLogRepository;
        this.securityHelper = securityHelper;
        this.userContextService = userContextService;
    }
    
    @Transactional(readOnly = true)
    public List<TicketDTO> getTickets(Long projectId, Long cycleId, String status) {
        List<RedmineIssue> issues = redmineIssueRepository.findAll();
        
        return issues.stream()
                .filter(issue -> isTicketAccessible(issue, projectId, cycleId, status))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public TicketDTO getTicketById(Long ticketId) {
        RedmineIssue issue = redmineIssueRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
        return toDTO(issue);
    }
    
    public TicketDTO updateTicketStatus(Long ticketId, String newStatus) {
        RedmineIssue issue = redmineIssueRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
        
        String oldStatus = issue.getStatus();
        String username = userContextService.getCurrentUser().getUsername();
        
        issue.setStatus(newStatus);
        
        // Create audit log
        TicketAuditLog auditLog = new TicketAuditLog();
        auditLog.setRedmineIssue(issue);
        auditLog.setAction(TicketAuditLog.ACTION_STATUS_CHANGED);
        auditLog.setOldValue(oldStatus);
        auditLog.setNewValue(newStatus);
        auditLog.setChangedBy(username);
        
        issue.addAuditLog(auditLog);
        
        RedmineIssue saved = redmineIssueRepository.save(issue);
        return toDTO(saved);
    }
    
    private boolean isTicketAccessible(RedmineIssue issue, Long projectId, Long cycleId, String status) {
        if (issue.getExecution() == null || issue.getExecution().getTestCase() == null) {
            return false;
        }
        
        // Check project access
        Long executionProjectId = issue.getExecution().getProjectId();
        if (projectId != null && !projectId.equals(executionProjectId)) {
            return false;
        }
        
        // Check cycle access
        if (cycleId != null) {
            Long executionCycleId = issue.getExecution().getTestCycleId();
            if (!cycleId.equals(executionCycleId)) {
                return false;
            }
        }
        
        // Check status filter
        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("all")) {
            if (!status.equalsIgnoreCase(issue.getStatus())) {
                return false;
            }
        }
        
        return true;
    }
    
    private TicketDTO toDTO(RedmineIssue issue) {
        TicketDTO dto = new TicketDTO();
        dto.setId(issue.getId());
        dto.setRedmineIssueId(issue.getRedmineIssueId());
        dto.setRedmineIssueUrl(issue.getRedmineIssueUrl());
        dto.setBugReportSubject(issue.getBugReportSubject());
        dto.setBugReportDescription(issue.getBugReportDescription());
        dto.setStatus(issue.getStatus());
        
        if (issue.getExecution() != null) {
            dto.setExecutionId(issue.getExecution().getId());
            dto.setTestCaseTitle(issue.getExecution().getTitle());
            dto.setTestCaseId(issue.getExecution().getTestCaseId());
            dto.setProjectId(issue.getExecution().getProjectId());
            dto.setProjectName(issue.getExecution().getProjectName());
            dto.setCycleId(issue.getExecution().getTestCycleId());
            dto.setCycleName(issue.getExecution().getTestCycleName());
        }
        
        dto.setCreatedAt(issue.getCreatedAt());
        dto.setUpdatedAt(issue.getUpdatedAt());
        
        // Map audit logs
        if (issue.getAuditLogs() != null) {
            dto.setAuditLogs(issue.getAuditLogs().stream()
                    .map(this::toAuditLogDTO)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    private TicketAuditLogDTO toAuditLogDTO(TicketAuditLog auditLog) {
        TicketAuditLogDTO dto = new TicketAuditLogDTO();
        dto.setId(auditLog.getId());
        dto.setAction(auditLog.getAction());
        dto.setOldValue(auditLog.getOldValue());
        dto.setNewValue(auditLog.getNewValue());
        dto.setChangedBy(auditLog.getChangedBy());
        dto.setChangedAt(auditLog.getChangedAt());
        dto.setNotes(auditLog.getNotes());
        return dto;
    }
}

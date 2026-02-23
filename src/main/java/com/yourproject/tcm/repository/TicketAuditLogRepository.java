package com.yourproject.tcm.repository;

import com.yourproject.tcm.model.TicketAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketAuditLogRepository extends JpaRepository<TicketAuditLog, Long> {
    
    List<TicketAuditLog> findByRedmineIssueIdOrderByChangedAtDesc(Long redmineIssueId);
}

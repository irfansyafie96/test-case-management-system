-- Test Cycle and Ticket Management Migration
-- Version 2: Add Test Cycles, Ticket Status, and Audit Trail
-- ============================================================

-- Test Cycles table
CREATE TABLE IF NOT EXISTS test_cycles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    redmine_project_url VARCHAR(500),
    project_id BIGINT NOT NULL,
    start_date DATETIME,
    end_date DATETIME,
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add test_cycle_id to test_executions
ALTER TABLE test_executions 
ADD COLUMN test_cycle_id BIGINT AFTER redmine_issue_updated_at,
ADD FOREIGN KEY (test_cycle_id) REFERENCES test_cycles(id) ON DELETE SET NULL;

-- Add status column to redmine_issues (OPEN/CLOSED)
ALTER TABLE redmine_issues 
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'OPEN' AFTER bug_report_description;

-- Ticket audit log table
CREATE TABLE IF NOT EXISTS ticket_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    redmine_issue_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_value VARCHAR(255),
    new_value VARCHAR(255),
    changed_by VARCHAR(255) NOT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    FOREIGN KEY (redmine_issue_id) REFERENCES redmine_issues(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create indexes for better performance
CREATE INDEX idx_cycles_project ON test_cycles(project_id);
CREATE INDEX idx_cycles_project_active ON test_cycles(project_id, is_active);
CREATE INDEX idx_executions_cycle ON test_executions(test_cycle_id);
CREATE INDEX idx_audit_logs_issue ON ticket_audit_logs(redmine_issue_id);
CREATE INDEX idx_issues_status ON redmine_issues(status);

-- Update existing redmine_issues to have OPEN status (for backward compatibility)
UPDATE redmine_issues SET status = 'OPEN' WHERE status IS NULL OR status = '';

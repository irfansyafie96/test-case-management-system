-- Migration: Create redmine_issues table for multi-issue support
-- Date: 2026-02-12

-- Create the redmine_issues table
CREATE TABLE IF NOT EXISTS redmine_issues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id BIGINT NOT NULL,
    redmine_issue_id VARCHAR(100),
    redmine_issue_url VARCHAR(500) NOT NULL,
    bug_report_subject VARCHAR(500),
    bug_report_description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (execution_id) REFERENCES test_executions(id) ON DELETE CASCADE,
    INDEX idx_execution_id (execution_id)
);

-- Migrate existing redmine data from test_executions to redmine_issues
INSERT INTO redmine_issues (execution_id, redmine_issue_id, redmine_issue_url, bug_report_subject, bug_report_description, created_at)
SELECT 
    te.id AS execution_id,
    te.redmine_issue_id,
    te.redmine_issue_url,
    te.bug_report_subject,
    te.bug_report_description,
    COALESCE(te.redmine_issue_created_at, te.completion_date, NOW()) AS created_at
FROM test_executions te
WHERE te.redmine_issue_url IS NOT NULL;

-- Note: The original redmine_* columns in test_executions are kept for backward compatibility
-- They can be removed in a future migration after frontend is fully migrated to use redmine_issues table
